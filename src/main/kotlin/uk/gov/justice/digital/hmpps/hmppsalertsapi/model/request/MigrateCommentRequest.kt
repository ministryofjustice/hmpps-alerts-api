package uk.gov.justice.digital.hmpps.hmppsalertsapi.model.request

import com.fasterxml.jackson.annotation.JsonFormat
import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDateTime

data class MigrateCommentRequest(
  @Schema(
    description = "The text added at the comment",
    example = "This is a comment",
  )
  val comment: String,
  @Schema(
    description = "The date the alert was created",
    example = "2022-07-15'H'23:03:01",
  )
  @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
  val createdAt: LocalDateTime,

  @Schema(
    description = "The user ID of the person who created the alert",
    example = "AB11DZ",
  )
  val createdBy: String,

  @Schema(
    description = "The friendly name of the person who created the alert",
    example = "C Reated",
  )
  val createdByDisplayName: String,
)
