package uk.gov.justice.digital.hmpps.hmppsalertsapi.entity

import jakarta.persistence.CascadeType
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.OneToMany
import jakarta.persistence.OrderBy
import jakarta.persistence.Table
import org.hibernate.annotations.Fetch
import org.hibernate.annotations.FetchMode
import org.hibernate.annotations.SQLRestriction
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.LastModifiedDate
import org.springframework.data.domain.AbstractAggregateRoot
import uk.gov.justice.digital.hmpps.hmppsalertsapi.entity.event.AlertCreatedEvent
import uk.gov.justice.digital.hmpps.hmppsalertsapi.entity.event.AlertDeletedEvent
import uk.gov.justice.digital.hmpps.hmppsalertsapi.entity.event.AlertUpdatedEvent
import uk.gov.justice.digital.hmpps.hmppsalertsapi.enumeration.AuditEventAction
import uk.gov.justice.digital.hmpps.hmppsalertsapi.enumeration.Source
import java.lang.StringBuilder
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
  @JoinColumn(name = "alert_code_id", nullable = false)
  val alertCode: AlertCode,

  val prisonNumber: String,

  var description: String?,

  var authorisedBy: String?,

  var activeFrom: LocalDate,

  var activeTo: LocalDate?,

  @CreatedDate
  val createdAt: LocalDateTime,

  var migratedAt: LocalDateTime? = null,

  @LastModifiedDate
  var lastModifiedAt: LocalDateTime? = null,
) : AbstractAggregateRoot<Alert>() {
  fun isActive() = activeFrom <= LocalDate.now() && (activeTo == null || activeTo!! > LocalDate.now())

  fun willBecomeActive() = activeFrom > LocalDate.now()

  @OneToMany(mappedBy = "alert", fetch = FetchType.EAGER, cascade = [CascadeType.ALL], orphanRemoval = true)
  @Fetch(FetchMode.SUBSELECT)
  private val comments: MutableList<Comment> = mutableListOf()

  fun comments() = comments.toList().sortedByDescending { it.createdAt }

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

  @OneToMany(mappedBy = "alert", fetch = FetchType.EAGER, cascade = [CascadeType.ALL], orphanRemoval = true)
  @Fetch(FetchMode.SUBSELECT)
  @OrderBy("actioned_at DESC")
  private val auditEvents: MutableList<AuditEvent> = mutableListOf()

  fun auditEvents() = auditEvents.toList().sortedByDescending { it.actionedAt }

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

  fun createdAuditEvent() = auditEvents().single { it.action == AuditEventAction.CREATED }

  fun lastModifiedAuditEvent() = auditEvents().firstOrNull { it.action == AuditEventAction.UPDATED }

  private var deletedAt: LocalDateTime? = null

  fun deletedAt() = deletedAt

  fun create(
    createdAt: LocalDateTime = LocalDateTime.now(),
    createdBy: String,
    createdByDisplayName: String,
    source: Source,
  ) = apply {
    auditEvent(
      action = AuditEventAction.CREATED,
      description = "Alert created",
      actionedAt = createdAt,
      actionedBy = createdBy,
      actionedByDisplayName = createdByDisplayName,
    )
    registerEvent(
      AlertCreatedEvent(
        alertUuid = alertUuid,
        prisonNumber = prisonNumber,
        alertCode = alertCode.code,
        occurredAt = createdAt,
        source = source,
        createdBy = createdBy,
      ),
    )
  }

  fun update(
    description: String?,
    authorisedBy: String?,
    activeFrom: LocalDate?,
    activeTo: LocalDate?,
    appendComment: String?,
    updatedAt: LocalDateTime = LocalDateTime.now(),
    updatedBy: String,
    updatedByDisplayName: String,
    source: Source,
  ) = apply {
    val descriptionUpdated = description != null && this.description != description
    val authorisedByUpdated = authorisedBy != null && this.authorisedBy != authorisedBy
    val activeFromUpdated = activeFrom != null && this.activeFrom != activeFrom
    val activeToUpdated = this.activeTo != activeTo
    val trimmedAppendComment = appendComment?.trim()
    val commentAppended = !trimmedAppendComment.isNullOrEmpty()
    var updated = false

    val sb = StringBuilder()
    if (descriptionUpdated) {
      sb.appendLine("Updated alert description from '${this.description}' to '$description'")
      this.description = description
      updated = true
    }
    if (authorisedByUpdated) {
      sb.appendLine("Updated authorised by from '${this.authorisedBy}' to '$authorisedBy'")
      this.authorisedBy = authorisedBy
      updated = true
    }
    if (activeFromUpdated) {
      sb.appendLine("Updated active from from '${this.activeFrom}' to '$activeFrom'")
      this.activeFrom = activeFrom!!
      updated = true
    }
    if (activeToUpdated) {
      sb.appendLine("Updated active to from '${this.activeTo}' to '$activeTo'")
      this.activeTo = activeTo
      updated = true
    }
    if (commentAppended) {
      sb.appendLine("Comment '$trimmedAppendComment' was added")
      addComment(comment = trimmedAppendComment!!, createdAt = updatedAt, createdBy = updatedBy, createdByDisplayName = updatedByDisplayName)
      updated = true
    }

    if (updated) {
      auditEvent(
        action = AuditEventAction.UPDATED,
        description = sb.toString(),
        actionedAt = updatedAt,
        actionedBy = updatedBy,
        actionedByDisplayName = updatedByDisplayName,
      )
      registerEvent(
        AlertUpdatedEvent(
          alertUuid = alertUuid,
          prisonNumber = prisonNumber,
          alertCode = alertCode.code,
          occurredAt = updatedAt,
          source = source,
          updatedBy = updatedBy,
          descriptionUpdated = descriptionUpdated,
          authorisedByUpdated = authorisedByUpdated,
          activeFromUpdated = activeFromUpdated,
          activeToUpdated = activeToUpdated,
          commentAppended = commentAppended,
        ),
      )
    }
  }

  fun delete(
    deletedAt: LocalDateTime = LocalDateTime.now(),
    deletedBy: String,
    deletedByDisplayName: String,
    source: Source,
  ): AuditEvent {
    this.deletedAt = deletedAt
    return auditEvent(
      action = AuditEventAction.DELETED,
      description = "Alert deleted",
      actionedAt = deletedAt,
      actionedBy = deletedBy,
      actionedByDisplayName = deletedByDisplayName,
    ).also {
      registerEvent(
        AlertDeletedEvent(
          alertUuid = alertUuid,
          prisonNumber = prisonNumber,
          alertCode = alertCode.code,
          occurredAt = deletedAt,
          source = source,
          deletedBy = deletedBy,
        ),
      )
    }
  }

  /**
   * Function exists for testing purposes. The AbstractAggregateRoot.domainEvents() function is protected so this
   * function supports testing the correct domain events have been registered
   */
  internal fun publishedDomainEvents() = this.domainEvents()
}
