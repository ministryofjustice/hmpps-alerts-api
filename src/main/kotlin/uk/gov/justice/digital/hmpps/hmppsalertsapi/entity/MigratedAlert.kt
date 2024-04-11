package uk.gov.justice.digital.hmpps.hmppsalertsapi.entity

import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.OneToOne
import jakarta.persistence.Table
import java.time.LocalDateTime

@Entity
@Table
data class MigratedAlert(
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  val migratedAlertId: Long = 0,

  val offenderBookId: Long,

  val bookingSeq: Int,

  val alertSeq: Int,

  @OneToOne
  @JoinColumn(name = "alert_id", nullable = false)
  val alert: Alert,

  var migratedAt: LocalDateTime,
)
