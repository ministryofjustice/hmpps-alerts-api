package uk.gov.justice.digital.hmpps.hmppsalertsapi.entity

import jakarta.persistence.CascadeType
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.ManyToOne
import jakarta.persistence.OneToMany
import jakarta.persistence.OrderBy
import jakarta.persistence.Table
import org.hibernate.annotations.SQLRestriction
import uk.gov.justice.digital.hmpps.hmppsalertsapi.enumeration.AuditEventAction
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

@Entity
@Table
@SQLRestriction("deleted_at IS NULL")
data class Alert(
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  val alertId: Long = 0,

  val alertUuid: UUID,

  @ManyToOne(fetch = FetchType.EAGER)
  val alertCode: AlertCode,

  val prisonNumber: String,

  val description: String,

  val authorisedBy: String?,

  val activeFrom: LocalDate,

  val activeTo: LocalDate?,
) {
  fun isActive() = activeFrom.isBefore(LocalDate.now()) && activeTo?.isBefore(LocalDate.now()) != true

  @OneToMany(mappedBy = "alert", cascade = [CascadeType.ALL], orphanRemoval = true)
  private val comments: MutableList<Comment> = mutableListOf()

  fun comments() = comments.toList()

  fun addComment(
    comment: String,
    createdAt: LocalDateTime = LocalDateTime.now(),
    createdBy: String,
    createdByDisplayName: String,
  ): Comment {
    val commentEntity = Comment(
      commentUuid = UUID.randomUUID(),
      alert = this,
      comment = comment,
      createdAt = createdAt,
      createdBy = createdBy,
      createdByDisplayName = createdByDisplayName,
    )
    comments.add(commentEntity)
    return commentEntity
  }

  @OneToMany(mappedBy = "alert", cascade = [CascadeType.ALL], orphanRemoval = true)
  @OrderBy("actioned_at DESC")
  private val auditEvents: MutableList<AuditEvent> = mutableListOf()

  fun auditEvents() = auditEvents.toList()

  fun auditEvent(
    action: AuditEventAction,
    description: String,
    actionedAt: LocalDateTime = LocalDateTime.now(),
    actionedBy: String,
    actionedByDisplayName: String,
  ): AuditEvent {
    val auditEvent = AuditEvent(
      alert = this,
      action = action,
      description = description,
      actionedAt = actionedAt,
      actionedBy = actionedBy,
      actionedByDisplayName = actionedByDisplayName,
    )
    auditEvents.add(auditEvent)
    return auditEvent
  }

  fun createdAuditEvent() = auditEvents.first { it.action == AuditEventAction.CREATED }

  fun lastModifiedAuditEvent() = auditEvents.firstOrNull { it.action == AuditEventAction.UPDATED }

  private var deletedAt: LocalDateTime? = null

  fun delete(
    deletedAt: LocalDateTime = LocalDateTime.now(),
    deletedBy: String,
    deletedByDisplayName: String,
  ): AuditEvent {
    this.deletedAt = deletedAt
    return auditEvent(
      action = AuditEventAction.DELETED,
      description = "Alert deleted",
      actionedAt = deletedAt,
      actionedBy = deletedBy,
      actionedByDisplayName = deletedByDisplayName,
    )
  }
}
