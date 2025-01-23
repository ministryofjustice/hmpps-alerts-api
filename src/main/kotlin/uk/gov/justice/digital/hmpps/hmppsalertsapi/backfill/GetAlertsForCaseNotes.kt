package uk.gov.justice.digital.hmpps.hmppsalertsapi.backfill

import org.springframework.data.jpa.domain.Specification
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppsalertsapi.entity.Alert
import uk.gov.justice.digital.hmpps.hmppsalertsapi.entity.AlertCode
import uk.gov.justice.digital.hmpps.hmppsalertsapi.entity.AlertType
import uk.gov.justice.digital.hmpps.hmppsalertsapi.entity.AuditEvent
import uk.gov.justice.digital.hmpps.hmppsalertsapi.repository.AlertRepository
import java.time.LocalDate
import java.time.LocalDateTime

@Service
class GetAlertsForCaseNotes(private val alertRepository: AlertRepository) {
  fun get(prisonNumber: String, from: LocalDate, to: LocalDate): List<CaseNoteAlert> =
    alertRepository.findAll(caseNoteSpecification(prisonNumber, from, to)).map { it.forCaseNotes() }

  private fun caseNoteSpecification(prisonNumber: String, from: LocalDate, to: LocalDate) =
    Specification<Alert> { alert, _, cb ->
      val pn = cb.equal(alert.get<String>("prisonNumber"), prisonNumber)
      val created = cb.between(alert.get("createdAt"), from, to)
      val modified = cb.between(alert.get("lastModifiedAt"), from, to)
      val activeFrom = cb.between(alert.get("activeFrom"), from, to)
      val activeTo = cb.between(alert.get("activeTo"), from, to)
      val code = alert.fetch<Alert, AlertCode>("alertCode")
      code.fetch<AlertCode, AlertType>("alertType")
      alert.fetch<Alert, AuditEvent>("auditEvents")
      cb.and(pn, cb.or(created, modified, activeFrom, activeTo))
    }

  private fun Alert.typeInfo() = Pair(
    CodedDescription(alertCode.alertType.code, alertCode.alertType.description),
    CodedDescription(alertCode.code, alertCode.description),
  )

  private fun Alert.forCaseNotes(): CaseNoteAlert {
    val (type, subtype) = typeInfo()
    return CaseNoteAlert(type, subtype, prisonCodeWhenCreated, activeFrom, activeTo, createdAt, lastModifiedAt)
  }
}

data class CaseNoteAlert(
  val type: CodedDescription,
  val subType: CodedDescription,
  val prisonCode: String?,
  val activeFrom: LocalDate,
  val activeTo: LocalDate?,
  val createdAt: LocalDateTime,
  val lastModifiedAt: LocalDateTime?,
)

data class CodedDescription(val code: String, val description: String)
