package uk.gov.justice.digital.hmpps.hmppsalertsapi.model.request

import com.fasterxml.jackson.annotation.JsonFormat
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.Size
import java.time.LocalDate

@Schema(
  description = "The request body for updating an existing alert for a person",
)
data class UpdateAlert(
  @Schema(
    description = "The updated description of the alert. Will be ignored if null and will clear the description if empty. " +
      "This is a free text field and can be used to provide additional information about the alert e.g. the reasons for adding it." +
      "It is limited to 4000 characters.",
    example = "Alert description",
  )
  @field:Size(max = 4000, message = "Description must be <= 4000 characters")
  val description: String? = null,

  @Schema(
    description = "The updated user, staff member, approving person or organisation that authorised the alert to be added. " +
      "Will be ignored if null and will clear the authorised by value if empty. " +
      "This is a free text field and can be used to record the name of the person who authorised the alert. " +
      "It is limited to 40 characters.",
    example = "A. Nurse, An Agency",
  )
  @field:Size(max = 40, message = "Authorised by must be <= 40 characters")
  val authorisedBy: String? = null,

  @Schema(
    description = "The date the alert should be active from. " +
      "If set to null the field will be ignored" +
      "The active from date can be in the past or the future, but must be before the active to date",
    example = "2021-09-27",
  )
  @JsonFormat(pattern = "yyyy-MM-dd")
  val activeFrom: LocalDate? = null,

  @Schema(
    description = "The date the alert should be active until. " +
      "If set to null i.e. cleared, the alert will be active indefinitely. " +
      "The active to date can be in the past or the future, but must be after the active from date",
    example = "2022-07-15",
  )
  @JsonFormat(pattern = "yyyy-MM-dd")
  val activeTo: LocalDate? = null,

  @Schema(
    description = "An additional comment to append to comments thread associated with the alert. Will be ignored if null or empty. " +
      "It is a free text field limited to 1000 characters.",
    example = "Additional user comment on the alert comment thread",
  )
  @field:Size(max = 1000, message = "Append comment must be <= 1000 characters")
  val appendComment: String? = null,
)
