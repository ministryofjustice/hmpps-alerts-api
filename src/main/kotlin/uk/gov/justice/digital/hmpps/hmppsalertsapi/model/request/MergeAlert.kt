package uk.gov.justice.digital.hmpps.hmppsalertsapi.model.request

import com.fasterxml.jackson.annotation.JsonFormat
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.Positive
import jakarta.validation.constraints.Size
import java.time.LocalDate

@Schema(
  description = "The request body for merging a new alert for a person",
)
data class MergeAlert(
  @Schema(
    description = "The internal NOMIS id for the offender booking. " +
      "An alert in NOMIS is uniquely identified by the offender booking id and alert sequence." +
      "This is returned as part of the merged alert response for mapping between NOMIS and DPS.",
    example = "12345",
  )
  @field:Positive(message = "Offender book id must be supplied and be > 0")
  val offenderBookId: Long,

  @Schema(
    description = "The NOMIS alert sequence. " +
      "An alert in NOMIS is uniquely identified by the offender booking id and alert sequence." +
      "This is returned as part of the merged alert response for mapping between NOMIS and DPS.",
    example = "2",
  )
  @field:Positive(message = "Alert sequence must be supplied and be > 0")
  val alertSeq: Int,

  @Schema(
    description = "The alert code for the alert. A person should only have one alert using each code active at any one time " +
      "however this is not enforced during merging. The alert code must exist but can be inactive.",
    example = "ABC",
  )
  @field:Size(min = 1, max = 12, message = "Alert code must be supplied and be <= 12 characters")
  val alertCode: String,

  @Schema(
    description = "The description of the alert. " +
      "This is a free text field and can be used to provide additional information about the alert e.g. the reasons for adding it." +
      "It is limited to 4000 characters when merging an alert.",
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
      "The active from date can be in the past or the future, but should be on or before the active to date",
    example = "2021-09-27",
  )
  @JsonFormat(pattern = "yyyy-MM-dd")
  val activeFrom: LocalDate,

  @Schema(
    description = "The date the alert should be active until. " +
      "If not provided, the alert will be active indefinitely. " +
      "The active to date can be in the past or the future, but should be on or after the active from date",
    example = "2022-07-15",
  )
  @JsonFormat(pattern = "yyyy-MM-dd")
  val activeTo: LocalDate?,
)
