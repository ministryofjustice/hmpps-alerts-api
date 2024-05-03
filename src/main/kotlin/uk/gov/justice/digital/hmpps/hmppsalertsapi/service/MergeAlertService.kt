package uk.gov.justice.digital.hmpps.hmppsalertsapi.service

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.hmppsalertsapi.domain.toAlertEntity
import uk.gov.justice.digital.hmpps.hmppsalertsapi.entity.Alert
import uk.gov.justice.digital.hmpps.hmppsalertsapi.entity.AlertCode
import uk.gov.justice.digital.hmpps.hmppsalertsapi.enumeration.Source.NOMIS
import uk.gov.justice.digital.hmpps.hmppsalertsapi.model.MergedAlert
import uk.gov.justice.digital.hmpps.hmppsalertsapi.model.MergedAlerts
import uk.gov.justice.digital.hmpps.hmppsalertsapi.model.request.MergeAlerts
import uk.gov.justice.digital.hmpps.hmppsalertsapi.repository.AlertCodeRepository
import uk.gov.justice.digital.hmpps.hmppsalertsapi.repository.AlertRepository
import java.time.LocalDateTime

@Service
@Transactional
class MergeAlertService(
  private val alertCodeRepository: AlertCodeRepository,
  private val alertRepository: AlertRepository,
) {
  fun mergePrisonerAlerts(request: MergeAlerts): MergedAlerts {
    val mergedAt = LocalDateTime.now()
    val alertCodes = request.alertCodes()
    request.checkForNotFoundAlertCodes(alertCodes)
    request.logActiveToBeforeActiveFrom(request.prisonNumberMergeTo)

    val alertsDeleted = alertRepository.saveAllAndFlush(
      alertRepository.findByPrisonNumber(request.prisonNumberMergeFrom).onEach {
        it.delete(mergedAt, "SYS", "Merge deleted from ${request.prisonNumberMergeFrom}", NOMIS, null)
      },
    )

    val alertsCreated = mutableListOf<MergedAlert>()
    request.newAlerts.map {
      val alert = alertRepository.save(it.toAlertEntity(request.prisonNumberMergeFrom, request.prisonNumberMergeTo, alertCodes[it.alertCode]!!, mergedAt))
      alertsCreated.add(
        MergedAlert(
          offenderBookId = it.offenderBookId,
          alertSeq = it.alertSeq,
          alertUuid = alert.alertUuid,
        ),
      )
      alert
    }.also {
      it.logDuplicateActiveAlerts(request.prisonNumberMergeTo)
      alertRepository.flush()
    }

    return MergedAlerts(
      alertsCreated = alertsCreated,
      alertsDeleted = alertsDeleted.map { it.alertUuid },
    )
  }

  private fun MergeAlerts.alertCodes() =
    newAlerts.map { it.alertCode }.distinct().let { requestAlertCodes ->
      alertCodeRepository.findByCodeIn(requestAlertCodes).associateBy { it.code }
    }

  private fun MergeAlerts.checkForNotFoundAlertCodes(alertCodes: Map<String, AlertCode>) =
    newAlerts.map { it.alertCode }.distinct().filterNot { alertCodes.containsKey(it) }.sorted().run {
      if (this.isNotEmpty()) {
        throw IllegalArgumentException("Alert code(s) '${this.joinToString("', '") }' not found")
      }
    }

  private fun MergeAlerts.logActiveToBeforeActiveFrom(prisonNumber: String) {
    newAlerts.filter { it.activeTo?.isBefore(it.activeFrom) == true }.forEach {
      log.warn("Alert with sequence '${it.alertSeq}' for person with prison number '$prisonNumber' from booking with id '${it.offenderBookId}' has an active to date '${it.activeTo}' that is before the active from date '${it.activeFrom}'")
    }
  }

  private fun List<Alert>.logDuplicateActiveAlerts(prisonNumber: String) {
    this.filter { it.isActive() || it.willBecomeActive() }.groupBy { it.alertCode.code }.filter { it.value.size > 1 }.run {
      if (any()) {
        log.warn("Person with prison number '$prisonNumber' has ${this.size} duplicate active alert(s) for code(s) ${this.map { "'${it.key}' (${it.value.size} active)" }.joinToString(", ")}")
      }
    }
  }

  private companion object {
    private val log: Logger = LoggerFactory.getLogger(this::class.java)
  }
}
