package uk.gov.justice.digital.hmpps.hmppsalertsapi.service

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.hmppsalertsapi.domain.toAlertEntity
import uk.gov.justice.digital.hmpps.hmppsalertsapi.domain.toAlertModel
import uk.gov.justice.digital.hmpps.hmppsalertsapi.model.request.CreateAlert
import uk.gov.justice.digital.hmpps.hmppsalertsapi.repository.AlertCodeRepository
import uk.gov.justice.digital.hmpps.hmppsalertsapi.repository.AlertRepository
import uk.gov.justice.digital.hmpps.hmppsalertsapi.model.Alert as AlertModel

@Service
@Transactional
class AlertService(
  private val alertRepository: AlertRepository,
  private val alertCodeRepository: AlertCodeRepository,
) {
  fun createAlert(request: CreateAlert): AlertModel {
    val alertCode = alertCodeRepository.findByCode(request.alertCode)
    require(alertCode != null) { "Alert code not found: ${request.alertCode}" }

    return alertRepository.save(
      request.toAlertEntity(
        alertCode = alertCode,
        createdBy = request.createdBy ?: "GET_FROM_REQUEST",
        // TODO: Get the display name from the user service
        createdByDisplayName = "Firstname Lastname",
      ),
    ).toAlertModel()
  }
}
