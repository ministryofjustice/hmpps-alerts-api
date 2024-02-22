package uk.gov.justice.digital.hmpps.hmppsalertsapi.model

import com.fasterxml.jackson.annotation.JsonFormat
import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

@Schema(
  description = "An alert associated with a person",
)
data class Alert(
  @Schema(
    description = "The unique identifier assigned to the alert",
    example = "8cdadcf3-b003-4116-9956-c99bd8df6a00",
  )
  val alertUuid: UUID,

  @Schema(
    description = "The prison number of the person the alert is for. " +
      "Also referred to as the offender number, offender id or NOMS id.",
    example = "A1234AA",
  )
  val prisonNumber: String,

  @Schema(
    description = "The alert code for the alert. A person will only have one alert using each code active at any one time.",
  )
  val alertCode: AlertCodeSummary,

  @Schema(
    description = "The description of the alert. " +
      "It is a free text field and is used to provide additional information about the alert e.g. the reasons for adding it." +
      "It is limited to 1000 characters.",
    example = "Alert description",
  )
  val description: String?,

  @Schema(
    description = "The user, staff member, approving person or organisation that authorised the alert to be added. " +
      "It is a free text field and is used to record the name of the person who authorised the alert. " +
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
    description = "Indicates that the alert is active for the person. " +
      "Alerts are active if their active from date is in the past and their active to date is either null or in the future. " +
      "Note that this field is read only and cannot be set directly using the API.",
    example = "true",
  )
  val isActive: Boolean,

  @Schema(
    description = "The comments thread associated with the alert. " +
      "The comments are ordered by the date and time they were created, with the most recent comment first.",
  )
  val comments: Collection<Comment>,

  @Schema(
    description = "The date and time the alert was created",
    example = "2021-09-27T14:19:25",
  )
  @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
  val createdAt: LocalDateTime,

  @Schema(
    description = "The username of the user who created the alert",
    example = "USER1234",
  )
  val createdBy: String,

  @Schema(
    description = "The displayable name of the user who created the alert",
    example = "Firstname Lastname",
  )
  val createdByDisplayName: String,

  @Schema(
    description = "The date and time the alert was last modified",
    example = "2022-07-15T15:24:56",
  )
  @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
  val lastModifiedAt: LocalDateTime?,

  @Schema(
    description = "The username of the user who last modified the alert",
    example = "USER1234",
  )
  val lastModifiedBy: String?,

  @Schema(
    description = "The displayable name of the user who last modified the alert",
    example = "Firstname Lastname",
  )
  val lastModifiedByDisplayName: String?,
)
