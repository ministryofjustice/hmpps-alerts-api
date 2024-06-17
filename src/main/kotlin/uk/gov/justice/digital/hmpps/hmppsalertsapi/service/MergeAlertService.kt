package uk.gov.justice.digital.hmpps.hmppsalertsapi.service

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.hmppsalertsapi.client.prisonersearch.PrisonerSearchClient
import uk.gov.justice.digital.hmpps.hmppsalertsapi.domain.toAlertEntity
import uk.gov.justice.digital.hmpps.hmppsalertsapi.entity.Alert
import uk.gov.justice.digital.hmpps.hmppsalertsapi.entity.AlertCode
import uk.gov.justice.digital.hmpps.hmppsalertsapi.entity.event.AlertsMergedEvent
import uk.gov.justice.digital.hmpps.hmppsalertsapi.enumeration.Reason.MERGE
import uk.gov.justice.digital.hmpps.hmppsalertsapi.enumeration.Source.NOMIS
import uk.gov.justice.digital.hmpps.hmppsalertsapi.model.MergedAlert
import uk.gov.justice.digital.hmpps.hmppsalertsapi.model.MergedAlerts
import uk.gov.justice.digital.hmpps.hmppsalertsapi.model.request.MergeAlerts
import uk.gov.justice.digital.hmpps.hmppsalertsapi.repository.AlertCodeRepository
import uk.gov.justice.digital.hmpps.hmppsalertsapi.repository.AlertRepository
import java.time.LocalDateTime
import java.util.UUID

@Service
@Transactional
class MergeAlertService(
  private val alertCodeRepository: AlertCodeRepository,
  private val alertRepository: AlertRepository,
  private val prisonerSearchClient: PrisonerSearchClient,
) {
  fun mergePrisonerAlerts(request: MergeAlerts): MergedAlerts {
    val mergedAt = LocalDateTime.now()
    val alertCodes = request.alertCodes()
    request.checkForNotFoundAlertCodes(alertCodes)
    request.validatePrisonNumberMergeTo()
    request.logActiveToBeforeActiveFrom(request.prisonNumberMergeTo)

    val retainedAlerts = alertRepository.findByAlertUuidIn(request.retainedAlertUuids)

    request.retainedAlertUuids.filter { uuid -> retainedAlerts.none { it.alertUuid == uuid } }
      .takeIf { it.isNotEmpty() }?.let {
        throw IllegalArgumentException("Could not find alert(s) with id(s) ${it.join()}")
      }

    val (prisonNumberOfAlertsToReassign, prisonNumberOfAlertsToCopy) = retainedAlerts.groupBy { it.prisonNumber }.keys.also {
      if (it.size > 1) {
        throw IllegalArgumentException("Alert(s) with id(s) ${request.retainedAlertUuids.join()} are not all associated with the same prison numbers")
      } else if (it.any { key -> key != request.prisonNumberMergeTo && key != request.prisonNumberMergeFrom }) {
        throw IllegalArgumentException("Alert(s) with id(s) ${request.retainedAlertUuids.join()} are not associated with either '${request.prisonNumberMergeFrom}' or '${request.prisonNumberMergeTo}'")
      }
    }.let {
      if (it.isEmpty() || it.single() == request.prisonNumberMergeTo) {
        Pair(request.prisonNumberMergeTo, request.prisonNumberMergeFrom)
      } else {
        Pair(request.prisonNumberMergeFrom, request.prisonNumberMergeTo)
      }
    }

    val alertsToReassign = alertRepository.findByPrisonNumber(prisonNumberOfAlertsToReassign).apply {
      if (size != request.retainedAlertUuids.size) {
        throw IllegalArgumentException("Retained Alert UUIDs does not cover all the alerts associated to '$prisonNumberOfAlertsToReassign'")
      }
    }
    val alertsToDelete = alertRepository.findByPrisonNumber(prisonNumberOfAlertsToCopy)
    val alertsCreated = mutableListOf<MergedAlert>()

    val newAlerts = request.newAlerts.map {
      val alert = it.toAlertEntity(
        request.prisonNumberMergeFrom,
        request.prisonNumberMergeTo,
        alertCodes[it.alertCode]!!,
        mergedAt,
        false,
      )
      alertsCreated.add(
        MergedAlert(
          offenderBookId = it.offenderBookId,
          alertSeq = it.alertSeq,
          alertUuid = alert.alertUuid,
        ),
      )
      alert
    }

    // As a workaround, the Alerts Merged domain event is to be registered into one of the affected Alert entity,
    // since there is not a single entity to represent the whole merge operation
    var domainEventRegistered = false
    val domainEvent = AlertsMergedEvent(
      prisonNumberMergeFrom = request.prisonNumberMergeFrom,
      prisonNumberMergeTo = request.prisonNumberMergeTo,
      mergedAlerts = alertsCreated,
    )

    newAlerts.map {
      if (!domainEventRegistered) {
        it.registerAlertsMergedEvent(domainEvent)
        domainEventRegistered = true
      }
      alertRepository.save(it)
    }.also {
      it.logDuplicateActiveAlerts(request.prisonNumberMergeTo)
      alertRepository.flush()
    }

    val alertsDeleted = alertRepository.saveAllAndFlush(
      alertsToDelete.onEach {
        it.delete(mergedAt, "SYS", "Merge deleted from $prisonNumberOfAlertsToCopy", NOMIS, MERGE, null, false)
      }.also {
        if (it.isNotEmpty() && !domainEventRegistered) {
          it.first().registerAlertsMergedEvent(domainEvent)
          domainEventRegistered = true
        }
      },
    )

    if (prisonNumberOfAlertsToReassign != request.prisonNumberMergeTo) {
      alertRepository.saveAllAndFlush(
        alertsToReassign.onEach {
          it.reassign(request.prisonNumberMergeTo)
          if (!domainEventRegistered) {
            it.registerAlertsMergedEvent(domainEvent)
            domainEventRegistered = true
          }
        },
      )
    }

    if (alertsDeleted.size != request.newAlerts.size) {
      log.warn(
        "Non-matching count while copying alerts " +
          "from prison number $prisonNumberOfAlertsToCopy to ${request.prisonNumberMergeTo}: " +
          "${alertsDeleted.size} deleted, and ${request.newAlerts.size} created.",
      )
    }

    return MergedAlerts(
      alertsCreated = alertsCreated,
      alertsDeleted = alertsDeleted.map { it.alertUuid },
    )
  }

  private fun MergeAlerts.alertCodes() = newAlerts.map { it.alertCode }.distinct().let { requestAlertCodes ->
    alertCodeRepository.findByCodeIn(requestAlertCodes).associateBy { it.code }
  }

  private fun MergeAlerts.checkForNotFoundAlertCodes(alertCodes: Map<String, AlertCode>) =
    newAlerts.map { it.alertCode }.distinct().filterNot { alertCodes.containsKey(it) }.sorted().run {
      if (this.isNotEmpty()) {
        throw IllegalArgumentException("Alert code(s) '${this.joinToString("', '")}' not found")
      }
    }

  private fun MergeAlerts.validatePrisonNumberMergeTo() =
    require(prisonerSearchClient.getPrisoner(prisonNumberMergeTo) != null) { "Prison number '$prisonNumberMergeTo' not found" }

  private fun MergeAlerts.logActiveToBeforeActiveFrom(prisonNumber: String) {
    newAlerts.filter { it.activeTo?.isBefore(it.activeFrom) == true }.forEach {
      log.warn("Alert with sequence '${it.alertSeq}' for person with prison number '$prisonNumber' from booking with id '${it.offenderBookId}' has an active to date '${it.activeTo}' that is before the active from date '${it.activeFrom}'")
    }
  }

  private fun List<Alert>.logDuplicateActiveAlerts(prisonNumber: String) {
    this.filter { it.isActive() }.groupBy { it.alertCode.code }.filter { it.value.size > 1 }.run {
      if (any()) {
        log.warn(
          "Person with prison number '$prisonNumber' has ${this.size} duplicate active alert(s) for code(s) ${
            this.map { "'${it.key}' (${it.value.size} active)" }.joinToString(", ")
          }",
        )
      }
    }
  }

  private fun Collection<UUID>.join() = joinToString(separator = ", ", transform = { "'$it'" })

  private companion object {
    private val log: Logger = LoggerFactory.getLogger(this::class.java)
  }
}
