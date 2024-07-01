package uk.gov.justice.digital.hmpps.hmppsalertsapi.service

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.hmppsalertsapi.common.aop.PublishPersonAlertsChanged
import uk.gov.justice.digital.hmpps.hmppsalertsapi.entity.Alert
import uk.gov.justice.digital.hmpps.hmppsalertsapi.entity.AlertCode
import uk.gov.justice.digital.hmpps.hmppsalertsapi.enumeration.Source
import uk.gov.justice.digital.hmpps.hmppsalertsapi.model.ResyncedAlert
import uk.gov.justice.digital.hmpps.hmppsalertsapi.model.request.ResyncAlert
import uk.gov.justice.digital.hmpps.hmppsalertsapi.repository.AlertCodeRepository
import uk.gov.justice.digital.hmpps.hmppsalertsapi.repository.AlertRepository
import java.util.UUID

@Transactional
@Service
class ResyncAlertsService(
  private val alertRepository: AlertRepository,
  private val alertCodeRepository: AlertCodeRepository,
) {
  @PublishPersonAlertsChanged
  fun resyncAlerts(prisonNumber: String, alerts: List<ResyncAlert>): List<ResyncedAlert> {
    val alertCodes = getValidatedAlertCodes(alerts)
    val existingAlerts = alertRepository.findByPrisonNumber(prisonNumber)
    alerts.logActiveToBeforeActiveFrom(prisonNumber)
    alerts.logDuplicateActiveAlerts(prisonNumber)
    val newAlerts = alerts.map { ResyncAlertMerge(it, existingAlerts.find(it)) }.map { it.alertFor(prisonNumber, alertCodes) }
    existingAlerts.forEach {
      it.delete(
        deletedBy = "SYS",
        deletedByDisplayName = "SYS",
        source = Source.NOMIS,
        activeCaseLoadId = null,
        description = "Alert deleted via resync",
      )
      alertRepository.saveAll(existingAlerts)
    }
    return newAlerts
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

  private fun ResyncAlertMerge.alertFor(prisonNumber: String, alertCodes: Map<String, AlertCode>): ResyncedAlert {
    with(resync) {
      val alert = Alert(
        alertUuid = UUID.randomUUID(),
        alertCode = alertCodes[alertCode]!!,
        prisonNumber = prisonNumber,
        description = description,
        authorisedBy = authorisedBy,
        activeFrom = activeFrom,
        activeTo = activeTo,
        createdAt = createdAt,
      )
      alert.let {
        it.lastModifiedAt = lastModifiedAt
        it.resync(
          createdBy = createdBy,
          createdByDisplayName = createdByDisplayName,
          lastModifiedBy = lastModifiedBy,
          lastModifiedByDisplayName = lastModifiedByDisplayName,
          original,
        )
        alertRepository.save(it)
        return ResyncedAlert(offenderBookId, alertSeq, it.alertUuid)
      }
    }
  }

  private fun List<ResyncAlert>.logActiveToBeforeActiveFrom(prisonNumber: String) {
    this.filter { it.activeTo?.isBefore(it.activeFrom) == true }.forEach {
      log.warn("Alert with sequence '${it.alertSeq}' for person with prison number '$prisonNumber' from booking with id '${it.offenderBookId}' has an active to date '${it.activeTo}' that is before the active from date '${it.activeFrom}'")
    }
  }

  private fun List<ResyncAlert>.logDuplicateActiveAlerts(prisonNumber: String) {
    this.filter { it.isActive() }.groupBy { it.alertCode }.filter { it.value.size > 1 }.run {
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

private data class ResyncAlertMerge(val resync: ResyncAlert, val original: Alert?)
