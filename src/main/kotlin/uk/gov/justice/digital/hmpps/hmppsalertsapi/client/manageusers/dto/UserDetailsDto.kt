package uk.gov.justice.digital.hmpps.hmppsalertsapi.client.manageusers.dto

import java.util.UUID

data class UserDetailsDto(
  val username: String,
  val active: Boolean,
  val name: String,
  val authSource: String,
  val activeCaseLoadId: String?,
  val userId: String,
  val uuid: UUID?,
)
