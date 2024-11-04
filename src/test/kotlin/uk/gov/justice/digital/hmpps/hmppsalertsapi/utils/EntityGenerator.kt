package uk.gov.justice.digital.hmpps.hmppsalertsapi.utils

import uk.gov.justice.digital.hmpps.hmppsalertsapi.IdGenerator.newUuid
import uk.gov.justice.digital.hmpps.hmppsalertsapi.entity.Alert
import uk.gov.justice.digital.hmpps.hmppsalertsapi.entity.AlertCode
import uk.gov.justice.digital.hmpps.hmppsalertsapi.entity.AlertType
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

object EntityGenerator {
  val AT_VULNERABILITY = alertType(ALERT_TYPE_CODE_VULNERABILITY, "Vulnerability")
  val AC_VICTIM = alertCode(ALERT_CODE_VICTIM, "Victim")

  fun alertType(
    code: String,
    description: String = "Description of $code",
    listSequence: Int = 6,
    createdAt: LocalDateTime = LocalDateTime.of(2006, 6, 28, 16, 19, 42),
    createdBy: String = "CREATED_BY",
    modifiedAt: LocalDateTime = LocalDateTime.of(2010, 3, 7, 16, 27, 58),
    modifiedBy: String = "MODIFIED_BY",
    deactivatedAt: LocalDateTime? = null,
    deactivatedBy: String? = null,
    id: Long = IdGenerator.newId(),
  ) = AlertType(id, code, description, listSequence, createdAt, createdBy).apply {
    this.modifiedAt = modifiedAt
    this.modifiedBy = modifiedBy
    this.deactivatedAt = deactivatedAt
    this.deactivatedBy = deactivatedBy
  }

  fun alertCode(
    code: String,
    description: String = "Description of $code",
    type: AlertType = AT_VULNERABILITY,
    listSequence: Int = 6,
    createdAt: LocalDateTime = LocalDateTime.of(2006, 6, 28, 16, 19, 44),
    createdBy: String = "CREATED_BY",
    modifiedAt: LocalDateTime = LocalDateTime.of(2010, 3, 7, 16, 27, 58),
    modifiedBy: String = "MODIFIED_BY",
    deactivatedAt: LocalDateTime? = null,
    deactivatedBy: String? = null,
    id: Long = IdGenerator.newId(),
  ) = AlertCode(id, type, code, description, listSequence, createdAt, createdBy).apply {
    this.modifiedAt = modifiedAt
    this.modifiedBy = modifiedBy
    this.deactivatedAt = deactivatedAt
    this.deactivatedBy = deactivatedBy
  }

  fun alert(
    prisonNumber: String,
    alertCode: AlertCode = AC_VICTIM,
    description: String = "A description of the prisoner alert",
    authorisedBy: String? = "A Person",
    activeFrom: LocalDate = LocalDate.now().minusDays(1),
    activeTo: LocalDate? = null,
    createdAt: LocalDateTime = LocalDateTime.now(),
    deletedAt: LocalDateTime? = null,
    alertUuid: UUID = newUuid(),
  ) = Alert(alertCode, prisonNumber, description, authorisedBy, activeFrom, activeTo, createdAt, null, alertUuid)
    .apply { set(::deletedAt, deletedAt) }
}
