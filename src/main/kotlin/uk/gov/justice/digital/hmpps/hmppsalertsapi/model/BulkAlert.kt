package uk.gov.justice.digital.hmpps.hmppsalertsapi.model

import com.fasterxml.jackson.annotation.JsonFormat
import io.swagger.v3.oas.annotations.media.Schema
import uk.gov.justice.digital.hmpps.hmppsalertsapi.model.request.BulkCreateAlerts
import java.time.LocalDateTime
import java.util.UUID

@Schema(
  description = "A set of alerts created in bulk. Contains detailed information of the result of a bulk alert creation request.",
)
data class BulkAlert(
  @Schema(
    description = "The unique identifier assigned to the alerts created in bulk",
    example = "b49053d8-3f29-4b1e-a9c5-15bde8c6e6cf",
  )
  val bulkAlertUuid: UUID,

  @Schema(
    description = "The original request body used to create alerts for multiple people in bulk",
  )
  val request: BulkCreateAlerts,

  @Schema(
    description = "The date and time the alerts were created in bulk",
    example = "2021-09-27T14:19:25",
  )
  @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
  val requestedAt: LocalDateTime,

  @Schema(
    description = "The username of the user who created the alerts in bulk",
    example = "USER1234",
  )
  val requestedBy: String,

  @Schema(
    description = "The displayable name of the user who created the alerts in bulk",
    example = "Firstname Lastname",
  )
  val requestedByDisplayName: String,

  @Schema(
    description = "The date and time the request to create alerts in bulk was completed",
    example = "2021-09-27T14:19:25",
  )
  @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
  val completedAt: LocalDateTime?,

  @Schema(
    description = "Whether the request to create alerts in bulk was successful or not",
    example = "true",
  )
  val successful: Boolean?,

  @Schema(
    description = "Collection of displayable messages relating to the result of the bulk alert creation request as a whole",
  )
  val messages: Collection<String>,

  @Schema(
    description = "Collection of existing active alerts that were not modified and resulted in no additional alert " +
      "being created for the associated prison number.",
  )
  val existingActiveAlerts: Collection<BulkAlertAlert>,

  @Schema(
    description = "Collection of new alerts that were created in bulk",
  )
  val alertsCreated: Collection<BulkAlertAlert>,

  @Schema(
    description = "Collection of existing alerts that were updated as a result of the bulk alert creation request. " +
      "The message for updated alerts will contain what was updated for example changing the active from date.",
  )
  val alertsUpdated: Collection<BulkAlertAlert>,

  @Schema(
    description = "Collection of existing alerts that were made inactive as a result of the bulk alert creation request",
  )
  val alertsExpired: Collection<BulkAlertAlert>,
)

@Schema(
  description = "Summary information of an alert affected by a bulk alert creation request",
)
data class BulkAlertAlert(
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
    description = "Optional displayable message relating to the result of the bulk alert creation request specific to this alert. " +
      "For example the description of the updates that were applied to this alert.",
  )
  val message: String,
)

@Schema(
  description = "The plan of a bulk alert request. Contains detailed information of the result if the bulk alert creation request is actually executed.",
)
data class BulkAlertPlan(
  @Schema(
    description = "The original request body used to create alerts for multiple people in bulk",
  )
  val request: BulkCreateAlerts,

  @Schema(
    description = "Collection of prisoner numbers of the existing active alerts that will not be modified, " +
      "nor have additional alert created for the same prison number",
  )
  val existingActiveAlertsPrisonNumbers: Collection<String>,

  @Schema(
    description = "Collection of prisoner numbers that will have new alerts created",
  )
  val alertsToBeCreatedForPrisonNumbers: Collection<String>,

  @Schema(
    description = "Collection of prison numbers of the existing alerts that will be updated as a result of the bulk alert creation request.",
  )
  val alertsToBeUpdatedForPrisonNumbers: Collection<String>,

  @Schema(
    description = "Collection of prison numbers of the existing alerts that will be made inactive as a result of the bulk alert creation request",
  )
  val alertsToBeExpiredForPrisonNumbers: Collection<String>,
)
