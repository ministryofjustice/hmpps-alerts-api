package uk.gov.justice.digital.hmpps.hmppsalertsapi.entity

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.hmppsalertsapi.entity.event.AlertCreatedEvent
import uk.gov.justice.digital.hmpps.hmppsalertsapi.entity.event.AlertDeletedEvent
import uk.gov.justice.digital.hmpps.hmppsalertsapi.entity.event.AlertUpdatedEvent
import uk.gov.justice.digital.hmpps.hmppsalertsapi.enumeration.AuditEventAction.CREATED
import uk.gov.justice.digital.hmpps.hmppsalertsapi.enumeration.AuditEventAction.DELETED
import uk.gov.justice.digital.hmpps.hmppsalertsapi.enumeration.AuditEventAction.UPDATED
import uk.gov.justice.digital.hmpps.hmppsalertsapi.enumeration.Source.ALERTS_SERVICE
import uk.gov.justice.digital.hmpps.hmppsalertsapi.enumeration.Source.NOMIS
import uk.gov.justice.digital.hmpps.hmppsalertsapi.integration.wiremock.PRISON_NUMBER
import uk.gov.justice.digital.hmpps.hmppsalertsapi.utils.alertCodeVictim
import java.lang.StringBuilder
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

class AlertTest {
  @Test
  fun `is active when active from is today`() {
    val alert = alertEntity().apply { activeFrom = LocalDate.now() }
    assertThat(alert.isActive()).isTrue
  }

  @Test
  fun `is not active when active from is tomorrow`() {
    val alert = alertEntity().apply { activeFrom = LocalDate.now().plusDays(1) }
    assertThat(alert.isActive()).isFalse
  }

  @Test
  fun `is active when active to is null`() {
    val alert = alertEntity().apply { activeTo = null }
    assertThat(alert.isActive()).isTrue
  }

  @Test
  fun `is active when active to is tomorrow`() {
    val alert = alertEntity().apply { activeTo = LocalDate.now().plusDays(1) }
    assertThat(alert.isActive()).isTrue
  }

  @Test
  fun `is not active when active to is today`() {
    val alert = alertEntity().apply { activeTo = LocalDate.now() }
    assertThat(alert.isActive()).isFalse
  }

  @Test
  fun `will become active when active from is tomorrow`() {
    val alert = alertEntity().apply { activeFrom = LocalDate.now().plusDays(1) }
    assertThat(alert.willBecomeActive()).isTrue
  }

  @Test
  fun `add comment`() {
    val createdAt = LocalDateTime.now().minusDays(3)
    var comment: Comment
    val entity = alertEntity().apply {
      comment = addComment("Comment", createdAt, "COMMENT_BY", "COMMENT_BY_DISPLAY_NAME")
    }

    assertThat(entity.comments().single()).isEqualTo(
      Comment(
        commentUuid = comment.commentUuid,
        alert = entity,
        comment = "Comment",
        createdAt = createdAt,
        createdBy = "COMMENT_BY",
        createdByDisplayName = "COMMENT_BY_DISPLAY_NAME",
      ),
    )
  }

  @Test
  fun `comments are ordered newest to oldest`() {
    val entity = alertEntity().apply {
      addComment("Comment 2", LocalDateTime.now().minusDays(2), "COMMENT_BY_2", "COMMENT_BY_DISPLAY_NAME_2")
      addComment("Comment 3", LocalDateTime.now().minusDays(1), "COMMENT_BY_3", "COMMENT_BY_DISPLAY_NAME_3")
      addComment("Comment 1", LocalDateTime.now().minusDays(3), "COMMENT_BY_1", "COMMENT_BY_DISPLAY_NAME_1")
    }

    assertThat(entity.comments()).isSortedAccordingTo(compareByDescending { it.createdBy })
  }

  @Test
  fun `add audit event`() {
    val actionedAt = LocalDateTime.now().minusDays(2)
    val entity = alertEntity().apply {
      auditEvent(
        action = UPDATED,
        description = "Alert updated",
        actionedAt = actionedAt,
        actionedBy = "UPDATED_BY",
        actionedByDisplayName = "UPDATED_BY_DISPLAY_NAME",
      )
    }

    assertThat(entity.auditEvents().single { it.action == UPDATED }).isEqualTo(
      AuditEvent(
        alert = entity,
        action = UPDATED,
        description = "Alert updated",
        actionedAt = actionedAt,
        actionedBy = "UPDATED_BY",
        actionedByDisplayName = "UPDATED_BY_DISPLAY_NAME",
      ),
    )
  }

  @Test
  fun `audit events are ordered newest to oldest`() {
    val entity = alertEntity().apply {
      auditEvent(
        action = UPDATED,
        description = "Alert updated",
        actionedAt = LocalDateTime.now().minusDays(2),
        actionedBy = "UPDATED_BY_2",
        actionedByDisplayName = "UPDATED_BY_2_DISPLAY_NAME",
      )
      auditEvent(
        action = DELETED,
        description = "Alert deleted",
        actionedAt = LocalDateTime.now(),
        actionedBy = "DELETED_BY",
        actionedByDisplayName = "DELETED_BY_DISPLAY_NAME",
      )
      auditEvent(
        action = UPDATED,
        description = "Alert updated",
        actionedAt = LocalDateTime.now().minusDays(1),
        actionedBy = "UPDATED_BY_3",
        actionedByDisplayName = "UPDATED_BY_3_DISPLAY_NAME",
      )
      auditEvent(
        action = UPDATED,
        description = "Alert updated",
        actionedAt = LocalDateTime.now().minusDays(3),
        actionedBy = "UPDATED_BY_1",
        actionedByDisplayName = "UPDATED_BY_1_DISPLAY_NAME",
      )
    }

    assertThat(entity.auditEvents()).isSortedAccordingTo(compareByDescending { it.actionedAt })
  }

  @Test
  fun `created audit event returns single created audit event`() {
    val createdAt: LocalDateTime = LocalDateTime.now().minusDays(3)
    val entity = alertEntity(createdAt).apply {
      auditEvent(
        action = UPDATED,
        description = "Alert updated",
        actionedAt = LocalDateTime.now().minusDays(2),
        actionedBy = "UPDATED_BY",
        actionedByDisplayName = "UPDATED_BY_DISPLAY_NAME",
      )
      auditEvent(
        action = DELETED,
        description = "Alert deleted",
        actionedAt = LocalDateTime.now(),
        actionedBy = "DELETED_BY",
        actionedByDisplayName = "DELETED_BY_DISPLAY_NAME",
      )
    }

    assertThat(entity.createdAuditEvent()).isEqualTo(
      AuditEvent(
        alert = entity,
        action = CREATED,
        description = "Alert created",
        actionedAt = createdAt,
        actionedBy = "CREATED_BY",
        actionedByDisplayName = "CREATED_BY_DISPLAY_NAME",
      ),
    )
  }

  @Test
  fun `last modified audit event is newest updated audit event`() {
    val lastModifiedAt = LocalDateTime.now().minusDays(1)
    val entity = alertEntity().apply {
      auditEvent(
        action = UPDATED,
        description = "Alert updated",
        actionedAt = LocalDateTime.now().minusDays(2),
        actionedBy = "UPDATED_BY_2",
        actionedByDisplayName = "UPDATED_BY_2_DISPLAY_NAME",
      )
      auditEvent(
        action = DELETED,
        description = "Alert deleted",
        actionedAt = LocalDateTime.now(),
        actionedBy = "DELETED_BY",
        actionedByDisplayName = "DELETED_BY_DISPLAY_NAME",
      )
      auditEvent(
        action = UPDATED,
        description = "Alert updated",
        actionedAt = lastModifiedAt,
        actionedBy = "UPDATED_BY_3",
        actionedByDisplayName = "UPDATED_BY_3_DISPLAY_NAME",
      )
      auditEvent(
        action = UPDATED,
        description = "Alert updated",
        actionedAt = LocalDateTime.now().minusDays(3),
        actionedBy = "UPDATED_BY_1",
        actionedByDisplayName = "UPDATED_BY_1_DISPLAY_NAME",
      )
    }

    assertThat(entity.lastModifiedAuditEvent()).isEqualTo(
      AuditEvent(
        alert = entity,
        action = UPDATED,
        description = "Alert updated",
        actionedAt = lastModifiedAt,
        actionedBy = "UPDATED_BY_3",
        actionedByDisplayName = "UPDATED_BY_3_DISPLAY_NAME",
      ),
    )
  }

  @Test
  fun `create audits event`() {
    val createdAt = LocalDateTime.now()
    val createdBy = "CREATED_BY"
    val createdByDisplayName = "CREATED_BY_DISPLAY_NAME"

    val entity = Alert(
      alertUuid = UUID.randomUUID(),
      alertCode = alertCodeVictim(),
      prisonNumber = PRISON_NUMBER,
      description = "Alert description",
      authorisedBy = "A. Authorizer",
      activeFrom = LocalDate.now().minusDays(3),
      activeTo = LocalDate.now().plusDays(3),
      createdAt = createdAt,
    ).create(createdAt, createdBy, createdByDisplayName, ALERTS_SERVICE)

    assertThat(entity.auditEvents().single()).isEqualTo(
      AuditEvent(
        alert = entity,
        action = CREATED,
        description = "Alert created",
        actionedAt = createdAt,
        actionedBy = createdBy,
        actionedByDisplayName = createdByDisplayName,
      ),
    )
  }

  @Test
  fun `create raises domain event`() {
    val createdAt = LocalDateTime.now()
    val createdBy = "CREATED_BY"
    val createdByDisplayName = "CREATED_BY_DISPLAY_NAME"
    val source = ALERTS_SERVICE

    val entity = Alert(
      alertUuid = UUID.randomUUID(),
      alertCode = alertCodeVictim(),
      prisonNumber = PRISON_NUMBER,
      description = "Alert description",
      authorisedBy = "A. Authorizer",
      activeFrom = LocalDate.now().minusDays(3),
      activeTo = LocalDate.now().plusDays(3),
      createdAt = createdAt,
    ).create(createdAt, createdBy, createdByDisplayName, source)

    assertThat(entity.publishedDomainEvents().single()).isEqualTo(
      AlertCreatedEvent(
        alertUuid = entity.alertUuid,
        prisonNumber = entity.prisonNumber,
        alertCode = entity.alertCode.code,
        occurredAt = createdAt,
        source = source,
        createdBy = createdBy,
      ),
    )
  }

  @Test
  fun `update all properties audits event`() {
    val entity = alertEntity()
    val updatedDescription = "Updated description"
    val updatedAuthorisedBy = "Updated authorised by"
    val updatedActiveFrom = entity.activeFrom.plusDays(1)
    val updatedActiveTo = entity.activeTo!!.plusDays(1)
    val updatedAppendComment = "Appended comment"
    val updatedAt = LocalDateTime.now()
    val updatedBy = "UPDATED_BY"
    val updatedByDisplayName = "UPDATED_BY_DISPLAY_NAME"

    val sb = StringBuilder()
    sb.appendLine("Updated alert description from '${entity.description}' to '$updatedDescription'")
    sb.appendLine("Updated authorised by from '${entity.authorisedBy}' to '$updatedAuthorisedBy'")
    sb.appendLine("Updated active from from '${entity.activeFrom}' to '$updatedActiveFrom'")
    sb.appendLine("Updated active to from '${entity.activeTo}' to '$updatedActiveTo'")
    sb.appendLine("Comment '$updatedAppendComment' was added")
    val expectedDescription = sb.toString()

    entity.update(
      description = updatedDescription,
      authorisedBy = updatedAuthorisedBy,
      activeFrom = updatedActiveFrom,
      activeTo = updatedActiveTo,
      appendComment = updatedAppendComment,
      updatedAt = updatedAt,
      updatedBy = updatedBy,
      updatedByDisplayName = updatedByDisplayName,
      source = ALERTS_SERVICE,
    )

    assertThat(entity.auditEvents().single { it.action == UPDATED }).isEqualTo(
      AuditEvent(
        alert = entity,
        action = UPDATED,
        description = expectedDescription,
        actionedAt = updatedAt,
        actionedBy = updatedBy,
        actionedByDisplayName = updatedByDisplayName,
      ),
    )
  }

  @Test
  fun `update all properties raises domain event`() {
    val entity = alertEntity()
    val updatedAt = LocalDateTime.now()
    val updatedBy = "UPDATED_BY"
    val updatedByDisplayName = "UPDATED_BY_DISPLAY_NAME"
    val source = NOMIS

    entity.update(
      description = "Updated description",
      authorisedBy = "Updated authorised by",
      activeFrom = entity.activeFrom.plusDays(1),
      activeTo = entity.activeTo!!.plusDays(1),
      appendComment = "Appended comment",
      updatedAt = updatedAt,
      updatedBy = updatedBy,
      updatedByDisplayName = updatedByDisplayName,
      source = source,
    )

    assertThat(entity.publishedDomainEvents().single()).isEqualTo(
      AlertUpdatedEvent(
        alertUuid = entity.alertUuid,
        prisonNumber = entity.prisonNumber,
        alertCode = entity.alertCode.code,
        occurredAt = updatedAt,
        source = source,
        updatedBy = updatedBy,
        descriptionUpdated = true,
        authorisedByUpdated = true,
        activeFromUpdated = true,
        activeToUpdated = true,
        commentAppended = true,
      ),
    )
  }

  @Test
  fun `update alert ignores no updates`() {
    val entity = alertEntity()

    entity.update(
      description = entity.description,
      authorisedBy = entity.authorisedBy,
      activeFrom = entity.activeFrom,
      activeTo = entity.activeTo,
      appendComment = null,
      updatedBy = "UPDATED_BY",
      updatedByDisplayName = "UPDATED_BY_DISPLAY_NAME",
      source = ALERTS_SERVICE,
    )

    assertThat(entity.description).isNotNull()
    assertThat(entity.auditEvents().none { it.action == UPDATED }).isTrue
    assertThat(entity.publishedDomainEvents()).isEmpty()
  }

  @Test
  fun `update alert ignores null description`() {
    val entity = alertEntity()

    entity.update(
      description = null,
      authorisedBy = entity.authorisedBy,
      activeFrom = entity.activeFrom,
      activeTo = entity.activeTo,
      appendComment = null,
      updatedBy = "UPDATED_BY",
      updatedByDisplayName = "UPDATED_BY_DISPLAY_NAME",
      source = NOMIS,
    )

    assertThat(entity.description).isNotNull()
    assertThat(entity.auditEvents().none { it.action == UPDATED }).isTrue
    assertThat(entity.publishedDomainEvents()).isEmpty()
  }

  @Test
  fun `update alert ignores null authorised by`() {
    val entity = alertEntity()

    entity.update(
      description = entity.description,
      authorisedBy = null,
      activeFrom = entity.activeFrom,
      activeTo = entity.activeTo,
      appendComment = null,
      updatedBy = "UPDATED_BY",
      updatedByDisplayName = "UPDATED_BY_DISPLAY_NAME",
      source = ALERTS_SERVICE,
    )

    assertThat(entity.description).isNotNull()
    assertThat(entity.auditEvents().none { it.action == UPDATED }).isTrue
    assertThat(entity.publishedDomainEvents()).isEmpty()
  }

  @Test
  fun `update alert ignores null active from`() {
    val entity = alertEntity()

    entity.update(
      description = entity.description,
      authorisedBy = entity.authorisedBy,
      activeFrom = null,
      activeTo = entity.activeTo,
      appendComment = null,
      updatedBy = "UPDATED_BY",
      updatedByDisplayName = "UPDATED_BY_DISPLAY_NAME",
      source = NOMIS,
    )

    assertThat(entity.description).isNotNull()
    assertThat(entity.auditEvents().none { it.action == UPDATED }).isTrue
    assertThat(entity.publishedDomainEvents()).isEmpty()
  }

  @Test
  fun `update alert ignores null append comment`() {
    val entity = alertEntity()

    entity.update(
      description = entity.description,
      authorisedBy = entity.authorisedBy,
      activeFrom = entity.activeFrom,
      activeTo = entity.activeTo,
      appendComment = null,
      updatedBy = "UPDATED_BY",
      updatedByDisplayName = "UPDATED_BY_DISPLAY_NAME",
      source = ALERTS_SERVICE,
    )

    assertThat(entity.description).isNotNull()
    assertThat(entity.auditEvents().none { it.action == UPDATED }).isTrue
    assertThat(entity.publishedDomainEvents()).isEmpty()
  }

  @Test
  fun `update alert ignores empty append comment`() {
    val entity = alertEntity()

    entity.update(
      description = entity.description,
      authorisedBy = entity.authorisedBy,
      activeFrom = entity.activeFrom,
      activeTo = entity.activeTo,
      appendComment = "",
      updatedBy = "UPDATED_BY",
      updatedByDisplayName = "UPDATED_BY_DISPLAY_NAME",
      source = NOMIS,
    )

    assertThat(entity.description).isNotNull()
    assertThat(entity.auditEvents().none { it.action == UPDATED }).isTrue
    assertThat(entity.publishedDomainEvents()).isEmpty()
  }

  @Test
  fun `update alert ignores whitespace append comment`() {
    val entity = alertEntity()

    entity.update(
      description = entity.description,
      authorisedBy = entity.authorisedBy,
      activeFrom = entity.activeFrom,
      activeTo = entity.activeTo,
      appendComment = "     ",
      updatedBy = "UPDATED_BY",
      updatedByDisplayName = "UPDATED_BY_DISPLAY_NAME",
      source = ALERTS_SERVICE,
    )

    assertThat(entity.description).isNotNull()
    assertThat(entity.auditEvents().none { it.action == UPDATED }).isTrue
    assertThat(entity.publishedDomainEvents()).isEmpty()
  }

  @Test
  fun `update alert description only`() {
    val entity = alertEntity()
    val updatedDescription = "Updated description"
    val source = NOMIS
    val expectedDescription = "Updated alert description from '${entity.description}' to '$updatedDescription'\n"

    entity.update(
      description = updatedDescription,
      authorisedBy = entity.authorisedBy,
      activeFrom = entity.activeFrom,
      activeTo = entity.activeTo,
      appendComment = null,
      updatedBy = "UPDATED_BY",
      updatedByDisplayName = "UPDATED_BY_DISPLAY_NAME",
      source = source,
    )

    assertThat(entity.auditEvents().single { it.action == UPDATED }.description).isEqualTo(expectedDescription)
    with(entity.publishedDomainEvents().single() as AlertUpdatedEvent) {
      assertThat(source).isEqualTo(source)
      assertThat(descriptionUpdated).isTrue
      assertThat(authorisedByUpdated).isFalse
      assertThat(activeFromUpdated).isFalse
      assertThat(activeToUpdated).isFalse
      assertThat(commentAppended).isFalse
    }
  }

  @Test
  fun `update alert authorised by only`() {
    val entity = alertEntity()
    val updatedAuthorisedBy = "Updated authorised by"
    val source = ALERTS_SERVICE
    val expectedDescription = "Updated authorised by from '${entity.authorisedBy}' to '$updatedAuthorisedBy'\n"

    entity.update(
      description = entity.description,
      authorisedBy = updatedAuthorisedBy,
      activeFrom = entity.activeFrom,
      activeTo = entity.activeTo,
      appendComment = null,
      updatedBy = "UPDATED_BY",
      updatedByDisplayName = "UPDATED_BY_DISPLAY_NAME",
      source = source,
    )

    assertThat(entity.auditEvents().single { it.action == UPDATED }.description).isEqualTo(expectedDescription)
    with(entity.publishedDomainEvents().single() as AlertUpdatedEvent) {
      assertThat(source).isEqualTo(source)
      assertThat(descriptionUpdated).isFalse
      assertThat(authorisedByUpdated).isTrue
      assertThat(activeFromUpdated).isFalse
      assertThat(activeToUpdated).isFalse
      assertThat(commentAppended).isFalse
    }
  }

  @Test
  fun `update alert active from only`() {
    val entity = alertEntity()
    val updatedActiveFrom = entity.activeFrom.plusDays(1)
    val source = NOMIS
    val expectedDescription = "Updated active from from '${entity.activeFrom}' to '$updatedActiveFrom'\n"

    entity.update(
      description = entity.description,
      authorisedBy = entity.authorisedBy,
      activeFrom = updatedActiveFrom,
      activeTo = entity.activeTo,
      appendComment = null,
      updatedBy = "UPDATED_BY",
      updatedByDisplayName = "UPDATED_BY_DISPLAY_NAME",
      source = source,
    )

    assertThat(entity.auditEvents().single { it.action == UPDATED }.description).isEqualTo(expectedDescription)
    with(entity.publishedDomainEvents().single() as AlertUpdatedEvent) {
      assertThat(source).isEqualTo(source)
      assertThat(descriptionUpdated).isFalse
      assertThat(authorisedByUpdated).isFalse
      assertThat(activeFromUpdated).isTrue
      assertThat(activeToUpdated).isFalse
      assertThat(commentAppended).isFalse
    }
  }

  @Test
  fun `update alert active to only`() {
    val entity = alertEntity()
    val updatedActiveTo = entity.activeTo!!.plusDays(1)
    val source = ALERTS_SERVICE
    val expectedDescription = "Updated active to from '${entity.activeTo}' to '$updatedActiveTo'\n"

    entity.update(
      description = entity.description,
      authorisedBy = entity.authorisedBy,
      activeFrom = entity.activeFrom,
      activeTo = updatedActiveTo,
      appendComment = null,
      updatedBy = "UPDATED_BY",
      updatedByDisplayName = "UPDATED_BY_DISPLAY_NAME",
      source = source,
    )

    assertThat(entity.auditEvents().single { it.action == UPDATED }.description).isEqualTo(expectedDescription)
    with(entity.publishedDomainEvents().single() as AlertUpdatedEvent) {
      assertThat(source).isEqualTo(source)
      assertThat(descriptionUpdated).isFalse
      assertThat(authorisedByUpdated).isFalse
      assertThat(activeFromUpdated).isFalse
      assertThat(activeToUpdated).isTrue
      assertThat(commentAppended).isFalse
    }
  }

  @Test
  fun `update alert append comment only`() {
    val entity = alertEntity()
    val updatedAppendComment = "Appended comment"
    val updatedAt = LocalDateTime.now()
    val updatedBy = "UPDATED_BY"
    val updatedByDisplayName = "UPDATED_BY_DISPLAY_NAME"
    val source = NOMIS
    val expectedDescription = "Comment '$updatedAppendComment' was added\n"

    entity.update(
      description = entity.description,
      authorisedBy = entity.authorisedBy,
      activeFrom = entity.activeFrom,
      activeTo = entity.activeTo,
      appendComment = updatedAppendComment,
      updatedAt = updatedAt,
      updatedBy = updatedBy,
      updatedByDisplayName = updatedByDisplayName,
      source = source,
    )

    with(entity.comments().single()) {
      assertThat(comment).isEqualTo(updatedAppendComment)
      assertThat(createdAt).isEqualTo(updatedAt)
      assertThat(createdBy).isEqualTo(updatedBy)
      assertThat(createdByDisplayName).isEqualTo(updatedByDisplayName)
    }
    assertThat(entity.auditEvents().single { it.action == UPDATED }.description).isEqualTo(expectedDescription)
    with(entity.publishedDomainEvents().single() as AlertUpdatedEvent) {
      assertThat(source).isEqualTo(source)
      assertThat(descriptionUpdated).isFalse
      assertThat(authorisedByUpdated).isFalse
      assertThat(activeFromUpdated).isFalse
      assertThat(activeToUpdated).isFalse
      assertThat(commentAppended).isTrue
    }
  }

  @Test
  fun `update alert append comment trims comment`() {
    val entity = alertEntity()
    val updatedAppendComment = " Appended comment  "
    val source = ALERTS_SERVICE
    val expectedDescription = "Comment '${updatedAppendComment.trim()}' was added\n"

    entity.update(
      description = entity.description,
      authorisedBy = entity.authorisedBy,
      activeFrom = entity.activeFrom,
      activeTo = entity.activeTo,
      appendComment = updatedAppendComment,
      updatedBy = "UPDATED_BY",
      updatedByDisplayName = "UPDATED_BY_DISPLAY_NAME",
      source = source,
    )

    assertThat(entity.comments().single().comment).isEqualTo(updatedAppendComment.trim())
    assertThat(entity.auditEvents().single { it.action == UPDATED }.description).isEqualTo(expectedDescription)
  }

  @Test
  fun `delete audits event`() {
    val entity = alertEntity()
    val deletedAt = LocalDateTime.now()

    entity.delete(deletedAt, "DELETED_BY", "DELETED_BY_DISPLAY_NAME", NOMIS)

    assertThat(entity.deletedAt()).isEqualTo(deletedAt)
    assertThat(entity.auditEvents().single { it.action == DELETED }).isEqualTo(
      AuditEvent(
        alert = entity,
        action = DELETED,
        description = "Alert deleted",
        actionedAt = deletedAt,
        actionedBy = "DELETED_BY",
        actionedByDisplayName = "DELETED_BY_DISPLAY_NAME",
      ),
    )
  }

  @Test
  fun `delete raises domain event`() {
    val entity = alertEntity()
    val deletedAt = LocalDateTime.now()
    val deletedBy = "DELETED_BY"
    val source = ALERTS_SERVICE

    entity.delete(deletedAt, deletedBy, "DELETED_BY_DISPLAY_NAME", source)

    assertThat(entity.publishedDomainEvents().single()).isEqualTo(
      AlertDeletedEvent(
        alertUuid = entity.alertUuid,
        prisonNumber = entity.prisonNumber,
        alertCode = entity.alertCode.code,
        occurredAt = deletedAt,
        source = source,
        deletedBy = deletedBy,
      ),
    )
  }

  private fun alertEntity(
    createdAt: LocalDateTime = LocalDateTime.now().minusDays(3),
    createdBy: String = "CREATED_BY",
    createdByDisplayName: String = "CREATED_BY_DISPLAY_NAME",
  ) =
    Alert(
      alertUuid = UUID.randomUUID(),
      alertCode = alertCodeVictim(),
      prisonNumber = PRISON_NUMBER,
      description = "Alert description",
      authorisedBy = "A. Authorizer",
      activeFrom = LocalDate.now().minusDays(3),
      activeTo = LocalDate.now().plusDays(3),
      createdAt = createdAt,
    ).apply {
      auditEvent(
        action = CREATED,
        description = "Alert created",
        actionedAt = createdAt,
        actionedBy = createdBy,
        actionedByDisplayName = createdByDisplayName,
      )
    }
}
