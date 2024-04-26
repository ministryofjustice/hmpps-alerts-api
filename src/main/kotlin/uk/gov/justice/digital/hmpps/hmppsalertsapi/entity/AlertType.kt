package uk.gov.justice.digital.hmpps.hmppsalertsapi.entity

import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.OneToMany
import jakarta.persistence.Table
import org.springframework.data.domain.AbstractAggregateRoot
import uk.gov.justice.digital.hmpps.hmppsalertsapi.entity.event.AlertTypeCreatedEvent
import uk.gov.justice.digital.hmpps.hmppsalertsapi.entity.event.AlertTypeDeactivatedEvent
import uk.gov.justice.digital.hmpps.hmppsalertsapi.entity.event.AlertTypeUpdatedEvent
import java.time.LocalDateTime

@Entity
@Table
data class AlertType(
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  val alertTypeId: Long = 0,

  val code: String,
  var description: String,
  var listSequence: Int,
  val createdAt: LocalDateTime,
  val createdBy: String,
) : AbstractAggregateRoot<AlertType>() {
  var modifiedAt: LocalDateTime? = null
  var modifiedBy: String? = null
  var deactivatedAt: LocalDateTime? = null
  var deactivatedBy: String? = null

  @OneToMany(mappedBy = "alertType")
  private val alertCodes: MutableList<AlertCode> = mutableListOf()

  fun addAlertCode(alertCode: AlertCode): AlertCode {
    alertCodes.add(alertCode)
    return alertCode
  }

  fun alertCodes(includeInactive: Boolean) = alertCodes.filter { includeInactive || it.isActive() }.toList()

  fun isActive() = deactivatedAt?.isBefore(LocalDateTime.now()) != true

  fun create(): AlertType = this.also {
    registerEvent(
      AlertTypeCreatedEvent(code, createdAt),
    )
  }

  fun deactivate(): AlertType = this.also {
    registerEvent(
      AlertTypeDeactivatedEvent(code, deactivatedAt!!),
    )
  }

  fun update(): AlertType = this.also {
    registerEvent(
      AlertTypeUpdatedEvent(code, modifiedAt!!),
    )
  }
}
