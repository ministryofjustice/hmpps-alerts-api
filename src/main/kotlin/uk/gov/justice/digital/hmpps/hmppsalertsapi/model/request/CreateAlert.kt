package uk.gov.justice.digital.hmpps.hmppsalertsapi.model.request

import com.fasterxml.jackson.annotation.JsonFormat
import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDate

@Schema(
  description = "The request body for creating a new alert for a person",
)
data class CreateAlert(
  @Schema(
    description = "The prison number of the person the alert is for. " +
      "Also referred to as the offender number, offender id or NOMS id.",
    example = "A1234AA",
  )
  val prisonNumber: String,

  @Schema(
    description = "The alert code for the alert. A person can only have one alert using each code active at any one time. " +
      "The alert code must exist and be active.",
    example = "ABC",
  )
  val alertCode: String,

  @Schema(
    description = "The description of the alert. " +
      "This is a free text field and can be used to provide additional information about the alert e.g. the reasons for adding it." +
      "It is limited to 1000 characters.",
    example = "Alert description",
  )
  val description: String?,

  @Schema(
    description = "The user, staff member, approving person or organisation that authorised the alert to be added. " +
      "This is a free text field and can be used to record the name of the person who authorised the alert. " +
      "It is limited to 40 characters.",
    example = "A. Nurse, An Agency",
  )
  val authorisedBy: String?,

  @Schema(
    description = "The date the alert should be active from. " +
      "If not provided, the alert will be active from the current date. " +
      "The active from date can be in the past or the future, but must be before the active to date",
    example = "2021-09-27",
  )
  @JsonFormat(pattern = "yyyy-MM-dd")
  val activeFrom: LocalDate?,

  @Schema(
    description = "The date the alert should be active until. " +
      "If not provided, the alert will be active indefinitely. " +
      "The active to date can be in the past or the future, but must be after the active from date",
    example = "2022-07-15",
  )
  @JsonFormat(pattern = "yyyy-MM-dd")
  val activeTo: LocalDate?,

  @Schema(
    description = "The username of the user who created the alert. " +
      "If not provided, the username of the user making the request will be used.",
    example = "USER1234",
  )
  val createdBy: String?,
)
