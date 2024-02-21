package uk.gov.justice.digital.hmpps.hmppsalertsapi.model

import com.fasterxml.jackson.annotation.JsonFormat
import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDateTime

@Schema(
  description = "An alert type used to categorise alerts",
)
data class AlertType(
  @Schema(
    description = "The short code for the alert type",
    example = "A",
  )
  val code: String,

  @Schema(
    description = "The description of the alert type",
    example = "Alert type description",
  )
  val description: String,

  @Schema(
    description = "The sequence number of the alert type. " +
      "Used for ordering alert types correctly in lists and drop downs. " +
      "A value of 0 indicates this is the default alert type",
    example = "3",
  )
  val listSequence: Int,

  @Schema(
    description = "Indicates that the alert type is active and can be used. " +
      "Inactive alert types are not returned by default in the API",
    example = "true",
  )
  val isActive: Boolean,

  @Schema(
    description = "The date and time the alert type was created",
    example = "2021-09-27T14:19:25",
  )
  @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
  val createdAt: LocalDateTime,

  @Schema(
    description = "The username of the user who created the alert type",
    example = "USER1234",
  )
  val createdBy: String,

  @Schema(
    description = "The date and time the alert type was last modified",
    example = "2022-07-15T15:24:56",
  )
  @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
  val modifiedAt: LocalDateTime?,

  @Schema(
    description = "The username of the user who last modified the alert type",
    example = "USER1234",
  )
  val modifiedBy: String?,

  @Schema(
    description = "The date and time the alert type was deactivated",
    example = "2023-11-08T09:53:34",
  )
  @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
  val deactivatedAt: LocalDateTime?,

  @Schema(
    description = "The username of the user who deactivated the alert type",
    example = "USER1234",
  )
  val deactivatedBy: String?,

  @Schema(
    description = "The alert codes associated with this alert type",
  )
  val alertCodes: Collection<AlertCode>,
)
