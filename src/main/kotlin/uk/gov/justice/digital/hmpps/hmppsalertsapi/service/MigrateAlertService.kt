package uk.gov.justice.digital.hmpps.hmppsalertsapi.service

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppsalertsapi.config.ExistingActiveAlertWithCodeException
import uk.gov.justice.digital.hmpps.hmppsalertsapi.domain.toAlertEntity
import uk.gov.justice.digital.hmpps.hmppsalertsapi.domain.toAlertModel
import uk.gov.justice.digital.hmpps.hmppsalertsapi.model.Alert
import uk.gov.justice.digital.hmpps.hmppsalertsapi.model.MigratedAlert
import uk.gov.justice.digital.hmpps.hmppsalertsapi.model.request.MigrateAlert
import uk.gov.justice.digital.hmpps.hmppsalertsapi.model.request.MigrateAlertRequest
import uk.gov.justice.digital.hmpps.hmppsalertsapi.repository.AlertCodeRepository
import uk.gov.justice.digital.hmpps.hmppsalertsapi.repository.AlertRepository
import uk.gov.justice.digital.hmpps.hmppsalertsapi.entity.Alert as AlertEntity

@Service
class MigrateAlertService(
  private val alertCodeRepository: AlertCodeRepository,
  private val alertRepository: AlertRepository,
) {
  fun migrateAlert(migrateAlertRequest: MigrateAlertRequest): Alert {
    val alertCode = alertCodeRepository.findByCode(migrateAlertRequest.alertCode) ?: throw IllegalArgumentException("Alert code '${migrateAlertRequest.alertCode}' not found")
    val alert = migrateAlertRequest.toAlertEntity(alertCode)
    if (alert.isActive() || alert.willBecomeActive()) {
      alert.checkForExistingActiveAlert()
    }
    return alertRepository.saveAndFlush(alert).toAlertModel()
  }

  private fun AlertEntity.checkForExistingActiveAlert() =
    alertRepository.findByPrisonNumberAndAlertCodeCode(prisonNumber, alertCode.code)
      .any { it.isActive() || it.willBecomeActive() } && throw ExistingActiveAlertWithCodeException(prisonNumber, alertCode.code)

  fun migratePrisonerAlerts(prisonNumber: String, request: List<MigrateAlert>): List<MigratedAlert> {
    request.checkForNotFoundAlertCodes()

    return emptyList()
  }

  private fun List<MigrateAlert>.checkForNotFoundAlertCodes() =
    map { it.alertCode }.distinct().let { requestAlertCodes ->
      alertCodeRepository.findByCodeIn(requestAlertCodes).associateBy { it.code }.let { alertCodes ->
        requestAlertCodes.filterNot { alertCodes.containsKey(it) }.sorted()
      }
    }.run {
      if (this.isNotEmpty()) {
        throw IllegalArgumentException("Alert code(s) '${this.joinToString("', '") }' not found")
      }
    }
}
