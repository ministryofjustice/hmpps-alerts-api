package uk.gov.justice.digital.hmpps.hmppsalertsapi.service

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppsalertsapi.config.ExistingActiveAlertWithCodeException
import uk.gov.justice.digital.hmpps.hmppsalertsapi.domain.toAlertEntity
import uk.gov.justice.digital.hmpps.hmppsalertsapi.domain.toAlertModel
import uk.gov.justice.digital.hmpps.hmppsalertsapi.model.Alert
import uk.gov.justice.digital.hmpps.hmppsalertsapi.model.request.MigrateAlertRequest
import uk.gov.justice.digital.hmpps.hmppsalertsapi.repository.AlertCodeRepository
import uk.gov.justice.digital.hmpps.hmppsalertsapi.repository.AlertRepository

@Service
class MigrateAlertService(
  private val alertCodeRepository: AlertCodeRepository,
  private val alertRepository: AlertRepository,
) {

  fun migrateAlert(migrateAlertRequest: MigrateAlertRequest): Alert {
    migrateAlertRequest.checkForExistingActiveAlert()
    val alertCode = alertCodeRepository.findByCode(migrateAlertRequest.alertCode) ?: throw IllegalArgumentException("Alert code '${migrateAlertRequest.alertCode}' not found")
    return alertRepository.saveAndFlush(migrateAlertRequest.toAlertEntity(alertCode)).toAlertModel()
  }

  private fun MigrateAlertRequest.checkForExistingActiveAlert() =
    alertRepository.findByPrisonNumberAndAlertCodeCode(prisonNumber, alertCode)
      .any { it.isActive() || it.willBecomeActive() } && throw ExistingActiveAlertWithCodeException(prisonNumber, alertCode)
}
