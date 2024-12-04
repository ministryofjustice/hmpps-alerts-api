package uk.gov.justice.digital.hmpps.hmppsalertsapi.model.request

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import jakarta.validation.constraints.NotEmpty
import jakarta.validation.constraints.Size
import uk.gov.justice.digital.hmpps.hmppsalertsapi.enumeration.BulkAlertCleanupMode

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
@JsonSubTypes(
  value = [
    JsonSubTypes.Type(value = SetAlertCode::class, name = "SetAlertCode"),
    JsonSubTypes.Type(value = SetDescription::class, name = "SetDescription"),
    JsonSubTypes.Type(value = AddPrisonNumbers::class, name = "AddPrisonNumbers"),
    JsonSubTypes.Type(value = RemovePrisonNumbers::class, name = "RemovePrisonNumbers"),
    JsonSubTypes.Type(value = SetCleanupMode::class, name = "SetCleanupMode"),
  ],
)
sealed interface BulkAction {
  val type: String
}

data class SetAlertCode(
  @field:Size(min = 1, max = 12, message = "Alert code must be supplied and be <= 12 characters")
  val alertCode: String,
) : BulkAction {
  override val type: String = this::class.simpleName!!
}

data class SetDescription(
  @field:Size(max = 255, message = "Description must be <= 255 characters")
  val description: String?,
) : BulkAction {
  override val type: String = this::class.simpleName!!
}

data class SetCleanupMode(
  val cleanupMode: BulkAlertCleanupMode,
) : BulkAction {
  override val type: String = this::class.simpleName!!
}

data class AddPrisonNumbers(
  @field:NotEmpty(message = "At least one prison number should be provided")
  val prisonNumbers: LinkedHashSet<String>,
) : BulkAction {
  override val type: String = this::class.simpleName!!
}

data class RemovePrisonNumbers(
  @field:NotEmpty(message = "At least one prison number should be provided")
  val prisonNumbers: LinkedHashSet<String>,
) : BulkAction {
  override val type: String = this::class.simpleName!!
}
