package uk.gov.justice.digital.hmpps.hmppsalertsapi.utils

import uk.gov.justice.digital.hmpps.hmppsalertsapi.client.manageusers.dto.UserDetailsDto
import uk.gov.justice.digital.hmpps.hmppsalertsapi.domain.ALERT_CODE_SECURITY_ALERT_OCG_NOMINAL
import uk.gov.justice.digital.hmpps.hmppsalertsapi.entity.AlertCode
import uk.gov.justice.digital.hmpps.hmppsalertsapi.entity.AlertType
import uk.gov.justice.digital.hmpps.hmppsalertsapi.enumeration.BulkCreateAlertCleanupMode.KEEP_ALL
import uk.gov.justice.digital.hmpps.hmppsalertsapi.enumeration.BulkCreateAlertMode.ADD_MISSING
import uk.gov.justice.digital.hmpps.hmppsalertsapi.integration.wiremock.PRISON_CODE_MOORLANDS
import uk.gov.justice.digital.hmpps.hmppsalertsapi.integration.wiremock.PRISON_NUMBER
import uk.gov.justice.digital.hmpps.hmppsalertsapi.integration.wiremock.TEST_USER
import uk.gov.justice.digital.hmpps.hmppsalertsapi.integration.wiremock.TEST_USER_NAME
import uk.gov.justice.digital.hmpps.hmppsalertsapi.model.AlertCodeSummary
import uk.gov.justice.digital.hmpps.hmppsalertsapi.model.request.BulkCreateAlerts
import uk.gov.justice.digital.hmpps.hmppsalertsapi.model.request.MigrateAlert
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

const val ALERT_TYPE_SOCIAL_CARE = "A"
const val ALERT_TYPE_CODE_MEDICAL = "M"
const val ALERT_TYPE_CODE_OTHER = "O"
const val ALERT_TYPE_CODE_VULNERABILITY = "V"

const val ALERT_CODE_ADULT_AT_RISK = "AAR"
const val ALERT_CODE_SOCIAL_CARE = "AS"
const val ALERT_CODE_VICTIM = "VI"
const val ALERT_CODE_ISOLATED_PRISONER = "VIP"
const val ALERT_CODE_POOR_COPER = "VU"
const val ALERT_CODE_HIDDEN_DISABILITY = "HID"
const val ALERT_CODE_READY_FOR_WORK = "ORFW"

const val ALERT_CODE_INACTIVE_COVID_REFUSING_TO_SHIELD = "URS"

val DEFAULT_UUID = UUID.fromString("00000000-0000-0000-0000-000000000000")

fun userDetailsDto(username: String = TEST_USER, name: String = TEST_USER_NAME, uuid: UUID? = UUID.randomUUID()) =
  UserDetailsDto(username, true, name, "nomis", PRISON_CODE_MOORLANDS, "123", uuid)

fun alertTypeVulnerability() =
  AlertType(
    18,
    "V",
    "Vulnerability",
    6,
    LocalDateTime.of(2006, 6, 28, 16, 19, 42),
    "CREATED_BY",
  ).apply {
    modifiedAt = LocalDateTime.of(2010, 3, 7, 16, 27, 58)
    modifiedBy = "MODIFIED_BY"
  }

fun alertTypeVulnerabilityDeactivated() =
  AlertType(
    18,
    "V",
    "Vulnerability",
    6,
    LocalDateTime.of(2006, 6, 28, 16, 19, 42),
    "CREATED_BY",
  ).apply {
    modifiedAt = LocalDateTime.of(2010, 3, 7, 16, 27, 58)
    modifiedBy = "MODIFIED_BY"
    deactivatedAt = LocalDateTime.of(2010, 3, 7, 16, 27, 58)
    deactivatedBy = "MODIFIED_BY"
  }

fun alertTypeCovidUnitManagement() =
  AlertType(
    17,
    "U",
    "COVID unit management",
    4,
    LocalDateTime.of(2020, 6, 4, 14, 12, 26),
    "CREATED_BY",
  ).apply {
    modifiedAt = LocalDateTime.of(2023, 11, 23, 16, 57, 42)
    modifiedBy = "MODIFIED_BY"
    deactivatedAt = LocalDateTime.of(2023, 11, 23, 0, 0, 0)
    modifiedBy = "DEACTIVATED_BY"
  }

fun alertCodeVictim() =
  AlertCode(
    158,
    alertTypeVulnerability(),
    ALERT_CODE_VICTIM,
    "Victim",
    6,
    LocalDateTime.of(2006, 6, 28, 16, 19, 44),
    "CREATED_BY",
  ).apply {
    modifiedAt = LocalDateTime.of(2010, 3, 7, 16, 27, 58)
    modifiedBy = "MODIFIED_BY"
  }

fun alertCodeVictimDeactivated() =
  AlertCode(
    158,
    alertTypeVulnerability(),
    ALERT_CODE_VICTIM,
    "Victim",
    6,
    LocalDateTime.of(2006, 6, 28, 16, 19, 44),
    "CREATED_BY",
  ).apply {
    modifiedAt = LocalDateTime.of(2010, 3, 7, 16, 27, 58)
    modifiedBy = "MODIFIED_BY"
    deactivatedAt = LocalDateTime.of(2010, 3, 7, 16, 27, 58)
    deactivatedBy = "MODIFIED_BY"
  }

fun alertCodeVictimSummary() =
  AlertCodeSummary(
    ALERT_TYPE_CODE_VULNERABILITY,
    "Vulnerability",
    ALERT_CODE_VICTIM,
    "Victim",
  )

fun migrateAlert() =
  MigrateAlert(
    offenderBookId = 12345,
    bookingSeq = 1,
    alertSeq = 2,
    alertCode = ALERT_CODE_VICTIM,
    description = "Alert description",
    authorisedBy = "A. Nurse, An Agency",
    activeFrom = LocalDate.now().minusDays(2),
    activeTo = null,
    createdAt = LocalDateTime.now().minusDays(2).withNano(0),
    createdBy = "AB11DZ",
    createdByDisplayName = "C REATED",
    updatedAt = null,
    updatedBy = null,
    updatedByDisplayName = null,
  )

fun bulkCreateAlertRequest() =
  BulkCreateAlerts(
    prisonNumbers = listOf(PRISON_NUMBER),
    alertCode = ALERT_CODE_SECURITY_ALERT_OCG_NOMINAL,
    mode = ADD_MISSING,
    cleanupMode = KEEP_ALL,
  )
