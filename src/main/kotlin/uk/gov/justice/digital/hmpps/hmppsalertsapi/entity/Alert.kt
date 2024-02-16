package uk.gov.justice.digital.hmpps.hmppsalertsapi.entity

import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.LocalDate
import java.time.ZonedDateTime
import java.util.UUID

@Entity
@Table(name = "alerts")
data class Alert(
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  val alertId: Long = 0,

  val alertUuid: UUID = UUID.randomUUID(),
  val alertType: String,
  val alertCode: String,
  val offenderId: String,
  val authorisedBy: String,
  val validFrom: LocalDate,
) {
  var removedAt: ZonedDateTime? = null
  var validTo: ZonedDateTime? = null

  fun remove() {
    removedAt = ZonedDateTime.now()
  }

  fun expire(expiryDate: ZonedDateTime) {
    this.validTo = expiryDate
  }
}
