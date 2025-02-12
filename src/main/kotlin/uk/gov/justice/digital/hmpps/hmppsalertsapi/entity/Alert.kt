package uk.gov.justice.digital.hmpps.hmppsalertsapi.entity

import jakarta.persistence.CascadeType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.NamedAttributeNode
import jakarta.persistence.NamedEntityGraph
import jakarta.persistence.NamedSubgraph
import jakarta.persistence.OneToMany
import jakarta.persistence.OrderBy
import jakarta.persistence.Table
import jakarta.persistence.Version
import org.hibernate.annotations.SQLRestriction
import org.springframework.data.domain.AbstractAggregateRoot
import uk.gov.justice.digital.hmpps.hmppsalertsapi.IdGenerator.newUuid
import uk.gov.justice.digital.hmpps.hmppsalertsapi.common.aop.PersonAlertsChanged
import uk.gov.justice.digital.hmpps.hmppsalertsapi.entity.event.AlertCreatedEvent
import uk.gov.justice.digital.hmpps.hmppsalertsapi.entity.event.AlertDeactivatedEvent
import uk.gov.justice.digital.hmpps.hmppsalertsapi.entity.event.AlertDeletedEvent
import uk.gov.justice.digital.hmpps.hmppsalertsapi.entity.event.AlertUpdatedEvent
import uk.gov.justice.digital.hmpps.hmppsalertsapi.enumeration.AuditEventAction
import uk.gov.justice.digital.hmpps.hmppsalertsapi.enumeration.Source
import java.time.LocalDate
import java.time.LocalDate.now
import java.time.LocalDateTime
import java.util.UUID

@Entity
@Table
@SQLRestriction("deleted_at IS NULL")
@NamedEntityGraph(
  name = "alert",
  attributeNodes = [NamedAttributeNode("alertCode", subgraph = "alertType")],
  subgraphs = [NamedSubgraph(name = "alertType", attributeNodes = [NamedAttributeNode("alertType")])],
)
class Alert(
  @ManyToOne
  @JoinColumn(name = "alert_code_id", nullable = false)
  val alertCode: AlertCode,

  var prisonNumber: String,

  var description: String?,

  var authorisedBy: String?,

  var activeFrom: LocalDate,

  var activeTo: LocalDate?,

  val createdAt: LocalDateTime,

  @Column(length = 6)
  val prisonCodeWhenCreated: String?,

  @Id
  val id: UUID = newUuid(),
) : AbstractAggregateRoot<Alert>() {
  @Version
  val version: Int? = null

  var lastModifiedAt: LocalDateTime? = null

  fun isActive() = activeTo == null || activeTo!! > now()

  fun isDeactivated(): Boolean {
    if (isActive()) return false
    return deactivationEvent() != null
  }

  fun deactivationEvent() = auditEvents().firstOrNull {
    it.action == AuditEventAction.INACTIVE ||
      (it.activeToUpdated == true && it.actionedAt.toLocalDate() == activeTo)
  }

  @OneToMany(mappedBy = "alert", fetch = FetchType.EAGER, cascade = [CascadeType.PERSIST, CascadeType.MERGE])
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
    )
    auditEvents.add(auditEvent)
    return auditEvent
  }

  fun createdAuditEvent() = auditEvents().single { it.action == AuditEventAction.CREATED }

  fun lastModifiedAuditEvent() = auditEvents().firstOrNull { it.action == AuditEventAction.UPDATED }

  var deletedAt: LocalDateTime? = null
    private set

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
        )
      }
    }
    if (publishEvent) {
      registerEvent(
        AlertCreatedEvent(
          alertUuid = id,
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
      if (isActive() && activeTo?.isAfter(now()) == false) {
        deactivate(updatedAt, updatedBy, updatedByDisplayName, source, activeCaseLoadId)
      }
      this.activeTo = activeTo
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
      )
      registerEvent(
        AlertUpdatedEvent(
          alertUuid = id,
          prisonNumber = prisonNumber,
          alertCode = alertCode.code,
          occurredAt = updatedAt,
          source = source,
          updatedBy = updatedBy,
          descriptionUpdated = descriptionUpdated,
          authorisedByUpdated = authorisedByUpdated,
          activeFromUpdated = activeFromUpdated,
          activeToUpdated = activeToUpdated,
        ),
      )
      PersonAlertsChanged.registerChange(prisonNumber)
    }
  }

  fun deactivate(
    updatedAt: LocalDateTime = LocalDateTime.now(),
    updatedBy: String,
    updatedByDisplayName: String,
    source: Source,
    activeCaseLoadId: String?,
  ): Alert = apply {
    auditEvent(
      action = AuditEventAction.INACTIVE,
      description = "Alert became inactive",
      actionedAt = updatedAt,
      actionedBy = updatedBy,
      actionedByDisplayName = updatedByDisplayName,
      source = source,
      activeCaseLoadId = activeCaseLoadId,
      activeToUpdated = true,
    )
    registerEvent(
      AlertDeactivatedEvent(
        alertUuid = id,
        prisonNumber = prisonNumber,
        alertCode = alertCode.code,
        occurredAt = updatedAt,
        source = source,
      ),
    )
    PersonAlertsChanged.registerChange(prisonNumber)
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
            alertUuid = id,
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

  /**
   * Function exists for testing purposes. The AbstractAggregateRoot.domainEvents() function is protected so this
   * function supports testing the correct domain events have been registered
   */
  internal fun publishedDomainEvents() = this.domainEvents()
}

val Alert?.withoutAuditHistory get() = this?.auditEvents()?.isEmpty() ?: true
