package uk.gov.justice.digital.hmpps.hmppsalertsapi.model

import com.fasterxml.jackson.annotation.JsonFormat
import io.swagger.v3.oas.annotations.media.Schema
import uk.gov.justice.digital.hmpps.hmppsalertsapi.enumeration.AuditEventAction
import java.time.LocalDateTime

data class AuditEvent(
  @Schema(
    description = "The audit event type",
    example = "CREATED",
  )
  val action: AuditEventAction,

  @Schema(
    description = "A description of what has changed",
    example = "The active to date was updated from 2012-02-03 to 2012-04-05",
  )
  val description: String,

  @Schema(
    description = "When the event happened",
    example = "2021-09-27T14:19:25",
  )
  @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
  val actionedAt: LocalDateTime,

  @Schema(
    description = "The user id of the person who was audited",
    example = "AB1234AA",
  )
  val actionedBy: String,

  @Schema(
    description = "The friendly name of the person who was audited",
    example = "An Auditer",
  )
  val actionedByDisplayName: String,
)
