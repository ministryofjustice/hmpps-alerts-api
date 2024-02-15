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
@Table(name = "comments")
data class Comment(
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Id
  val commentId: Long,
  val comment: String,
  @ManyToOne
  @JoinColumn(name = "alert_id")
  val alert: Alert,
  val createdAt: ZonedDateTime,
  val createdBy: String,
)
