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
import uk.gov.justice.digital.hmpps.hmppsalertsapi.utils.alertCodeVictim
import uk.gov.justice.digital.hmpps.hmppsalertsapi.utils.migrateAlert
import java.time.LocalDateTime

class MigrateAlertTranslationTest {

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
      lastModifiedAt = LocalDateTime.now().minusDays(1),
      lastModifiedBy = "AG1221GG",
      lastModifiedByDisplayName = "Up Dated",
    )
    val entity = request.toAlertEntity(PRISON_NUMBER, alertCodeVictim())
    assertThat(entity.auditEvents()).usingRecursiveComparison().isEqualTo(
      listOf(
        AuditEvent(
          alert = entity,
          action = UPDATED,
          description = "Migrated alert updated",
          actionedAt = request.lastModifiedAt!!,
          actionedBy = request.lastModifiedBy!!,
          actionedByDisplayName = request.lastModifiedByDisplayName!!,
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
