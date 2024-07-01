package uk.gov.justice.digital.hmpps.hmppsalertsapi.entity

import jakarta.persistence.CascadeType
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.NamedAttributeNode
import jakarta.persistence.NamedEntityGraph
import jakarta.persistence.NamedSubgraph
import jakarta.persistence.OneToMany
import jakarta.persistence.OneToOne
import jakarta.persistence.OrderBy
import jakarta.persistence.Table
import org.hibernate.annotations.SQLRestriction
import org.springframework.data.domain.AbstractAggregateRoot
import uk.gov.justice.digital.hmpps.hmppsalertsapi.common.aop.PersonAlertsChanged
import uk.gov.justice.digital.hmpps.hmppsalertsapi.entity.event.AlertCreatedEvent
import uk.gov.justice.digital.hmpps.hmppsalertsapi.entity.event.AlertDeletedEvent
import uk.gov.justice.digital.hmpps.hmppsalertsapi.entity.event.AlertUpdatedEvent
import uk.gov.justice.digital.hmpps.hmppsalertsapi.entity.event.AlertsMergedEvent
import uk.gov.justice.digital.hmpps.hmppsalertsapi.enumeration.AuditEventAction
import uk.gov.justice.digital.hmpps.hmppsalertsapi.enumeration.Source
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

@Entity
@Table
@SQLRestriction("deleted_at IS NULL")
@NamedEntityGraph(
  name = "alert",
  attributeNodes = [
    NamedAttributeNode("alertCode", subgraph = "alertType"),
    NamedAttributeNode("migratedAlert"),
  ],
  subgraphs = [NamedSubgraph(name = "alertType", attributeNodes = [NamedAttributeNode("alertType")])],
)
data class Alert(
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  val alertId: Long = 0,

  val alertUuid: UUID,

  @ManyToOne
  @JoinColumn(name = "alert_code_id", nullable = false)
  val alertCode: AlertCode,

  var prisonNumber: String,

  var description: String?,

  var authorisedBy: String?,

  var activeFrom: LocalDate,

  var activeTo: LocalDate?,

  val createdAt: LocalDateTime,
) : AbstractAggregateRoot<Alert>() {
  var lastModifiedAt: LocalDateTime? = null

  @OneToOne(mappedBy = "alert", cascade = [CascadeType.PERSIST, CascadeType.REMOVE])
  var migratedAlert: MigratedAlert? = null

  fun isActive() = activeTo == null || activeTo!! > LocalDate.now()

  @OneToMany(
    mappedBy = "alert",
    fetch = FetchType.EAGER,
    cascade = [CascadeType.PERSIST, CascadeType.MERGE, CascadeType.REMOVE],
  )
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

  @OneToMany(
    mappedBy = "alert",
    fetch = FetchType.EAGER,
    cascade = [CascadeType.PERSIST, CascadeType.MERGE, CascadeType.REMOVE],
  )
  @OrderBy("actioned_at DESC")
  private val auditEvents: MutableList<AuditEvent> = mutableListOf()

  fun auditEvents() = auditEvents.toList().sortedByDescending { it.actionedAt }

  fun auditEvent(
    action: AuditEventAction,
    description: String,
    actionedAt: LocalDateTime = LocalDateTime.now(),
    actionedBy: String,
    actionedByDisplayName: String,
    source: Source,
    activeCaseLoadId: String?,
    descriptionUpdated: Boolean? = null,
    authorisedByUpdated: Boolean? = null,
    activeFromUpdated: Boolean? = null,
    activeToUpdated: Boolean? = null,
    commentAppended: Boolean? = null,
  ): AuditEvent {
    val auditEvent = AuditEvent(
      alert = this,
      action = action,
      description = description,
      actionedAt = actionedAt,
      actionedBy = actionedBy,
      actionedByDisplayName = actionedByDisplayName,
      source = source,
      activeCaseLoadId = activeCaseLoadId,
      descriptionUpdated = descriptionUpdated,
      authorisedByUpdated = authorisedByUpdated,
      activeFromUpdated = activeFromUpdated,
      activeToUpdated = activeToUpdated,
      commentAppended = commentAppended,
    )
    auditEvents.add(auditEvent)
    return auditEvent
  }

  fun createdAuditEvent() = auditEvents().single { it.action == AuditEventAction.CREATED }

  fun lastModifiedAuditEvent() = auditEvents().firstOrNull { it.action == AuditEventAction.UPDATED }

  private var deletedAt: LocalDateTime? = null

  fun deletedAt() = deletedAt

  fun resync(
    createdBy: String,
    createdByDisplayName: String,
    lastModifiedBy: String?,
    lastModifiedByDisplayName: String?,
    original: Alert?,
  ) = apply {
    create(
      description = "Alert created via resync",
      createdBy = createdBy,
      createdByDisplayName = createdByDisplayName,
      source = Source.NOMIS,
      activeCaseLoadId = null,
      publishEvent = true,
      auditHistory = original?.auditEvents ?: emptyList(),
    )
    if (original.withoutAuditHistory && lastModifiedAt != null) {
      auditEvent(
        action = AuditEventAction.UPDATED,
        description = "Alert updated via resync",
        actionedAt = lastModifiedAt!!,
        actionedBy = lastModifiedBy!!,
        actionedByDisplayName = lastModifiedByDisplayName!!,
        source = Source.NOMIS,
        activeCaseLoadId = null,
      )
    }
  }

  fun create(
    description: String = "Alert created",
    createdAt: LocalDateTime = LocalDateTime.now(),
    createdBy: String,
    createdByDisplayName: String,
    source: Source,
    activeCaseLoadId: String?,
    publishEvent: Boolean = true,
    auditHistory: List<AuditEvent> = emptyList(),
  ) = apply {
    if (auditHistory.isEmpty()) {
      auditEvent(
        action = AuditEventAction.CREATED,
        description = description,
        actionedAt = createdAt,
        actionedBy = createdBy,
        actionedByDisplayName = createdByDisplayName,
        source = source,
        activeCaseLoadId = activeCaseLoadId,
      )
    } else {
      auditHistory.forEach {
        auditEvent(
          it.action,
          it.description,
          it.actionedAt,
          it.actionedBy,
          it.actionedByDisplayName,
          it.source,
          it.activeCaseLoadId,
          it.descriptionUpdated,
          it.authorisedByUpdated,
          it.activeFromUpdated,
          it.activeToUpdated,
          it.commentAppended,
        )
      }
    }
    if (publishEvent) {
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
      PersonAlertsChanged.registerChange(prisonNumber)
    }
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
    activeCaseLoadId: String?,
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
      addComment(
        comment = trimmedAppendComment!!,
        createdAt = updatedAt,
        createdBy = updatedBy,
        createdByDisplayName = updatedByDisplayName,
      )
      updated = true
    }

    if (updated) {
      lastModifiedAt = updatedAt
      auditEvent(
        action = AuditEventAction.UPDATED,
        description = sb.toString().trimEnd(),
        actionedAt = updatedAt,
        actionedBy = updatedBy,
        actionedByDisplayName = updatedByDisplayName,
        source = source,
        activeCaseLoadId = activeCaseLoadId,
        descriptionUpdated = descriptionUpdated,
        authorisedByUpdated = authorisedByUpdated,
        activeFromUpdated = activeFromUpdated,
        activeToUpdated = activeToUpdated,
        commentAppended = commentAppended,
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
      PersonAlertsChanged.registerChange(prisonNumber)
    }
  }

  fun delete(
    deletedAt: LocalDateTime = LocalDateTime.now(),
    deletedBy: String,
    deletedByDisplayName: String,
    source: Source,
    activeCaseLoadId: String?,
    publishEvent: Boolean = true,
    description: String = "Alert deleted",
  ): AuditEvent {
    lastModifiedAt = deletedAt
    this.deletedAt = deletedAt
    return auditEvent(
      action = AuditEventAction.DELETED,
      description = description,
      actionedAt = deletedAt,
      actionedBy = deletedBy,
      actionedByDisplayName = deletedByDisplayName,
      source = source,
      activeCaseLoadId = activeCaseLoadId,
    ).also {
      if (publishEvent) {
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
        PersonAlertsChanged.registerChange(prisonNumber)
      }
    }
  }

  fun reassign(prisonNumberMergeTo: String) = apply {
    prisonNumber = prisonNumberMergeTo
  }

  fun registerAlertsMergedEvent(event: AlertsMergedEvent) = apply { registerEvent(event) }

  /**
   * Function exists for testing purposes. The AbstractAggregateRoot.domainEvents() function is protected so this
   * function supports testing the correct domain events have been registered
   */
  internal fun publishedDomainEvents() = this.domainEvents()
}

val Alert?.withoutAuditHistory get() = this?.auditEvents()?.isEmpty() ?: true
