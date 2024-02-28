package uk.gov.justice.digital.hmpps.hmppsalertsapi.utils

import uk.gov.justice.digital.hmpps.hmppsalertsapi.client.usermanagement.dto.UserDetailsDto
import uk.gov.justice.digital.hmpps.hmppsalertsapi.entity.AlertCode
import uk.gov.justice.digital.hmpps.hmppsalertsapi.entity.AlertType
import uk.gov.justice.digital.hmpps.hmppsalertsapi.integration.wiremock.TEST_USER
import uk.gov.justice.digital.hmpps.hmppsalertsapi.integration.wiremock.TEST_USER_NAME
import uk.gov.justice.digital.hmpps.hmppsalertsapi.model.AlertCodeSummary
import java.time.LocalDateTime
import java.util.UUID

const val ALERT_CODE_ADULT_AT_RISK = "AAR"
const val ALERT_CODE_SOCIAL_CARE = "AS"
const val ALERT_CODE_VICTIM = "VI"
const val ALERT_CODE_ISOLATED_PRISONER = "VIP"
const val ALERT_CODE_POOR_COPER = "VU"
const val ALERT_CODE_HIDDEN_DISABILITY = "HID"

const val ALERT_CODE_SECURE_ALERT_OCG_NOMINAL = "DOCGM"

const val ALERT_CODE_INACTIVE_COVID_REFUSING_TO_SHIELD = "URS"

fun userDetailsDto(username: String = TEST_USER, name: String = TEST_USER_NAME, uuid: UUID? = UUID.randomUUID()) =
  UserDetailsDto(username, true, name, "nomis", "123", uuid)

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

fun alertCodeRefusingToShieldInactive() =
  AlertCode(
    152,
    alertTypeCovidUnitManagement(),
    ALERT_CODE_INACTIVE_COVID_REFUSING_TO_SHIELD,
    "Refusing to shield",
    1,
    LocalDateTime.of(2020, 6, 4, 14, 13, 41),
    "CREATED_BY",
  ).apply {
    modifiedAt = LocalDateTime.of(2023, 11, 23, 16, 57, 34)
    modifiedBy = "MODIFIED_BY"
    deactivatedAt = LocalDateTime.of(2023, 11, 23, 0, 0, 0)
    modifiedBy = "DEACTIVATED_BY"
  }

fun alertCodeVictimSummary() =
  AlertCodeSummary(
    "V",
    ALERT_CODE_VICTIM,
    "Victim",
    6,
    true,
  )
