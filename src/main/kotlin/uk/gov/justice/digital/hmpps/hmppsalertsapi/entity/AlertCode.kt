package uk.gov.justice.digital.hmpps.hmppsalertsapi.entity

import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import org.springframework.data.domain.AbstractAggregateRoot
import uk.gov.justice.digital.hmpps.hmppsalertsapi.entity.event.AlertCodeCreatedEvent
import uk.gov.justice.digital.hmpps.hmppsalertsapi.entity.event.AlertCodeDeactivatedEvent
import uk.gov.justice.digital.hmpps.hmppsalertsapi.entity.event.AlertCodeReactivatedEvent
import uk.gov.justice.digital.hmpps.hmppsalertsapi.entity.event.AlertCodeUpdatedEvent
import java.time.LocalDateTime

@Entity
@Table
class AlertCode(

  @ManyToOne
  @JoinColumn(name = "alert_type_id", nullable = false)
  val alertType: AlertType,

  val code: String,
  var description: String,
  var listSequence: Int,
  val createdAt: LocalDateTime,
  val createdBy: String,
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  val alertCodeId: Long = 0,
) : AbstractAggregateRoot<AlertCode>() {
  var modifiedAt: LocalDateTime? = null
  var modifiedBy: String? = null
  var deactivatedAt: LocalDateTime? = null
  var deactivatedBy: String? = null

  fun isActive() = deactivatedAt?.isBefore(LocalDateTime.now()) != true

  fun create(): AlertCode = this.also {
    registerEvent(
      AlertCodeCreatedEvent(code, createdAt),
    )
  }

  fun deactivate(): AlertCode = this.also {
    registerEvent(
      AlertCodeDeactivatedEvent(code, deactivatedAt!!),
    )
  }

  fun reactivate(requestAt: LocalDateTime): AlertCode = this.also {
    registerEvent(
      AlertCodeReactivatedEvent(code, requestAt),
    )
  }

  fun update(): AlertCode = this.also {
    registerEvent(
      AlertCodeUpdatedEvent(code, modifiedAt!!),
    )
  }
}
