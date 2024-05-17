package uk.gov.justice.digital.hmpps.hmppsalertsapi.domain

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.hmppsalertsapi.entity.Alert
import uk.gov.justice.digital.hmpps.hmppsalertsapi.entity.AuditEvent
import uk.gov.justice.digital.hmpps.hmppsalertsapi.entity.MigratedAlert
import uk.gov.justice.digital.hmpps.hmppsalertsapi.enumeration.AuditEventAction.CREATED
import uk.gov.justice.digital.hmpps.hmppsalertsapi.enumeration.AuditEventAction.UPDATED
import uk.gov.justice.digital.hmpps.hmppsalertsapi.enumeration.Source.NOMIS
import uk.gov.justice.digital.hmpps.hmppsalertsapi.integration.wiremock.PRISON_NUMBER
import uk.gov.justice.digital.hmpps.hmppsalertsapi.model.request.MigrateCommentRequest
import uk.gov.justice.digital.hmpps.hmppsalertsapi.utils.alertCodeVictim
import uk.gov.justice.digital.hmpps.hmppsalertsapi.utils.migrateAlert
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
        createdAt = request.createdAt,
        migratedAt = entity.migratedAt,
      ).apply {
        auditEvent(
          action = CREATED,
          description = "Migrated alert created",
          actionedAt = request.createdAt,
          actionedBy = request.createdBy,
          actionedByDisplayName = request.createdByDisplayName,
          source = NOMIS,
          activeCaseLoadId = null,
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
        createdAt = request.createdAt,
        migratedAt = entity.migratedAt,
      ).apply {
        lastModifiedAt = request.updatedAt
        auditEvent(
          action = CREATED,
          description = "Migrated alert created",
          actionedAt = request.createdAt,
          actionedBy = request.createdBy,
          actionedByDisplayName = request.createdByDisplayName,
          source = NOMIS,
          activeCaseLoadId = null,
        )
        auditEvent(
          action = UPDATED,
          description = "Migrated alert updated",
          actionedAt = request.updatedAt!!,
          actionedBy = request.updatedBy!!,
          actionedByDisplayName = request.updatedByDisplayName!!,
          source = NOMIS,
          activeCaseLoadId = null,
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
        createdAt = request.createdAt,
        migratedAt = entity.migratedAt,
      ).apply {
        auditEvent(
          action = CREATED,
          description = "Migrated alert created",
          actionedAt = request.createdAt,
          actionedBy = request.createdBy,
          actionedByDisplayName = request.createdByDisplayName,
          source = NOMIS,
          activeCaseLoadId = null,
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

  @Test
  fun `convert migrate alert request to alert and migrated alert`() {
    val request = migrateAlert()
    val alertCode = alertCodeVictim()
    val migratedAt = LocalDateTime.now()
    val entity = request.toAlertEntity(PRISON_NUMBER, alertCode, migratedAt)
    assertThat(entity).isEqualTo(
      Alert(
        alertUuid = entity.alertUuid,
        alertCode = alertCode,
        prisonNumber = PRISON_NUMBER,
        description = request.description,
        authorisedBy = request.authorisedBy,
        activeFrom = request.activeFrom,
        activeTo = request.activeTo,
        createdAt = request.createdAt,
        migratedAt = entity.migratedAt,
      ).apply {
        migratedAlert = MigratedAlert(
          offenderBookId = request.offenderBookId,
          bookingSeq = request.bookingSeq,
          alertSeq = request.alertSeq,
          alert = this,
          migratedAt = migratedAt,
        )
        auditEvent(
          action = CREATED,
          description = "Migrated alert created",
          actionedAt = request.createdAt,
          actionedBy = request.createdBy,
          actionedByDisplayName = request.createdByDisplayName,
          source = NOMIS,
          activeCaseLoadId = null,
        )
      },
    )
  }

  @Test
  fun `convert migrate alert request with updated audit event`() {
    val request = migrateAlert().copy(
      updatedAt = LocalDateTime.now().minusDays(1),
      updatedBy = "AG1221GG",
      updatedByDisplayName = "Up Dated",
    )
    val entity = request.toAlertEntity(PRISON_NUMBER, alertCodeVictim())
    assertThat(entity.auditEvents()).usingRecursiveComparison().isEqualTo(
      listOf(
        AuditEvent(
          alert = entity,
          action = UPDATED,
          description = "Migrated alert updated",
          actionedAt = request.updatedAt!!,
          actionedBy = request.updatedBy!!,
          actionedByDisplayName = request.updatedByDisplayName!!,
          source = NOMIS,
          activeCaseLoadId = null,
        ),
        AuditEvent(
          alert = entity,
          action = CREATED,
          description = "Migrated alert created",
          actionedAt = request.createdAt,
          actionedBy = request.createdBy,
          actionedByDisplayName = "C Reated",
          source = NOMIS,
          activeCaseLoadId = null,
        ),
      ),
    )
  }
}
