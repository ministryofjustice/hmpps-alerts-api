package uk.gov.justice.digital.hmpps.hmppsalertsapi.backfill

import org.springframework.data.jpa.domain.Specification
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppsalertsapi.entity.Alert
import uk.gov.justice.digital.hmpps.hmppsalertsapi.entity.AlertCode
import uk.gov.justice.digital.hmpps.hmppsalertsapi.entity.AlertType
import uk.gov.justice.digital.hmpps.hmppsalertsapi.entity.AuditEvent
import uk.gov.justice.digital.hmpps.hmppsalertsapi.repository.AlertRepository
import java.time.LocalDate
import java.time.LocalDate.parse
import java.time.LocalDateTime

@Service
class GetAlertsForCaseNotes(private val alertRepository: AlertRepository) {
  fun get(prisonNumber: String, from: LocalDate, to: LocalDate): List<CaseNoteAlert> = alertRepository.findAll(caseNoteSpecification(prisonNumber, from, to)).map { it.forCaseNotes() }

  private fun caseNoteSpecification(prisonNumber: String, from: LocalDate, to: LocalDate) = Specification<Alert> { alert, q, cb ->
    val pn = cb.equal(alert.get<String>("prisonNumber"), prisonNumber)
    val created = cb.between(alert.get("createdAt"), from, to)
    val activeFrom = cb.between(alert.get("activeFrom"), from, to)
    val activeTo = cb.between(alert.get("activeTo"), from, to)
    val code = alert.fetch<Alert, AlertCode>("alertCode")
    code.fetch<AlertCode, AlertType>("alertType")
    alert.fetch<Alert, AuditEvent>("auditEvents")
    val modified = q.subquery<AuditEvent>(AuditEvent::class.java)
    val audit = modified.from<AuditEvent>(AuditEvent::class.java)
    val auditAlert = audit.join<AuditEvent, Alert>("alert")
    modified.select(audit).where(
      cb.and(
        cb.equal(auditAlert.get<String>("id"), alert.get<String>("id")),
        cb.between(audit.get("actionedAt"), from, to),
      ),
    )
    cb.and(pn, cb.or(created, activeFrom, activeTo, cb.exists(modified)))
  }

  private fun Alert.typeInfo() = Pair(
    CodedDescription(alertCode.alertType.code, alertCode.alertType.description),
    CodedDescription(alertCode.code, alertCode.description),
  )

  private fun Alert.forCaseNotes(): CaseNoteAlert {
    val (type, subtype) = typeInfo()
    val created = createdAuditEvent()
    val deactivated = deactivationEvent()
    val activeFromChanged = auditEvents().lastOrNull { it.activeFromUpdated == true }
    val regex = "Updated active from from '(\\d{4}-\\d{2}-\\d{2})' to '(\\d{4}-\\d{2}-\\d{2})'".toRegex()
    val originalActiveFrom = activeFromChanged?.description?.let {
      regex.find(it)?.groupValues?.get(1)?.let { date -> parse(date) }
    }
    return CaseNoteAlert(
      type,
      subtype,
      prisonCodeWhenCreated,
      originalActiveFrom ?: activeFrom,
      activeTo,
      createdAt,
      deactivated?.actionedAt,
      ActionedBy(created.actionedBy, created.actionedByDisplayName),
      deactivated?.let { ActionedBy(it.actionedBy, it.actionedByDisplayName) },
    )
  }
}

data class CaseNoteAlert(
  val type: CodedDescription,
  val subType: CodedDescription,
  val prisonCode: String?,
  val activeFrom: LocalDate,
  val activeTo: LocalDate?,
  val createdAt: LocalDateTime,
  val madeInactiveAt: LocalDateTime?,
  val createdBy: ActionedBy,
  val deactivatedBy: ActionedBy?,
)

data class CodedDescription(val code: String, val description: String)
data class ActionedBy(val username: String, val displayName: String)
