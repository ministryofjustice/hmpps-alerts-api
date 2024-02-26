package uk.gov.justice.digital.hmpps.hmppsalertsapi.service

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.hmppsalertsapi.client.prisonersearch.PrisonerSearchClient
import uk.gov.justice.digital.hmpps.hmppsalertsapi.config.AlertRequestContext
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
  private val prisonerSearchClient: PrisonerSearchClient,
) {
  fun createAlert(request: CreateAlert, context: AlertRequestContext): AlertModel {
    val prisoner = prisonerSearchClient.getPrisoner(request.prisonNumber)
    require(prisoner != null) { "Prisoner not found for prison number: ${request.prisonNumber}" }

    val alertCode = alertCodeRepository.findByCode(request.alertCode)
    require(alertCode != null) { "Alert code '${request.alertCode}' not found" }
    require(alertCode.isActive()) { "Alert code '${request.alertCode}' is inactive" }

    return alertRepository.save(
      request.toAlertEntity(
        alertCode = alertCode,
        createdBy = context.username,
        createdByDisplayName = context.userDisplayName,
      ),
    ).toAlertModel()
  }
}
