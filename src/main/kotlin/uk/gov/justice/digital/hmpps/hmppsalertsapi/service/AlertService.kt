package uk.gov.justice.digital.hmpps.hmppsalertsapi.service

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.hmppsalertsapi.client.prisonersearch.PrisonerSearchClient
import uk.gov.justice.digital.hmpps.hmppsalertsapi.config.AlertNotFoundException
import uk.gov.justice.digital.hmpps.hmppsalertsapi.config.AlertRequestContext
import uk.gov.justice.digital.hmpps.hmppsalertsapi.config.ExistingActiveAlertWithCodeException
import uk.gov.justice.digital.hmpps.hmppsalertsapi.domain.toAlertEntity
import uk.gov.justice.digital.hmpps.hmppsalertsapi.domain.toAlertModel
import uk.gov.justice.digital.hmpps.hmppsalertsapi.domain.toAuditEventModel
import uk.gov.justice.digital.hmpps.hmppsalertsapi.model.AuditEvent
import uk.gov.justice.digital.hmpps.hmppsalertsapi.model.request.CreateAlert
import uk.gov.justice.digital.hmpps.hmppsalertsapi.model.request.UpdateAlert
import uk.gov.justice.digital.hmpps.hmppsalertsapi.repository.AlertCodeRepository
import uk.gov.justice.digital.hmpps.hmppsalertsapi.repository.AlertRepository
import java.util.UUID
import uk.gov.justice.digital.hmpps.hmppsalertsapi.model.Alert as AlertModel

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
          source = context.source,
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

  fun retrieveAlert(alertUuid: UUID): AlertModel =
    alertRepository.findByAlertUuid(alertUuid)?.toAlertModel() ?: throw AlertNotFoundException("Could not find alert with uuid $alertUuid")

  fun updateAlert(alertUuid: UUID, request: UpdateAlert, context: AlertRequestContext) =
    alertRepository.findByAlertUuid(alertUuid)?.let {
      alertRepository.saveAndFlush(
        it.update(
          description = request.description,
          authorisedBy = request.authorisedBy,
          activeFrom = request.activeFrom,
          activeTo = request.activeTo,
          appendComment = request.appendComment,
          updatedAt = context.requestAt,
          updatedBy = context.username,
          updatedByDisplayName = context.userDisplayName,
          source = context.source,
        ),
      ).toAlertModel()
    } ?: throw AlertNotFoundException("Could not find alert with ID $alertUuid")

  fun deleteAlert(alertUuid: UUID, context: AlertRequestContext) {
    val alert = alertRepository.findByAlertUuid(alertUuid) ?: throw AlertNotFoundException("Could not find alert with uuid $alertUuid")
    with(alert) {
      delete(
        deletedAt = context.requestAt,
        deletedBy = context.username,
        deletedByDisplayName = context.userDisplayName,
        source = context.source,
      )
    }
    alertRepository.saveAndFlush(alert)
  }

  fun retrieveAlertsForPrisonNumber(prisonNumber: String): Collection<AlertModel> =
    alertRepository.findAllByPrisonNumber(prisonNumber).map { it.toAlertModel() }

  fun retrieveAuditEventsForAlert(alertUuid: UUID): Collection<AuditEvent> =
    alertRepository.findByAlertUuid(alertUuid)?.let { alert ->
      alert.auditEvents().map { it.toAuditEventModel() }
    } ?: throw AlertNotFoundException("Could not find alert with uuid $alertUuid")
}
