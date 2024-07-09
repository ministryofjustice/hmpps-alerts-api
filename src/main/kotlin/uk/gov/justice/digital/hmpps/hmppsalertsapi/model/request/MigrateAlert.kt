package uk.gov.justice.digital.hmpps.hmppsalertsapi.model.request

import com.fasterxml.jackson.annotation.JsonAlias
import com.fasterxml.jackson.annotation.JsonFormat
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.Positive
import jakarta.validation.constraints.Size
import uk.gov.justice.digital.hmpps.hmppsalertsapi.validator.LastModifiedByDisplayNameRequired
import uk.gov.justice.digital.hmpps.hmppsalertsapi.validator.LastModifiedByRequired
import uk.gov.justice.digital.hmpps.hmppsalertsapi.validator.Modifiable
import java.time.LocalDate
import java.time.LocalDateTime

@Schema(
  description = "The request body for migrating an alert from NOMIS to DPS",
)
@LastModifiedByRequired("Updated by is required when updated at is supplied")
@LastModifiedByDisplayNameRequired("Updated by display name is required when updated at is supplied")
data class MigrateAlert(
  @Schema(
    description = "The internal NOMIS id for the offender booking. " +
      "An alert in NOMIS is uniquely identified by the offender booking id and alert sequence." +
      "This is returned as part of the migrated alert response for mapping between NOMIS and DPS.",
    example = "12345",
  )
  @field:Positive(message = "Offender book id must be supplied and be > 0")
  val offenderBookId: Long,

  @Schema(
    description = "The sequence of the NOMIS offender booking. " +
      "A sequence of 1 means the alert is from the current booking. A sequence of > 1 means the alert is from a historic booking." +
      "This is returned as part of the migrated alert response for mapping between NOMIS and DPS.",
    example = "1",
  )
  @field:Positive(message = "Booking sequence must be supplied and be > 0")
  val bookingSeq: Int,

  @Schema(
    description = "The NOMIS alert sequence. " +
      "An alert in NOMIS is uniquely identified by the offender booking id and alert sequence." +
      "This is returned as part of the migrated alert response for mapping between NOMIS and DPS.",
    example = "2",
  )
  @field:Positive(message = "Alert sequence must be supplied and be > 0")
  val alertSeq: Int,

  @Schema(
    description = "The alert code for the alert. A person should only have one alert using each code active at any one time " +
      "however this is not enforced during migration. The alert code must exist but can be inactive.",
    example = "ABC",
  )
  @field:Size(min = 1, max = 12, message = "Alert code must be supplied and be <= 12 characters")
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

  @Schema(
    description = "The date and time the alert was created.",
    example = "2022-07-15'H'23:03:01.123456",
  )
  val createdAt: LocalDateTime,

  @Schema(
    description = "The user id of the person who created the alert.",
    example = "AB11DZ",
  )
  @field:Size(min = 1, max = 32, message = "Created by must be supplied and be <= 32 characters")
  val createdBy: String,

  @Schema(
    description = "The displayable name of the person who created the alert.",
    example = "C Reated",
  )
  @field:Size(min = 1, max = 255, message = "Created by display name must be supplied and be <= 255 characters")
  val createdByDisplayName: String,

  @Schema(
    description = "The date and time the alert was updated. Only provide if the alert has been updated since creation.",
    example = "2022-07-15'H'23:03:01.123456",
  )
  @JsonAlias("updatedAt")
  override val lastModifiedAt: LocalDateTime?,

  @Schema(
    description = "The user id of the person who updated the alert. Required if updated at has been supplied.",
    example = "AB11DZ",
  )
  @field:Size(min = 1, max = 32, message = "Updated by must be <= 32 characters")
  @JsonAlias("updatedBy")
  override val lastModifiedBy: String?,

  @Schema(
    description = "The displayable name of the person who updated the alert. Required if updated at has been supplied.",
    example = "Up Dated",
  )
  @field:Size(min = 1, max = 255, message = "Updated by display name must be <= 255 characters")
  @JsonAlias("updatedByDisplayName")
  override val lastModifiedByDisplayName: String?,
) : Modifiable
