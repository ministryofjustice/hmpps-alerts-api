package uk.gov.justice.digital.hmpps.hmppsalertsapi.entity

import com.fasterxml.jackson.databind.JsonNode
import io.hypersistence.utils.hibernate.type.json.JsonType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import jakarta.persistence.ManyToOne
import jakarta.persistence.OneToOne
import jakarta.persistence.JoinColumn
import jakarta.persistence.CascadeType
import jakarta.persistence.FetchType
import org.hibernate.annotations.Fetch
import org.hibernate.annotations.FetchMode
import org.hibernate.annotations.Type
import java.time.LocalDateTime
import java.util.UUID

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
