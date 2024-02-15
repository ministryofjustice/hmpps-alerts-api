package uk.gov.justice.digital.hmpps.hmppsalertsapi.entity

import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import java.time.ZonedDateTime

@Entity
@Table(name = "verifications")
data class Verification(
  @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
  val verificationId: Long,
  @ManyToOne
  @JoinColumn(name = "alert_id")
  val alert: Alert,
  val action: String,
  val verifiedBy: String,
  val verifiedAt: ZonedDateTime,
)
