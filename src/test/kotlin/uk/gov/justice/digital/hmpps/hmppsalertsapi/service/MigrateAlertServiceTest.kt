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
import org.mockito.kotlin.lastValue
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.hmppsalertsapi.domain.toAlertEntity
import uk.gov.justice.digital.hmpps.hmppsalertsapi.entity.Alert
import uk.gov.justice.digital.hmpps.hmppsalertsapi.enumeration.AuditEventAction
import uk.gov.justice.digital.hmpps.hmppsalertsapi.enumeration.Source.NOMIS
import uk.gov.justice.digital.hmpps.hmppsalertsapi.integration.wiremock.PRISON_NUMBER
import uk.gov.justice.digital.hmpps.hmppsalertsapi.model.MigratedAlert
import uk.gov.justice.digital.hmpps.hmppsalertsapi.repository.AlertCodeRepository
import uk.gov.justice.digital.hmpps.hmppsalertsapi.repository.AlertRepository
import uk.gov.justice.digital.hmpps.hmppsalertsapi.utils.ALERT_CODE_VICTIM
import uk.gov.justice.digital.hmpps.hmppsalertsapi.utils.alertCodeRefusingToShieldInactive
import uk.gov.justice.digital.hmpps.hmppsalertsapi.utils.alertCodeVictim
import uk.gov.justice.digital.hmpps.hmppsalertsapi.utils.migrateAlert
import uk.gov.justice.digital.hmpps.hmppsalertsapi.utils.migrateAlertRequest
import java.time.LocalDate
import java.time.LocalDateTime
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
  fun `migrate single alert will throw exception if code not found`() {
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
          activeCaseLoadId = null,
        )
      },
    )
    underTest.migrateAlert(migrateAlertRequest)
    verify(alertRepository).saveAndFlush(argumentCaptor.capture())
    with(argumentCaptor.firstValue) {
      assertThat(alertUuid).isNotNull()
      assertThat(auditEvents()).hasSize(1)
      assertThat(alertCode).isEqualTo(alertCodeRefusingToShieldInactive())
      assertThat(migratedAt).isCloseTo(LocalDateTime.now(), within(3, SECONDS))
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
          activeCaseLoadId = null,
        )
      },
    )
    underTest.migrateAlert(migrateAlertRequest)
    verify(alertRepository).saveAndFlush(argumentCaptor.capture())
    with(argumentCaptor.firstValue) {
      assertThat(alertUuid).isNotNull()
      assertThat(auditEvents()).hasSize(1)
      assertThat(alertCode).isEqualTo(alertCodeVictim())
      assertThat(migratedAt).isCloseTo(LocalDateTime.now(), within(3, SECONDS))
      assertThat(publishedDomainEvents()).isEmpty()
    }
  }

  @Test
  fun `will throw exception if code not found`() {
    whenever(alertCodeRepository.findByCodeIn(any())).thenReturn(emptyList())
    val exception = assertThrows<IllegalArgumentException> {
      underTest.migratePrisonerAlerts(PRISON_NUMBER, listOf(migrateAlert()))
    }
    assertThat(exception.message).isEqualTo("Alert code(s) 'VI' not found")
  }

  @Test
  fun `converts migration request and saves alert`() {
    val request = migrateAlert()
    val alertCode = alertCodeVictim()
    whenever(alertCodeRepository.findByCodeIn(listOf(request.alertCode))).thenReturn(listOf(alertCode))
    whenever(alertRepository.save(argumentCaptor.capture())).thenAnswer { argumentCaptor.firstValue }
    underTest.migratePrisonerAlerts(PRISON_NUMBER, listOf(request))
    with(argumentCaptor.firstValue) {
      assertThat(this).isEqualTo(request.toAlertEntity(PRISON_NUMBER, alertCode, migratedAlert!!.migratedAt).copy(alertUuid = alertUuid))
    }
    verify(alertRepository, times(2)).flush()
  }

  @Test
  fun `returns migrated alert mapping`() {
    val request = migrateAlert()
    whenever(alertCodeRepository.findByCodeIn(any())).thenReturn(listOf(alertCodeVictim()))
    whenever(alertRepository.save(argumentCaptor.capture())).thenAnswer { argumentCaptor.firstValue }
    val migratedAlert = underTest.migratePrisonerAlerts(PRISON_NUMBER, listOf(request)).single()
    assertThat(migratedAlert).isEqualTo(
      MigratedAlert(
        offenderBookId = request.offenderBookId,
        bookingSeq = request.bookingSeq,
        alertSeq = request.alertSeq,
        alertUuid = argumentCaptor.firstValue.alertUuid,
      ),
    )
  }

  @Test
  fun `deletes existing alerts`() {
    val request = migrateAlert()
    val alertCode = alertCodeVictim()
    val existingAlert = request.toAlertEntity(PRISON_NUMBER, alertCode)
    whenever(alertCodeRepository.findByCodeIn(any())).thenReturn(listOf(alertCode))
    whenever(alertRepository.findByPrisonNumber(PRISON_NUMBER)).thenReturn(listOf(existingAlert))
    whenever(alertRepository.save(argumentCaptor.capture())).thenAnswer { argumentCaptor.firstValue }
    val migratedAlert = underTest.migratePrisonerAlerts(PRISON_NUMBER, listOf(request)).single()
    assertThat(migratedAlert.alertUuid).isNotEqualTo(existingAlert.alertUuid)
    verify(alertRepository).deleteAll(listOf(existingAlert))
    verify(alertRepository, times(2)).flush()
  }

  @Test
  fun `accepts two active alerts with the same alert code`() {
    val request = listOf(
      migrateAlert().copy(
        alertSeq = 1,
        alertCode = ALERT_CODE_VICTIM,
        description = "Active",
        activeFrom = LocalDate.now().minusDays(1),
        activeTo = null,
      ),
      migrateAlert().copy(
        alertSeq = 2,
        alertCode = ALERT_CODE_VICTIM,
        description = "Active",
        activeFrom = LocalDate.now().minusDays(1),
        activeTo = null,
      ),
    )
    whenever(alertCodeRepository.findByCodeIn(any())).thenReturn(listOf(alertCodeVictim()))
    whenever(alertRepository.save(argumentCaptor.capture())).thenAnswer { argumentCaptor.lastValue }
    val migratedAlerts = underTest.migratePrisonerAlerts(PRISON_NUMBER, request)
    assertThat(migratedAlerts).hasSize(2)
    verify(alertRepository, times(2)).save(any())
  }

  @Test
  fun `accepts an active alert and an alert that will become active with the same alert code`() {
    val request = listOf(
      migrateAlert().copy(
        alertSeq = 1,
        alertCode = ALERT_CODE_VICTIM,
        description = "Active",
        activeFrom = LocalDate.now().minusDays(1),
        activeTo = null,
      ),
      migrateAlert().copy(
        alertSeq = 2,
        alertCode = ALERT_CODE_VICTIM,
        description = "Will become active",
        activeFrom = LocalDate.now().plusDays(1),
        activeTo = null,
      ),
    )
    whenever(alertCodeRepository.findByCodeIn(any())).thenReturn(listOf(alertCodeVictim()))
    whenever(alertRepository.save(argumentCaptor.capture())).thenAnswer { argumentCaptor.lastValue }
    val migratedAlerts = underTest.migratePrisonerAlerts(PRISON_NUMBER, request)
    assertThat(migratedAlerts).hasSize(2)
    verify(alertRepository, times(2)).save(any())
  }

  @Test
  fun `accepts alerts from historic bookings`() {
    val request = listOf(
      migrateAlert().copy(
        offenderBookId = 54321,
        bookingSeq = 2,
        alertSeq = 1,
        alertCode = ALERT_CODE_VICTIM,
        description = "Active from booking sequence > 1",
        activeFrom = LocalDate.now().minusDays(1),
        activeTo = null,
      ),
      migrateAlert().copy(
        offenderBookId = 54321,
        bookingSeq = 2,
        alertSeq = 2,
        alertCode = ALERT_CODE_VICTIM,
        description = "Will become active from booking sequence > 1",
        activeFrom = LocalDate.now().plusDays(1),
        activeTo = null,
      ),
      migrateAlert().copy(
        offenderBookId = 54321,
        bookingSeq = 2,
        alertSeq = 3,
        alertCode = ALERT_CODE_VICTIM,
        description = "Inactive from booking sequence > 1",
        activeFrom = LocalDate.now().minusDays(2),
        activeTo = LocalDate.now().minusDays(1),
      ),
    )
    whenever(alertCodeRepository.findByCodeIn(any())).thenReturn(listOf(alertCodeVictim()))
    whenever(alertRepository.save(argumentCaptor.capture())).thenAnswer { argumentCaptor.lastValue }
    val migratedAlerts = underTest.migratePrisonerAlerts(PRISON_NUMBER, request)
    assertThat(migratedAlerts).hasSize(3)
    verify(alertRepository, times(3)).save(any())
  }
}
