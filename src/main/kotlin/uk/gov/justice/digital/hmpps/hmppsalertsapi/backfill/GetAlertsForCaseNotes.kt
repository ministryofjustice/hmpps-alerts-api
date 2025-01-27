package uk.gov.justice.digital.hmpps.hmppsalertsapi.backfill

import jakarta.persistence.criteria.Join
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
  fun get(prisonNumber: String, from: LocalDate, to: LocalDate): List<CaseNoteAlert> =
    alertRepository.findAll(caseNoteSpecification(prisonNumber, from, to)).map { it.forCaseNotes() }

  private fun caseNoteSpecification(prisonNumber: String, from: LocalDate, to: LocalDate) =
    Specification<Alert> { alert, _, cb ->
      val pn = cb.equal(alert.get<String>("prisonNumber"), prisonNumber)
      val created = cb.between(alert.get("createdAt"), from, to)
      val activeFrom = cb.between(alert.get("activeFrom"), from, to)
      val activeTo = cb.between(alert.get("activeTo"), from, to)
      val code = alert.fetch<Alert, AlertCode>("alertCode")
      code.fetch<AlertCode, AlertType>("alertType")
      val audit = (alert.fetch<Alert, AuditEvent>("auditEvents") as Join<*, *>)
      val modified = cb.between(audit.get("actionedAt"), from, to)
      cb.and(pn, cb.or(created, activeFrom, activeTo, modified))
    }

  private fun Alert.typeInfo() = Pair(
    CodedDescription(alertCode.alertType.code, alertCode.alertType.description),
    CodedDescription(alertCode.code, alertCode.description),
  )

  private fun Alert.forCaseNotes(): CaseNoteAlert {
    val (type, subtype) = typeInfo()
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
      deactivationEvent()?.actionedAt,
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
)

data class CodedDescription(val code: String, val description: String)
