package uk.gov.justice.digital.hmpps.hmppsalertsapi.domain

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.hmppsalertsapi.config.AlertRequestContext
import uk.gov.justice.digital.hmpps.hmppsalertsapi.enumeration.AuditEventAction
import uk.gov.justice.digital.hmpps.hmppsalertsapi.enumeration.Source.DPS
import uk.gov.justice.digital.hmpps.hmppsalertsapi.enumeration.Source.NOMIS
import uk.gov.justice.digital.hmpps.hmppsalertsapi.integration.wiremock.PRISON_CODE_LEEDS
import uk.gov.justice.digital.hmpps.hmppsalertsapi.integration.wiremock.PRISON_CODE_MOORLANDS
import uk.gov.justice.digital.hmpps.hmppsalertsapi.integration.wiremock.PRISON_NUMBER
import uk.gov.justice.digital.hmpps.hmppsalertsapi.integration.wiremock.TEST_USER
import uk.gov.justice.digital.hmpps.hmppsalertsapi.integration.wiremock.TEST_USER_NAME
import uk.gov.justice.digital.hmpps.hmppsalertsapi.model.request.CreateAlert
import uk.gov.justice.digital.hmpps.hmppsalertsapi.utils.ALERT_CODE_VICTIM
import uk.gov.justice.digital.hmpps.hmppsalertsapi.utils.alertCodeVictim
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID
import uk.gov.justice.digital.hmpps.hmppsalertsapi.entity.Alert as AlertEntity
import uk.gov.justice.digital.hmpps.hmppsalertsapi.entity.Comment as CommentEntity
import uk.gov.justice.digital.hmpps.hmppsalertsapi.model.Alert as AlertModel
import uk.gov.justice.digital.hmpps.hmppsalertsapi.model.Comment as CommentModel

class AlertTranslationTest {
  @Test
  fun `convert create alert to alert entity`() {
    val request = CreateAlert(
      prisonNumber = PRISON_NUMBER,
      alertCode = ALERT_CODE_VICTIM,
      description = "Alert description",
      authorisedBy = "A. Authorizer",
      activeFrom = LocalDate.now().minusDays(3),
      activeTo = LocalDate.now().plusDays(3),
    )
    val context = AlertRequestContext(
      username = TEST_USER,
      userDisplayName = TEST_USER_NAME,
      activeCaseLoadId = PRISON_CODE_MOORLANDS,
    )

    val entity = request.toAlertEntity(alertCodeVictim(), context.requestAt, context.username, context.userDisplayName, context.source, context.activeCaseLoadId)

    assertThat(entity).isEqualTo(
      AlertEntity(
        alertUuid = entity.alertUuid,
        alertCode = alertCodeVictim(),
        prisonNumber = request.prisonNumber,
        description = request.description,
        authorisedBy = request.authorisedBy,
        activeFrom = request.activeFrom!!,
        activeTo = request.activeTo,
        createdAt = context.requestAt,
      ).apply {
        auditEvent(
          action = AuditEventAction.CREATED,
          description = "Alert created",
          actionedAt = context.requestAt,
          actionedBy = context.username,
          actionedByDisplayName = context.userDisplayName,
          source = context.source,
          activeCaseLoadId = context.activeCaseLoadId,
        )
      },
    )
  }

  @Test
  fun `replace supplied description with fixed description for security alerts`() {
    val request = CreateAlert(
      prisonNumber = PRISON_NUMBER,
      alertCode = ALERT_CODE_SECURITY_ALERT_OCG_NOMINAL,
      description = "Alert description",
      authorisedBy = "A. Authorizer",
      activeFrom = null,
      activeTo = LocalDate.now().plusDays(3),
    )
    val context = AlertRequestContext(
      username = TEST_USER,
      userDisplayName = TEST_USER_NAME,
      activeCaseLoadId = PRISON_CODE_LEEDS,
    )

    val entity = request.toAlertEntity(alertCodeVictim(), context.requestAt, context.username, context.userDisplayName, context.source, context.activeCaseLoadId)

    assertThat(entity.description).isEqualTo(alertCodeDescriptionMap[ALERT_CODE_SECURITY_ALERT_OCG_NOMINAL])
  }

  @Test
  fun `use today when active from is not supplied`() {
    val request = CreateAlert(
      prisonNumber = PRISON_NUMBER,
      alertCode = ALERT_CODE_VICTIM,
      description = "Alert description",
      authorisedBy = "A. Authorizer",
      activeFrom = null,
      activeTo = LocalDate.now().plusDays(3),
    )
    val context = AlertRequestContext(
      username = TEST_USER,
      userDisplayName = TEST_USER_NAME,
      activeCaseLoadId = PRISON_CODE_LEEDS,
    )

    val entity = request.toAlertEntity(alertCodeVictim(), context.requestAt, context.username, context.userDisplayName, context.source, context.activeCaseLoadId)

    assertThat(entity.activeFrom).isEqualTo(LocalDate.now())
  }

  @Test
  fun `convert alert entity to model`() {
    val createdAt = LocalDateTime.now().minusDays(3)
    val entity = alertEntity(createdAt)

    val model = entity.toAlertModel()

    assertThat(model).isEqualTo(
      AlertModel(
        alertUuid = entity.alertUuid,
        alertCode = entity.alertCode.toAlertCodeSummary(),
        prisonNumber = entity.prisonNumber,
        description = entity.description,
        authorisedBy = entity.authorisedBy,
        activeFrom = entity.activeFrom,
        activeTo = entity.activeTo,
        isActive = entity.isActive(),
        comments = emptyList(),
        createdAt = createdAt,
        createdBy = "CREATED_BY",
        createdByDisplayName = "CREATED_BY_DISPLAY_NAME",
        lastModifiedAt = null,
        lastModifiedBy = null,
        lastModifiedByDisplayName = null,
        activeToLastSetAt = null,
        activeToLastSetBy = null,
        activeToLastSetByDisplayName = null,
      ),
    )
  }

  @Test
  fun `convert modified alert entity to model`() {
    val lastModifiedAt = LocalDateTime.now().minusDays(2)
    val entity = alertEntity().apply {
      auditEvent(
        action = AuditEventAction.UPDATED,
        description = "Alert updated",
        actionedAt = lastModifiedAt,
        actionedBy = "UPDATED_BY",
        actionedByDisplayName = "UPDATED_BY_DISPLAY_NAME",
        source = DPS,
        activeCaseLoadId = PRISON_CODE_LEEDS,
      )
    }

    val model = entity.toAlertModel()

    with(model) {
      assertThat(lastModifiedAt).isEqualTo(lastModifiedAt)
      assertThat(lastModifiedBy).isEqualTo("UPDATED_BY")
      assertThat(lastModifiedByDisplayName).isEqualTo("UPDATED_BY_DISPLAY_NAME")
    }
  }

  @Test
  fun `convert alert entity to model last modified uses most recent updated audit event`() {
    val lastModifiedAt = LocalDateTime.now().minusDays(1)
    val entity = alertEntity().apply {
      auditEvent(
        action = AuditEventAction.UPDATED,
        description = "Alert updated",
        actionedAt = LocalDateTime.now().minusDays(2),
        actionedBy = "UPDATED_BY_2",
        actionedByDisplayName = "UPDATED_BY_2_DISPLAY_NAME",
        source = DPS,
        activeCaseLoadId = PRISON_CODE_MOORLANDS,
      )
      auditEvent(
        action = AuditEventAction.UPDATED,
        description = "Alert updated",
        actionedAt = lastModifiedAt,
        actionedBy = "UPDATED_BY_3",
        actionedByDisplayName = "UPDATED_BY_3_DISPLAY_NAME",
        source = NOMIS,
        activeCaseLoadId = PRISON_CODE_LEEDS,
      )
      auditEvent(
        action = AuditEventAction.UPDATED,
        description = "Alert updated",
        actionedAt = LocalDateTime.now().minusDays(3),
        actionedBy = "UPDATED_BY_1",
        actionedByDisplayName = "UPDATED_BY_1_DISPLAY_NAME",
        source = DPS,
        activeCaseLoadId = PRISON_CODE_MOORLANDS,
      )
    }

    val model = entity.toAlertModel()

    with(model) {
      assertThat(lastModifiedAt).isEqualTo(lastModifiedAt)
      assertThat(lastModifiedBy).isEqualTo("UPDATED_BY_3")
      assertThat(lastModifiedByDisplayName).isEqualTo("UPDATED_BY_3_DISPLAY_NAME")
    }
  }

  @Test
  fun `convert alert entity to model with different last modified and last deactivated audit event`() {
    val lastModifiedAt = LocalDateTime.now().minusDays(1)
    val lastDeactivatedAt = LocalDateTime.now().minusDays(2)
    val entity = alertEntity().apply {
      auditEvent(
        action = AuditEventAction.UPDATED,
        description = "Alert updated",
        actionedAt = lastDeactivatedAt,
        actionedBy = "UPDATED_BY_2",
        actionedByDisplayName = "UPDATED_BY_2_DISPLAY_NAME",
        source = DPS,
        activeCaseLoadId = PRISON_CODE_MOORLANDS,
        activeToUpdated = true,
      )
      auditEvent(
        action = AuditEventAction.UPDATED,
        description = "Alert updated",
        actionedAt = lastModifiedAt,
        actionedBy = "UPDATED_BY_3",
        actionedByDisplayName = "UPDATED_BY_3_DISPLAY_NAME",
        source = NOMIS,
        activeCaseLoadId = PRISON_CODE_LEEDS,
        activeToUpdated = false,
      )
      auditEvent(
        action = AuditEventAction.UPDATED,
        description = "Alert updated",
        actionedAt = LocalDateTime.now().minusDays(3),
        actionedBy = "UPDATED_BY_1",
        actionedByDisplayName = "UPDATED_BY_1_DISPLAY_NAME",
        source = DPS,
        activeCaseLoadId = PRISON_CODE_MOORLANDS,
      )
    }

    val model = entity.toAlertModel()

    with(model) {
      assertThat(lastModifiedAt).isEqualTo(lastModifiedAt)
      assertThat(lastModifiedBy).isEqualTo("UPDATED_BY_3")
      assertThat(lastModifiedByDisplayName).isEqualTo("UPDATED_BY_3_DISPLAY_NAME")
      assertThat(activeToLastSetAt).isEqualTo(lastDeactivatedAt)
      assertThat(activeToLastSetBy).isEqualTo("UPDATED_BY_2")
      assertThat(activeToLastSetByDisplayName).isEqualTo("UPDATED_BY_2_DISPLAY_NAME")
    }
  }

  @Test
  fun `convert alert entity to model converts alert comment`() {
    var comment: CommentEntity
    val entity = alertEntity().apply {
      comment = addComment("Comment", LocalDateTime.now().minusDays(3), "COMMENT_BY", "COMMENT_BY_DISPLAY_NAME")
    }

    val model = entity.toAlertModel()

    assertThat(model.comments).isEqualTo(listOf(comment.toAlertCommentModel()))
  }

  @Test
  fun `convert alert entity to model orders alert comment`() {
    var comment1: CommentEntity
    var comment2: CommentEntity
    var comment3: CommentEntity
    val entity = alertEntity().apply {
      comment2 = addComment("Comment 2", LocalDateTime.now().minusDays(2), "COMMENT_BY_2", "COMMENT_BY_DISPLAY_NAME_2")
      comment3 = addComment("Comment 3", LocalDateTime.now().minusDays(1), "COMMENT_BY_3", "COMMENT_BY_DISPLAY_NAME_3")
      comment1 = addComment("Comment 1", LocalDateTime.now().minusDays(3), "COMMENT_BY_1", "COMMENT_BY_DISPLAY_NAME_1")
    }

    val model = entity.toAlertModel()

    assertThat(model.comments).isEqualTo(
      listOf(
        comment3.toAlertCommentModel(),
        comment2.toAlertCommentModel(),
        comment1.toAlertCommentModel(),
      ),
    )
  }

  @Test
  fun `convert comment entity to model`() {
    val createdAt = LocalDateTime.now().minusDays(3)
    val comment = alertEntity().addComment("Comment", createdAt, "COMMENT_BY", "COMMENT_BY_DISPLAY_NAME")

    val model = comment.toAlertCommentModel()

    assertThat(model).isEqualTo(
      CommentModel(
        commentUuid = comment.commentUuid,
        comment = "Comment",
        createdAt = createdAt,
        createdBy = "COMMENT_BY",
        createdByDisplayName = "COMMENT_BY_DISPLAY_NAME",
      ),
    )
  }

  private fun alertEntity(createdAt: LocalDateTime = LocalDateTime.now().minusDays(3)) =
    AlertEntity(
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
        action = AuditEventAction.CREATED,
        description = "Alert created",
        actionedAt = createdAt,
        actionedBy = "CREATED_BY",
        actionedByDisplayName = "CREATED_BY_DISPLAY_NAME",
        source = DPS,
        activeCaseLoadId = PRISON_CODE_LEEDS,
      )
    }
}
