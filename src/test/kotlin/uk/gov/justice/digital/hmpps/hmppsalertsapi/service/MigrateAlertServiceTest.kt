package uk.gov.justice.digital.hmpps.hmppsalertsapi.service

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.within
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.ArgumentCaptor
import org.mockito.Captor
import org.mockito.InjectMocks
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.firstValue
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.hmppsalertsapi.entity.Alert
import uk.gov.justice.digital.hmpps.hmppsalertsapi.enumeration.AuditEventAction
import uk.gov.justice.digital.hmpps.hmppsalertsapi.enumeration.Source.NOMIS
import uk.gov.justice.digital.hmpps.hmppsalertsapi.integration.wiremock.PRISON_NUMBER
import uk.gov.justice.digital.hmpps.hmppsalertsapi.repository.AlertCodeRepository
import uk.gov.justice.digital.hmpps.hmppsalertsapi.repository.AlertRepository
import uk.gov.justice.digital.hmpps.hmppsalertsapi.utils.alertCodeRefusingToShieldInactive
import uk.gov.justice.digital.hmpps.hmppsalertsapi.utils.alertCodeVictim
import uk.gov.justice.digital.hmpps.hmppsalertsapi.utils.migrateAlertRequest
import java.time.temporal.ChronoUnit.SECONDS
import java.util.UUID

@ExtendWith(MockitoExtension::class)
class MigrateAlertServiceTest {
  private val alertRepository = mock<AlertRepository>()
  private val alertCodeRepository = mock<AlertCodeRepository>()

  @InjectMocks
  private lateinit var underTest: MigrateAlertService

  @Captor
  private lateinit var argumentCaptor: ArgumentCaptor<Alert>

  @Test
  fun `will throw exception if code not found`() {
    whenever(alertCodeRepository.findByCode(any())).thenReturn(null)
    val exception = assertThrows<IllegalArgumentException> {
      underTest.migrateAlert(migrateAlertRequest())
    }
    assertThat(exception.message).isEqualTo("Alert code 'VI' not found")
  }

  @Test
  fun `will save even if code inactive`() {
    whenever(alertCodeRepository.findByCode(any())).thenReturn(alertCodeRefusingToShieldInactive())
    val migrateAlertRequest = migrateAlertRequest()
    whenever(alertRepository.saveAndFlush(any())).thenReturn(
      Alert(
        alertUuid = UUID.randomUUID(),
        alertCode = alertCodeRefusingToShieldInactive(),
        prisonNumber = PRISON_NUMBER,
        description = "Alert description",
        authorisedBy = "A. Authorizer",
        activeFrom = migrateAlertRequest.activeFrom,
        activeTo = migrateAlertRequest.activeTo,
        createdAt = migrateAlertRequest.createdAt,
        migratedAt = migrateAlertRequest.createdAt,
      ).apply {
        auditEvent(
          action = AuditEventAction.CREATED,
          description = "Migrated alert created",
          actionedAt = migrateAlertRequest.createdAt,
          actionedBy = migrateAlertRequest.createdBy,
          actionedByDisplayName = migrateAlertRequest.createdByDisplayName,
          source = NOMIS,
        )
      },
    )
    underTest.migrateAlert(migrateAlertRequest)
    verify(alertRepository).saveAndFlush(argumentCaptor.capture())
    with(argumentCaptor.firstValue) {
      assertThat(alertUuid).isNotNull()
      assertThat(auditEvents()).hasSize(1)
      assertThat(alertCode).isEqualTo(alertCodeRefusingToShieldInactive())
      assertThat(migratedAt).isCloseToUtcNow(within(3, SECONDS))
      assertThat(publishedDomainEvents()).isEmpty()
    }
  }

  @Test
  fun `will save if code active`() {
    whenever(alertCodeRepository.findByCode(any())).thenReturn(alertCodeVictim())
    val migrateAlertRequest = migrateAlertRequest()
    whenever(alertRepository.saveAndFlush(any())).thenReturn(
      Alert(
        alertUuid = UUID.randomUUID(),
        alertCode = alertCodeVictim(),
        prisonNumber = PRISON_NUMBER,
        description = "Alert description",
        authorisedBy = "A. Authorizer",
        activeFrom = migrateAlertRequest.activeFrom,
        activeTo = migrateAlertRequest.activeTo,
        createdAt = migrateAlertRequest.createdAt,
        migratedAt = migrateAlertRequest.createdAt,
      ).apply {
        auditEvent(
          action = AuditEventAction.CREATED,
          description = "Migrated alert created",
          actionedAt = migrateAlertRequest.createdAt,
          actionedBy = migrateAlertRequest.createdBy,
          actionedByDisplayName = migrateAlertRequest.createdByDisplayName,
          source = NOMIS,
        )
      },
    )
    underTest.migrateAlert(migrateAlertRequest)
    verify(alertRepository).saveAndFlush(argumentCaptor.capture())
    with(argumentCaptor.firstValue) {
      assertThat(alertUuid).isNotNull()
      assertThat(auditEvents()).hasSize(1)
      assertThat(alertCode).isEqualTo(alertCodeVictim())
      assertThat(migratedAt).isCloseToUtcNow(within(3, SECONDS))
      assertThat(publishedDomainEvents()).isEmpty()
    }
  }
}
