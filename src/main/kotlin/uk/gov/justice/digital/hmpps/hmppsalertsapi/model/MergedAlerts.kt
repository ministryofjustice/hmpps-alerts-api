package uk.gov.justice.digital.hmpps.hmppsalertsapi.model

import io.swagger.v3.oas.annotations.media.Schema
import java.util.UUID

data class MergedAlerts(
  @Schema(
    description = "The identifiers associated with the merged alerts added to the prison number to merge to",
  )
  val alertsCreated: Collection<MergedAlert>,

  @Schema(
    description = "The unique identifiers of the deleted alerts associated with the prison number to merge from",
  )
  val alertsDeleted: Collection<UUID>,
)
