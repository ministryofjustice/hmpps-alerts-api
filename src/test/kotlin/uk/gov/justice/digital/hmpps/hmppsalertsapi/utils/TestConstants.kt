package uk.gov.justice.digital.hmpps.hmppsalertsapi.utils

import uk.gov.justice.digital.hmpps.hmppsalertsapi.client.manageusers.dto.UserDetailsDto
import uk.gov.justice.digital.hmpps.hmppsalertsapi.client.prisonersearch.dto.PrisonerDetails
import uk.gov.justice.digital.hmpps.hmppsalertsapi.integration.wiremock.PRISON_CODE_MOORLANDS
import uk.gov.justice.digital.hmpps.hmppsalertsapi.integration.wiremock.TEST_USER
import uk.gov.justice.digital.hmpps.hmppsalertsapi.integration.wiremock.TEST_USER_NAME
import uk.gov.justice.digital.hmpps.hmppsalertsapi.utils.IdGenerator.cellLocation
import uk.gov.justice.digital.hmpps.hmppsalertsapi.utils.IdGenerator.prisonNumber
import java.util.UUID

const val ALERT_TYPE_SOCIAL_CARE = "A"
const val ALERT_TYPE_CODE_MEDICAL = "M"
const val ALERT_TYPE_CODE_OTHER = "O"
const val ALERT_TYPE_CODE_VULNERABILITY = "V"

const val ALERT_CODE_SOCIAL_CARE = "AS"
const val ALERT_CODE_VICTIM = "VI"
const val ALERT_CODE_POOR_COPER = "VU"
const val ALERT_CODE_READY_FOR_WORK = "ORFW"

const val ALERT_CODE_INACTIVE_COVID_REFUSING_TO_SHIELD = "URS"

fun userDetailsDto(username: String = TEST_USER, name: String = TEST_USER_NAME, uuid: UUID? = UUID.randomUUID()) = UserDetailsDto(username, true, name, "nomis", PRISON_CODE_MOORLANDS, "123", uuid)

fun prisoner(
  prisonerNumber: String = prisonNumber(),
  firstName: String = "First",
  middleNames: String? = "Middle",
  lastName: String = "Last",
  prisonId: String? = "LEI",
  status: String = "ACTIVE IN",
  restrictedPatient: Boolean = false,
  cellLocation: String? = cellLocation(),
  supportingPrisonId: String? = null,
) = PrisonerDetails(
  prisonerNumber,
  firstName,
  middleNames,
  lastName,
  prisonId,
  status,
  restrictedPatient,
  cellLocation,
  supportingPrisonId,
)
