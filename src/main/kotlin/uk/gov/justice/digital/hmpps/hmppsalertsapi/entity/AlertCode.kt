package uk.gov.justice.digital.hmpps.hmppsalertsapi.entity

import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import java.time.LocalDateTime

@Entity
@Table
data class AlertCode(
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  val alertCodeId: Long = 0,

  @ManyToOne
  @JoinColumn(name = "alert_type_id", nullable = false)
  val alertType: AlertType,

  val code: String,
  var description: String,
  var listSequence: Int,
  val createdAt: LocalDateTime,
  val createdBy: String,
) {
  var modifiedAt: LocalDateTime? = null
  var modifiedBy: String? = null
  var deactivatedAt: LocalDateTime? = null
  var deactivatedBy: String? = null

  fun isActive() = deactivatedAt?.isBefore(LocalDateTime.now()) != true
}
