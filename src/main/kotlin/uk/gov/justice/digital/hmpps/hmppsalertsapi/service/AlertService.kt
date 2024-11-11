package uk.gov.justice.digital.hmpps.hmppsalertsapi.service

import com.microsoft.applicationinsights.TelemetryClient
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.hmppsalertsapi.client.prisonersearch.dto.PrisonerDto
import uk.gov.justice.digital.hmpps.hmppsalertsapi.common.aop.PersonAlertsChanged
import uk.gov.justice.digital.hmpps.hmppsalertsapi.common.aop.PublishPersonAlertsChanged
import uk.gov.justice.digital.hmpps.hmppsalertsapi.config.AlertRequestContext
import uk.gov.justice.digital.hmpps.hmppsalertsapi.domain.toAlertEntity
import uk.gov.justice.digital.hmpps.hmppsalertsapi.domain.toAlertModel
import uk.gov.justice.digital.hmpps.hmppsalertsapi.domain.toAuditEventModel
import uk.gov.justice.digital.hmpps.hmppsalertsapi.enumeration.Source
import uk.gov.justice.digital.hmpps.hmppsalertsapi.exceptions.AlreadyExistsException
import uk.gov.justice.digital.hmpps.hmppsalertsapi.exceptions.InvalidInputException
import uk.gov.justice.digital.hmpps.hmppsalertsapi.exceptions.NotFoundException
import uk.gov.justice.digital.hmpps.hmppsalertsapi.exceptions.verifyExists
import uk.gov.justice.digital.hmpps.hmppsalertsapi.model.Alert
import uk.gov.justice.digital.hmpps.hmppsalertsapi.model.AuditEvent
import uk.gov.justice.digital.hmpps.hmppsalertsapi.model.request.CreateAlert
import uk.gov.justice.digital.hmpps.hmppsalertsapi.model.request.UpdateAlert
import uk.gov.justice.digital.hmpps.hmppsalertsapi.repository.AlertCodeRepository
import uk.gov.justice.digital.hmpps.hmppsalertsapi.repository.AlertRepository
import uk.gov.justice.digital.hmpps.hmppsalertsapi.repository.AlertsFilter
import uk.gov.justice.digital.hmpps.hmppsalertsapi.repository.AuditEventRepository
import java.time.LocalDate
import java.util.UUID
import uk.gov.justice.digital.hmpps.hmppsalertsapi.model.Alert as AlertModel

@Service
@Transactional
class AlertService(
  private val alertRepository: AlertRepository,
  private val alertCodeRepository: AlertCodeRepository,
  private val auditEventRepository: AuditEventRepository,
  private val telemetryClient: TelemetryClient,
) {
  @PublishPersonAlertsChanged
  fun createAlert(prisoner: PrisonerDto, request: CreateAlert, allowInactiveCode: Boolean): Alert {
    val context = AlertRequestContext.get()
    val notNomis = context.source != Source.NOMIS
    val alertCode = request.getAlertCode(notNomis && !allowInactiveCode)

    if (notNomis) {
      check(request.dateRangeIsValid()) { "Active from must be before active to" }
      checkForExistingActiveAlert(prisoner.prisonerNumber, request.alertCode)
    }

    return alertRepository.save(request.toAlertEntity(context, prisoner.prisonerNumber, alertCode, prisoner.prisonId))
      .toAlertModel().apply {
        if (allowInactiveCode && !alertCode.isActive()) {
          telemetryClient.trackEvent(
            "INACTIVE_CODE_ALERT_CREATION",
            mapOf(
              "username" to context.username,
              "alertCode" to alertCode.code,
              "alertUuid" to alertUuid.toString(),
            ),
            mapOf(),
          )
        }
      }
  }

  private fun CreateAlert.dateRangeIsValid() = !(activeFrom?.isAfter(activeTo ?: activeFrom) ?: false)

  private fun CreateAlert.getAlertCode(activeOnly: Boolean = true) =
    verifyExists(alertCodeRepository.findByCode(alertCode)) {
      InvalidInputException("Alert code", alertCode)
    }.also {
      if (activeOnly) require(it.isActive()) { "Alert code is inactive" }
    }

  private fun checkForExistingActiveAlert(prisonNumber: String, alertCode: String) =
    alertRepository.findByPrisonNumberAndAlertCodeCode(prisonNumber, alertCode)
      .any { it.isActive() } && throw AlreadyExistsException("Alert", alertCode)

  fun retrieveAlert(alertUuid: UUID): AlertModel =
    alertRepository.findByIdOrNull(alertUuid)?.toAlertModel()
      ?: throw NotFoundException("Alert", alertUuid.toString())

  @PublishPersonAlertsChanged
  fun updateAlert(alertUuid: UUID, request: UpdateAlert, context: AlertRequestContext) =
    alertRepository.findByIdOrNull(alertUuid)?.let {
      alertRepository.save(
        it.update(
          description = request.description,
          authorisedBy = request.authorisedBy,
          activeFrom = request.activeFrom,
          activeTo = request.activeTo,
          updatedAt = context.requestAt,
          updatedBy = context.username,
          updatedByDisplayName = context.userDisplayName,
          source = context.source,
          activeCaseLoadId = context.activeCaseLoadId,
        ),
      ).toAlertModel()
    } ?: throw NotFoundException("Alert", alertUuid.toString())

  @PublishPersonAlertsChanged
  fun deleteAlert(alertUuid: UUID, context: AlertRequestContext) {
    val alert = alertRepository.findByIdOrNull(alertUuid)
      ?: throw NotFoundException("Alert", alertUuid.toString())
    with(alert) {
      delete(
        deletedAt = context.requestAt,
        deletedBy = context.username,
        deletedByDisplayName = context.userDisplayName,
        source = context.source,
        activeCaseLoadId = context.activeCaseLoadId,
      )
    }
    alertRepository.save(alert)
    PersonAlertsChanged.registerChange(alert.prisonNumber)
  }

  fun retrieveAlertsForPrisonNumber(
    prisonNumber: String,
    isActive: Boolean?,
    alertType: String?,
    alertCode: String?,
    activeFromStart: LocalDate?,
    activeFromEnd: LocalDate?,
    search: String?,
    pageable: Pageable,
  ): Page<AlertModel> =
    alertRepository.findAll(
      AlertsFilter(
        prisonNumber = prisonNumber,
        isActive = isActive,
        alertType = alertType,
        alertCode = alertCode,
        activeFromStart = activeFromStart,
        activeFromEnd = activeFromEnd,
        search = search,
      ),
      pageable = pageable,
    ).let { alerts ->
      val alertIds = alerts.content.map { it.id }
      val auditEvents =
        auditEventRepository.findAuditEventsByAlertIdInOrderByActionedAtDesc(alertIds).groupBy { it.alert.id }
      alerts.map { it.toAlertModel(auditEvents[it.id]) }
    }

  fun retrieveAuditEventsForAlert(alertUuid: UUID): Collection<AuditEvent> =
    alertRepository.findByIdOrNull(alertUuid)?.let { alert ->
      alert.auditEvents().map { it.toAuditEventModel() }
    } ?: throw NotFoundException("Alert", alertUuid.toString())

  fun retrieveAlertsForPrisonNumbers(prisonNumbers: Collection<String>) =
    alertRepository.findByPrisonNumberInOrderByActiveFromDesc(prisonNumbers).let { alerts ->
      val alertIds = alerts.map { it.id }
      val auditEvents =
        auditEventRepository.findAuditEventsByAlertIdInOrderByActionedAtDesc(alertIds).groupBy { it.alert.id }
      alerts.map { it.toAlertModel(auditEvents[it.id]) }
        .groupBy { it.prisonNumber }
    }
}
