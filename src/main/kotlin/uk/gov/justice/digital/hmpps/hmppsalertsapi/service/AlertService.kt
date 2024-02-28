package uk.gov.justice.digital.hmpps.hmppsalertsapi.service

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.hmppsalertsapi.client.prisonersearch.PrisonerSearchClient
import uk.gov.justice.digital.hmpps.hmppsalertsapi.config.AlertRequestContext
import uk.gov.justice.digital.hmpps.hmppsalertsapi.config.ExistingActiveAlertWithCodeException
import uk.gov.justice.digital.hmpps.hmppsalertsapi.domain.toAlertEntity
import uk.gov.justice.digital.hmpps.hmppsalertsapi.domain.toAlertModel
import uk.gov.justice.digital.hmpps.hmppsalertsapi.model.request.CreateAlert
import uk.gov.justice.digital.hmpps.hmppsalertsapi.repository.AlertCodeRepository
import uk.gov.justice.digital.hmpps.hmppsalertsapi.repository.AlertRepository

@Service
@Transactional
class AlertService(
  private val alertRepository: AlertRepository,
  private val alertCodeRepository: AlertCodeRepository,
  private val prisonerSearchClient: PrisonerSearchClient,
) {
  fun createAlert(request: CreateAlert, context: AlertRequestContext) =
    request.let {
      // Perform database checks first prior to checks that require API calls
      val alertCode = it.getAlertCode()
      it.checkForExistingActiveAlert()

      // Uses API call
      it.validatePrisonNumber()

      alertRepository.saveAndFlush(
        it.toAlertEntity(
          alertCode = alertCode,
          createdAt = context.requestAt,
          createdBy = context.username,
          createdByDisplayName = context.userDisplayName,
        ),
      ).toAlertModel()
    }

  private fun CreateAlert.getAlertCode() =
    alertCodeRepository.findByCode(alertCode)?.also {
      require(it.isActive()) { "Alert code '$alertCode' is inactive" }
    } ?: throw IllegalArgumentException("Alert code '$alertCode' not found")

  private fun CreateAlert.checkForExistingActiveAlert() =
    alertRepository.findByPrisonNumberAndAlertCodeCode(prisonNumber, alertCode)
      .any { it.isActive() || it.willBecomeActive() } && throw ExistingActiveAlertWithCodeException(prisonNumber, alertCode)

  private fun CreateAlert.validatePrisonNumber() =
    require(prisonerSearchClient.getPrisoner(prisonNumber) != null) { "Prison number '$prisonNumber' not found" }
}
