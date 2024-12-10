package uk.gov.justice.digital.hmpps.hmppsalertsapi.service

import org.springframework.context.ApplicationEventPublisher
import org.springframework.data.repository.findByIdOrNull
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.hmppsalertsapi.common.aop.PersonAlertsChanged
import uk.gov.justice.digital.hmpps.hmppsalertsapi.common.aop.PublishPersonAlertsChanged
import uk.gov.justice.digital.hmpps.hmppsalertsapi.entity.BulkPlan.Companion.BULK_ALERT_USERNAME
import uk.gov.justice.digital.hmpps.hmppsalertsapi.entity.BulkPlanRepository
import uk.gov.justice.digital.hmpps.hmppsalertsapi.entity.event.AlertCreatedEvent
import uk.gov.justice.digital.hmpps.hmppsalertsapi.entity.event.AlertDeactivatedEvent
import uk.gov.justice.digital.hmpps.hmppsalertsapi.entity.event.AlertUpdatedEvent
import uk.gov.justice.digital.hmpps.hmppsalertsapi.enumeration.AuditEventAction
import uk.gov.justice.digital.hmpps.hmppsalertsapi.repository.AuditEventRepository
import java.util.UUID

@Transactional
@Service
class AuditEventService(
  private val planRepository: BulkPlanRepository,
  private val auditEventRepository: AuditEventRepository,
  private val applicationEventPublisher: ApplicationEventPublisher,
) {
  @Async
  @PublishPersonAlertsChanged
  fun resendDomainEventsForPlan(planId: UUID) {
    planRepository.findByIdOrNull(planId)
      ?.takeIf { it.startedAt != null }
      ?.let {
        auditEventRepository.findByActionedByAndActionedAt(BULK_ALERT_USERNAME, it.startedAt!!)
      }?.mapNotNull {
        val alert = it.alert
        when (it.action) {
          AuditEventAction.CREATED -> AlertCreatedEvent(
            alert.id,
            alert.prisonNumber,
            alert.alertCode.code,
            it.actionedAt,
            it.source,
            it.actionedBy,
          ).also { e -> PersonAlertsChanged.registerChange(e.prisonNumber) }

          AuditEventAction.UPDATED -> AlertUpdatedEvent(
            alert.id,
            alert.prisonNumber,
            alert.alertCode.code,
            it.actionedAt,
            it.source,
            it.actionedBy,
            it.descriptionUpdated ?: false,
            it.authorisedByUpdated ?: false,
            it.activeFromUpdated ?: false,
            it.activeToUpdated ?: false,
          ).also { e -> PersonAlertsChanged.registerChange(e.prisonNumber) }

          AuditEventAction.INACTIVE -> AlertDeactivatedEvent(
            alert.id,
            alert.prisonNumber,
            alert.alertCode.code,
            it.actionedAt,
            it.source,
          ).also { e -> PersonAlertsChanged.registerChange(e.prisonNumber) }

          AuditEventAction.DELETED -> null
        }
      }
      ?.forEach(applicationEventPublisher::publishEvent)
  }
}
