package uk.gov.justice.digital.hmpps.hmppsalertsapi.utils

import uk.gov.justice.digital.hmpps.hmppsalertsapi.entity.AlertCode
import uk.gov.justice.digital.hmpps.hmppsalertsapi.entity.AlertType
import java.time.LocalDateTime

object EntityGenerator {
  val AT_VULNERABILITY = alertType(ALERT_TYPE_CODE_VULNERABILITY, "Vulnerability")
  val AC_VICTIM = alertCode(ALERT_CODE_VICTIM, "Victim")
  val AC_RESTRICTED = alertCode(
    code = ALERT_CODE_RESTRICTED,
    description = "Restricted",
    restricted = true
  )

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
  ) = AlertType(code, description, listSequence, createdAt, createdBy).apply {
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
    restricted: Boolean = false,
    createdAt: LocalDateTime = LocalDateTime.of(2006, 6, 28, 16, 19, 44),
    createdBy: String = "CREATED_BY",
    modifiedAt: LocalDateTime = LocalDateTime.of(2010, 3, 7, 16, 27, 58),
    modifiedBy: String = "MODIFIED_BY",
    deactivatedAt: LocalDateTime? = null,
    deactivatedBy: String? = null,
  ) = AlertCode(type, code, description, restricted, listSequence, createdAt, createdBy).apply {
    this.modifiedAt = modifiedAt
    this.modifiedBy = modifiedBy
    this.deactivatedAt = deactivatedAt
    this.deactivatedBy = deactivatedBy
  }
}
