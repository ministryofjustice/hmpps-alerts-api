package uk.gov.justice.digital.hmpps.hmppsalertsapi.service

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.hmppsalertsapi.client.prisonersearch.PrisonerSearchClient
import uk.gov.justice.digital.hmpps.hmppsalertsapi.config.AlertNotFoundException
import uk.gov.justice.digital.hmpps.hmppsalertsapi.config.AlertRequestContext
import uk.gov.justice.digital.hmpps.hmppsalertsapi.config.ExistingActiveAlertWithCodeException
import uk.gov.justice.digital.hmpps.hmppsalertsapi.domain.toAlertEntity
import uk.gov.justice.digital.hmpps.hmppsalertsapi.domain.toAlertModel
import uk.gov.justice.digital.hmpps.hmppsalertsapi.entity.Alert
import uk.gov.justice.digital.hmpps.hmppsalertsapi.enumeration.AuditEventAction
import uk.gov.justice.digital.hmpps.hmppsalertsapi.model.request.CreateAlert
import uk.gov.justice.digital.hmpps.hmppsalertsapi.model.request.UpdateAlert
import uk.gov.justice.digital.hmpps.hmppsalertsapi.repository.AlertCodeRepository
import uk.gov.justice.digital.hmpps.hmppsalertsapi.repository.AlertRepository
import java.lang.StringBuilder
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

  fun updateAlert(alertUuid: UUID, request: UpdateAlert, requestContext: AlertRequestContext): AlertModel {
    val alert = alertRepository.findByAlertUuid(alertUuid) ?: throw AlertNotFoundException("Could not find alert with ID $alertUuid")
    with(alert) {
      val auditDescription = buildAuditDescription(this, request)
      description = request.description
      authorisedBy = request.authorisedBy
      if (request.activeFrom != null) {
        activeFrom = request.activeFrom
      }
      activeTo = request.activeTo
      if (request.appendComment != null) {
        addComment(comment = request.appendComment, createdBy = requestContext.username, createdByDisplayName = requestContext.userDisplayName)
      }
      auditEvent(
        AuditEventAction.UPDATED,
        actionedBy = requestContext.username,
        actionedByDisplayName = requestContext.userDisplayName,
        description = auditDescription,
      )
    }
    return alertRepository.saveAndFlush(alert).toAlertModel()
  }

  private fun buildAuditDescription(alert: Alert, request: UpdateAlert): String {
    val sb = StringBuilder()
    if (alert.description != request.description) {
      sb.appendLine("Updated alert description from ${alert.description} to ${request.description}.")
    }
    if (alert.authorisedBy != request.authorisedBy) {
      sb.appendLine("Updated authorised by from ${alert.authorisedBy} to ${request.authorisedBy}")
    }
    if (alert.activeFrom != request.activeFrom) {
      sb.appendLine("Updated active from from ${alert.activeFrom} to ${request.activeFrom}")
    }
    if (alert.activeTo != request.activeTo) {
      sb.appendLine("Updated active from from ${alert.activeTo} to ${request.activeTo}")
    }
    if (request.appendComment != null) {
      sb.appendLine("A new comment was added")
    }
    return sb.toString()
  }
}
