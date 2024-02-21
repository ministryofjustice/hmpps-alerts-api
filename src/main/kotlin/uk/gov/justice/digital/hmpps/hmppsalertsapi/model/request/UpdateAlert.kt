package uk.gov.justice.digital.hmpps.hmppsalertsapi.model.request

import com.fasterxml.jackson.annotation.JsonFormat
import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDate

@Schema(
  description = "The request body for updating an existing alert for a person",
)
data class UpdateAlert(
  @Schema(
    description = "The updated description of the alert. Will be ignored if null and will clear the description if empty. " +
      "This is a free text field and can be used to provide additional information about the alert e.g. the reasons for adding it." +
      "It is limited to 1000 characters.",
    example = "Alert description",
  )
  val description: String?,

  @Schema(
    description = "The updated user, staff member, approving person or organisation that authorised the alert to be added. " +
      "Will be ignored if null and will clear the authorised by value if empty. " +
      "This is a free text field and can be used to record the name of the person who authorised the alert. " +
      "It is limited to 40 characters.",
    example = "A. Nurse, An Agency",
  )
  val authorisedBy: String?,

  @Schema(
    description = "The date the alert should be active from. " +
      "If set to null i.e. cleared, the alert will be active from the current date. " +
      "The active from date can be in the past or the future, but must be before the active to date",
    example = "2021-09-27",
  )
  @JsonFormat(pattern = "yyyy-MM-dd")
  val activeFrom: LocalDate?,

  @Schema(
    description = "The date the alert should be active until. " +
      "If set to null i.e. cleared, the alert will be active indefinitely. " +
      "The active to date can be in the past or the future, but must be after the active from date",
    example = "2022-07-15",
  )
  @JsonFormat(pattern = "yyyy-MM-dd")
  val activeTo: LocalDate?,

  @Schema(
    description = "An additional comment to append to comments thread associated with the alert. Will be ignored if null or empty. " +
      "It is a free text field limited to 1000 characters.",
    example = "Additional user comment on the alert comment thread",
  )
  val appendComment: String?,
)
