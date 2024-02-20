package uk.gov.justice.digital.hmpps.hmppsalertsapi.entity

import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.OneToMany
import jakarta.persistence.Table
import java.time.ZonedDateTime

@Entity
@Table
data class AlertType(
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  val alertTypeId: Long = 0,

  val code: String,
  var description: String,
  var listSequence: Int,
  val createdAt: ZonedDateTime,
  val createdBy: String,
) {
  var modifiedAt: ZonedDateTime? = null
  var modifiedBy: String? = null
  var deactivatedAt: ZonedDateTime? = null
  var deactivatedBy: String? = null

  @OneToMany(mappedBy = "alertType")
  private val alertCodes: MutableList<AlertCode> = mutableListOf()

  fun alertCodes(includeInactive: Boolean) = alertCodes.filter { includeInactive || it.isActive() }.toList()

  fun isActive() = deactivatedAt?.isBefore(ZonedDateTime.now()) != true
}
