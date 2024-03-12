package uk.gov.justice.digital.hmpps.hmppsalertsapi.domain

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.hmppsalertsapi.entity.Alert
import uk.gov.justice.digital.hmpps.hmppsalertsapi.enumeration.AuditEventAction.CREATED
import uk.gov.justice.digital.hmpps.hmppsalertsapi.enumeration.AuditEventAction.UPDATED
import uk.gov.justice.digital.hmpps.hmppsalertsapi.model.request.MigrateCommentRequest
import uk.gov.justice.digital.hmpps.hmppsalertsapi.utils.alertCodeVictim
import uk.gov.justice.digital.hmpps.hmppsalertsapi.utils.migrateAlertRequest
import java.time.LocalDateTime

class MigrateAlertTranslationTest {

  @Test
  fun `should create an alert entity`() {
    val request = migrateAlertRequest()
    val entity = request.toAlertEntity(alertCodeVictim())
    assertThat(entity).isEqualTo(
      Alert(
        alertUuid = entity.alertUuid,
        alertCode = alertCodeVictim(),
        prisonNumber = request.prisonNumber,
        description = request.description,
        authorisedBy = request.authorisedBy,
        activeFrom = request.activeFrom,
        activeTo = request.activeTo,
        migratedAt = entity.migratedAt,
      ).apply {
        auditEvent(
          action = CREATED,
          description = "Migrated alert created",
          actionedAt = request.createdAt,
          actionedBy = request.createdBy,
          actionedByDisplayName = request.createdByDisplayName,
        )
      },
    )
  }

  @Test
  fun `should create an alert entity with updated values set if present`() {
    val request = migrateAlertRequest(includeUpdate = true)
    val entity = request.toAlertEntity(alertCodeVictim())
    assertThat(entity).isEqualTo(
      Alert(
        alertUuid = entity.alertUuid,
        alertCode = alertCodeVictim(),
        prisonNumber = request.prisonNumber,
        description = request.description,
        authorisedBy = request.authorisedBy,
        activeFrom = request.activeFrom,
        activeTo = request.activeTo,
        migratedAt = entity.migratedAt,
      ).apply {
        auditEvent(
          action = CREATED,
          description = "Migrated alert created",
          actionedAt = request.createdAt,
          actionedBy = request.createdBy,
          actionedByDisplayName = request.createdByDisplayName,
        )
        auditEvent(
          action = UPDATED,
          description = "Migrated alert updated",
          actionedAt = request.updatedAt!!,
          actionedBy = request.updatedBy!!,
          actionedByDisplayName = request.updatedByDisplayName!!,
        )
      },
    )
  }

  @Test
  fun `should create an alert entity with comments`() {
    val comments = listOf(MigrateCommentRequest("comment", LocalDateTime.now().minusDays(1), "A11fds", "C omment"))
    val request = migrateAlertRequest(comments = comments)
    val entity = request.toAlertEntity(alertCodeVictim())
    assertThat(entity).isEqualTo(
      Alert(
        alertUuid = entity.alertUuid,
        alertCode = alertCodeVictim(),
        prisonNumber = request.prisonNumber,
        description = request.description,
        authorisedBy = request.authorisedBy,
        activeFrom = request.activeFrom,
        activeTo = request.activeTo,
        migratedAt = entity.migratedAt,
      ).apply {
        auditEvent(
          action = CREATED,
          description = "Migrated alert created",
          actionedAt = request.createdAt,
          actionedBy = request.createdBy,
          actionedByDisplayName = request.createdByDisplayName,
        )
        addComment(
          comments.single().comment,
          comments.single().createdAt,
          comments.single().createdBy,
          comments.single().createdByDisplayName,
        )
      },
    )
  }
}
