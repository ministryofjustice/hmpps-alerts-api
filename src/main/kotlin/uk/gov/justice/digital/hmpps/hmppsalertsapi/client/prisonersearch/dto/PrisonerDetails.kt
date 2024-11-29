package uk.gov.justice.digital.hmpps.hmppsalertsapi.client.prisonersearch.dto

data class PrisonerDetails(
  val prisonerNumber: String,
  val firstName: String,
  val middleNames: String?,
  val lastName: String,
  val prisonId: String?,
  val status: String,
  val restrictedPatient: Boolean,
  val cellLocation: String?,
  val supportingPrisonId: String?,
)
