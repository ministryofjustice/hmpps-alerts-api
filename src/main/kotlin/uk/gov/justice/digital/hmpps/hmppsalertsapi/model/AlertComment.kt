package uk.gov.justice.digital.hmpps.hmppsalertsapi.model

import com.fasterxml.jackson.annotation.JsonFormat
import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDateTime
import java.util.UUID

@Schema(
  description = "A comment appended to an alert comment thread",
)
data class AlertComment(
  @Schema(
    description = "The unique identifier assigned to the comment",
    example = "476939e3-7cc1-4c5f-8f54-e7d055d1d50c",
  )
  val commentUuid: UUID,

  @Schema(
    description = "The comment text appended to the alert comment thread. " +
      "It is a free text field limited to 1000 characters.",
    example = "Additional user comment on the alert comment thread",
  )
  val description: String,

  @Schema(
    description = "The date and time the comment was created and appended to the alert comment thread",
    example = "2021-09-27T14:19:25",
  )
  @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
  val createdAt: LocalDateTime,

  @Schema(
    description = "The username of the user who created the comment and appended it to the alert comment thread",
    example = "USER1234",
  )
  val createdBy: String,
)
