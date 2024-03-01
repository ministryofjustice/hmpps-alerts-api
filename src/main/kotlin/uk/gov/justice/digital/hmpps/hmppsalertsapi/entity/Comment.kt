package uk.gov.justice.digital.hmpps.hmppsalertsapi.entity

import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import java.time.LocalDateTime
import java.util.UUID

@Entity
@Table
data class Comment(
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Id
  val commentId: Long = 0,

  val commentUuid: UUID,

  @ManyToOne
  @JoinColumn(name = "alert_id", nullable = false)
  val alert: Alert,

  val comment: String,

  val createdAt: LocalDateTime,
  val createdBy: String,
  val createdByDisplayName: String,
)
