package uk.gov.justice.digital.hmpps.hmppsalertsapi.model.request

import com.fasterxml.jackson.annotation.JsonFormat
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.Size
import uk.gov.justice.digital.hmpps.hmppsalertsapi.validator.DateComparison
import java.time.LocalDate
import java.time.LocalDateTime

@DateComparison("Active to must be on or after active from")
data class MigrateAlert(
  val offenderBookId: Int,
  val bookingSeq: Int,
  val alertSeq: Int,

  @Schema(
    description = "The alert code for the alert. A person can only have one alert using each code active at any one time. " +
      "The alert code must exist but can be inactive when migrating an alert.",
    example = "ABC",
  )
  val alertCode: String,

  @Schema(
    description = "The description of the alert. " +
      "This is a free text field and can be used to provide additional information about the alert e.g. the reasons for adding it." +
      "It is limited to 4000 characters when migrating an alert.",
    example = "Alert description",
  )
  @field:Size(max = 4000, message = "Description must be <= 4000 characters")
  val description: String?,

  @Schema(
    description = "The user, staff member, approving person or organisation that authorised the alert to be added. " +
      "This is a free text field and can be used to record the name of the person who authorised the alert. " +
      "It is limited to 40 characters.",
    example = "A. Nurse, An Agency",
  )
  @field:Size(max = 40, message = "Authorised by must be <= 40 characters")
  val authorisedBy: String?,

  @Schema(
    description = "The date the alert should be active from. " +
      "The active from date can be in the past or the future, but must be on or before the active to date",
    example = "2021-09-27",
  )
  @JsonFormat(pattern = "yyyy-MM-dd")
  val activeFrom: LocalDate,

  @Schema(
    description = "The date the alert should be active until. " +
      "If not provided, the alert will be active indefinitely. " +
      "The active to date can be in the past or the future, but must be on or after the active from date",
    example = "2022-07-15",
  )
  @JsonFormat(pattern = "yyyy-MM-dd")
  val activeTo: LocalDate?,

  @Schema(
    description = "The date and time the alert was created.",
    example = "2022-07-15'H'23:03:01.123456",
  )
  val createdAt: LocalDateTime,

  @Schema(
    description = "The user id of the person who created the alert.",
    example = "AB11DZ",
  )
  val createdBy: String,

  @Schema(
    description = "The displayable name of the person who created the alert.",
    example = "C Reated",
  )
  val createdByDisplayName: String,

  @Schema(
    description = "The date and time the alert was updated. Only provide if the alert has been updated since creation.",
    example = "2022-07-15'H'23:03:01.123456",
  )
  val updatedAt: LocalDateTime?,

  @Schema(
    description = "The user id of the person who updated the alert. Required if updated at has been supplied.",
    example = "AB11DZ",
  )
  val updatedBy: String?,

  @Schema(
    description = "The displayable name of the person who updated the alert. Required if updated at has been supplied.",
    example = "Up Dated",
  )
  val updatedByDisplayName: String?,
)
