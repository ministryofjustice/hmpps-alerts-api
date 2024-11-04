package uk.gov.justice.digital.hmpps.hmppsalertsapi.service

import com.fasterxml.jackson.databind.ObjectMapper
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.hmppsalertsapi.common.aop.PublishPersonAlertsChanged
import uk.gov.justice.digital.hmpps.hmppsalertsapi.entity.Alert
import uk.gov.justice.digital.hmpps.hmppsalertsapi.entity.AlertCode
import uk.gov.justice.digital.hmpps.hmppsalertsapi.entity.ResyncAudit
import uk.gov.justice.digital.hmpps.hmppsalertsapi.entity.ResyncAuditRepository
import uk.gov.justice.digital.hmpps.hmppsalertsapi.enumeration.Source
import uk.gov.justice.digital.hmpps.hmppsalertsapi.model.ResyncedAlert
import uk.gov.justice.digital.hmpps.hmppsalertsapi.model.request.ResyncAlert
import uk.gov.justice.digital.hmpps.hmppsalertsapi.repository.AlertCodeRepository
import uk.gov.justice.digital.hmpps.hmppsalertsapi.repository.AlertRepository
import java.time.LocalDateTime

const val REQUESTED_BY_SYS = "SYS"

@Transactional
@Service
class ResyncAlertsService(
  private val objectMapper: ObjectMapper,
  private val alertRepository: AlertRepository,
  private val alertCodeRepository: AlertCodeRepository,
  private val resyncAuditRepository: ResyncAuditRepository,
) {
  @PublishPersonAlertsChanged
  fun resyncAlerts(prisonNumber: String, alerts: List<ResyncAlert>): List<ResyncedAlert> {
    val requestedAt = LocalDateTime.now()
    val alertCodes = getValidatedAlertCodes(alerts)
    val existingAlerts = alertRepository.findByPrisonNumber(prisonNumber)
    alerts.logActiveToBeforeActiveFrom(prisonNumber)
    alerts.logDuplicateActiveAlerts(prisonNumber)

    val newAlerts = alerts
      .map { ResyncAlertMerge(it, existingAlerts.find(it)) }
      .map { it.alertFor(prisonNumber, alertCodes) }

    existingAlerts.forEach {
      it.delete(
        deletedBy = REQUESTED_BY_SYS,
        deletedByDisplayName = REQUESTED_BY_SYS,
        source = Source.NOMIS,
        activeCaseLoadId = null,
        description = "Alert deleted via resync",
      )
      alertRepository.saveAll(existingAlerts)
    }
    resyncAudit(
      prisonNumber,
      alerts,
      requestedAt,
      existingAlerts,
      newAlerts.mapNotNull { it.alert },
    )
    return newAlerts.map { it.resynced() }
  }

  private fun getValidatedAlertCodes(alerts: List<ResyncAlert>): Map<String, AlertCode> {
    val alertCodeCodes = alerts.map { it.alertCode }.toSet()
    val alertCodes = alertCodeRepository.findByCodeIn(alertCodeCodes).associateBy { it.code }
    val codesNotFound = alertCodeCodes.filter { it !in alertCodes.keys }
    require(codesNotFound.isEmpty()) {
      codesNotFound.joinToString(prefix = "Alert code(s) ", postfix = " not found")
    }
    return alertCodes
  }

  private fun resyncAudit(
    prisonNumber: String,
    request: List<ResyncAlert>,
    requestedAt: LocalDateTime,
    deleted: Collection<Alert>,
    created: Collection<Alert>,
  ) {
    resyncAuditRepository.save(
      ResyncAudit(
        prisonNumber,
        request = objectMapper.valueToTree(request),
        requestedAt = requestedAt,
        requestedBy = REQUESTED_BY_SYS,
        requestedByDisplayName = REQUESTED_BY_SYS,
        source = Source.NOMIS,
        completedAt = LocalDateTime.now(),
        alertsDeleted = deleted.map { it.id },
        alertsCreated = created.map { it.id },
      ),
    )
  }

  private fun ResyncAlertMerge.alertFor(prisonNumber: String, alertCodes: Map<String, AlertCode>): ResyncAlertMerge {
    with(resync) {
      val alert = Alert(
        alertCode = alertCodes[alertCode]!!,
        prisonNumber = prisonNumber,
        description = description,
        authorisedBy = authorisedBy,
        activeFrom = activeFrom,
        activeTo = activeTo,
        createdAt = createdAt,
        prisonCodeWhenCreated = null,
      )
      alert.let {
        it.lastModifiedAt = lastModifiedAt
        it.resync(
          createdBy = createdBy,
          createdByDisplayName = createdByDisplayName,
          lastModifiedBy = lastModifiedBy,
          lastModifiedByDisplayName = lastModifiedByDisplayName,
          this@alertFor.alert,
        )
        alertRepository.save(it)
        return ResyncAlertMerge(resync, it)
      }
    }
  }

  private fun ResyncAlertMerge.resynced() = ResyncedAlert(resync.offenderBookId, resync.alertSeq, alert!!.id)

  private fun List<ResyncAlert>.logActiveToBeforeActiveFrom(prisonNumber: String) {
    this.filter { it.activeTo?.isBefore(it.activeFrom) == true }.forEach {
      log.warn("Alert with sequence '${it.alertSeq}' for person with prison number '$prisonNumber' from booking with id '${it.offenderBookId}' has an active to date '${it.activeTo}' that is before the active from date '${it.activeFrom}'")
    }
  }

  private fun List<ResyncAlert>.logDuplicateActiveAlerts(prisonNumber: String) {
    with(filter { it.isActive() }.groupBy { it.alertCode }.filter { it.value.size > 1 }) {
      if (any()) {
        log.warn(
          "Person with prison number '$prisonNumber' has ${this.size} duplicate active alert(s) for code(s) ${
            this.map { "'${it.key}' (${it.value.size} active)" }.joinToString(", ")
          }",
        )
      }
    }
  }

  private fun Collection<Alert>.find(resync: ResyncAlert): Alert? = firstOrNull {
    it.alertCode.code == resync.alertCode && it.activeFrom == resync.activeFrom && it.activeTo == resync.activeTo
  }

  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }
}

private data class ResyncAlertMerge(val resync: ResyncAlert, val alert: Alert?)
