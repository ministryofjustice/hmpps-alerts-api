package uk.gov.justice.digital.hmpps.hmppsalertsapi.entity

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.hmppsalertsapi.enumeration.AuditEventAction
import uk.gov.justice.digital.hmpps.hmppsalertsapi.integration.wiremock.PRISON_NUMBER
import uk.gov.justice.digital.hmpps.hmppsalertsapi.utils.alertCodeVictim
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
        action = AuditEventAction.UPDATED,
        description = "Alert updated",
        actionedAt = actionedAt,
        actionedBy = "UPDATED_BY",
        actionedByDisplayName = "UPDATED_BY_DISPLAY_NAME",
      )
    }

    assertThat(entity.auditEvents().single { it.action == AuditEventAction.UPDATED }).isEqualTo(
      AuditEvent(
        alert = entity,
        action = AuditEventAction.UPDATED,
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
        action = AuditEventAction.UPDATED,
        description = "Alert updated",
        actionedAt = LocalDateTime.now().minusDays(2),
        actionedBy = "UPDATED_BY_2",
        actionedByDisplayName = "UPDATED_BY_2_DISPLAY_NAME",
      )
      auditEvent(
        action = AuditEventAction.DELETED,
        description = "Alert deleted",
        actionedAt = LocalDateTime.now(),
        actionedBy = "DELETED_BY",
        actionedByDisplayName = "DELETED_BY_DISPLAY_NAME",
      )
      auditEvent(
        action = AuditEventAction.UPDATED,
        description = "Alert updated",
        actionedAt = LocalDateTime.now().minusDays(1),
        actionedBy = "UPDATED_BY_3",
        actionedByDisplayName = "UPDATED_BY_3_DISPLAY_NAME",
      )
      auditEvent(
        action = AuditEventAction.UPDATED,
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
        action = AuditEventAction.UPDATED,
        description = "Alert updated",
        actionedAt = LocalDateTime.now().minusDays(2),
        actionedBy = "UPDATED_BY",
        actionedByDisplayName = "UPDATED_BY_DISPLAY_NAME",
      )
      auditEvent(
        action = AuditEventAction.DELETED,
        description = "Alert deleted",
        actionedAt = LocalDateTime.now(),
        actionedBy = "DELETED_BY",
        actionedByDisplayName = "DELETED_BY_DISPLAY_NAME",
      )
    }

    assertThat(entity.createdAuditEvent()).isEqualTo(
      AuditEvent(
        alert = entity,
        action = AuditEventAction.CREATED,
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
        action = AuditEventAction.UPDATED,
        description = "Alert updated",
        actionedAt = LocalDateTime.now().minusDays(2),
        actionedBy = "UPDATED_BY_2",
        actionedByDisplayName = "UPDATED_BY_2_DISPLAY_NAME",
      )
      auditEvent(
        action = AuditEventAction.DELETED,
        description = "Alert deleted",
        actionedAt = LocalDateTime.now(),
        actionedBy = "DELETED_BY",
        actionedByDisplayName = "DELETED_BY_DISPLAY_NAME",
      )
      auditEvent(
        action = AuditEventAction.UPDATED,
        description = "Alert updated",
        actionedAt = lastModifiedAt,
        actionedBy = "UPDATED_BY_3",
        actionedByDisplayName = "UPDATED_BY_3_DISPLAY_NAME",
      )
      auditEvent(
        action = AuditEventAction.UPDATED,
        description = "Alert updated",
        actionedAt = LocalDateTime.now().minusDays(3),
        actionedBy = "UPDATED_BY_1",
        actionedByDisplayName = "UPDATED_BY_1_DISPLAY_NAME",
      )
    }

    assertThat(entity.lastModifiedAuditEvent()).isEqualTo(
      AuditEvent(
        alert = entity,
        action = AuditEventAction.UPDATED,
        description = "Alert updated",
        actionedAt = lastModifiedAt,
        actionedBy = "UPDATED_BY_3",
        actionedByDisplayName = "UPDATED_BY_3_DISPLAY_NAME",
      ),
    )
  }

  @Test
  fun `delete audits event`() {
    val deletedAt = LocalDateTime.now()
    val entity = alertEntity()

    entity.delete(deletedAt, "DELETED_BY", "DELETED_BY_DISPLAY_NAME")

    assertThat(entity.deletedAt()).isEqualTo(deletedAt)
    assertThat(entity.auditEvents().single { it.action == AuditEventAction.DELETED }).isEqualTo(
      AuditEvent(
        alert = entity,
        action = AuditEventAction.DELETED,
        description = "Alert deleted",
        actionedAt = deletedAt,
        actionedBy = "DELETED_BY",
        actionedByDisplayName = "DELETED_BY_DISPLAY_NAME",
      ),
    )
  }

  private fun alertEntity(createdAt: LocalDateTime = LocalDateTime.now().minusDays(3)) =
    Alert(
      alertUuid = UUID.randomUUID(),
      alertCode = alertCodeVictim(),
      prisonNumber = PRISON_NUMBER,
      description = "Alert description",
      authorisedBy = "A. Authorizer",
      activeFrom = LocalDate.now().minusDays(3),
      activeTo = LocalDate.now().plusDays(3),
    ).apply {
      auditEvent(
        action = AuditEventAction.CREATED,
        description = "Alert created",
        actionedAt = createdAt,
        actionedBy = "CREATED_BY",
        actionedByDisplayName = "CREATED_BY_DISPLAY_NAME",
      )
    }
}
