package uk.gov.justice.digital.hmpps.hmppsalertsapi.service

import org.assertj.core.api.Assertions.assertThat
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
import org.mockito.kotlin.never
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.hmppsalertsapi.domain.toAlertEntity
import uk.gov.justice.digital.hmpps.hmppsalertsapi.entity.Alert
import uk.gov.justice.digital.hmpps.hmppsalertsapi.integration.wiremock.PRISON_NUMBER
import uk.gov.justice.digital.hmpps.hmppsalertsapi.model.MigratedAlert
import uk.gov.justice.digital.hmpps.hmppsalertsapi.repository.AlertCodeRepository
import uk.gov.justice.digital.hmpps.hmppsalertsapi.repository.AlertRepository
import uk.gov.justice.digital.hmpps.hmppsalertsapi.repository.MigratedAlertRepository
import uk.gov.justice.digital.hmpps.hmppsalertsapi.utils.ALERT_CODE_VICTIM
import uk.gov.justice.digital.hmpps.hmppsalertsapi.utils.alertCodeVictim
import uk.gov.justice.digital.hmpps.hmppsalertsapi.utils.migrateAlert
import java.time.LocalDate

@ExtendWith(MockitoExtension::class)
class MigrateAlertServiceTest {
  private val alertRepository = mock<AlertRepository>()
  private val alertCodeRepository = mock<AlertCodeRepository>()
  private val migratedAlertRepository = mock<MigratedAlertRepository>()

  @InjectMocks
  private lateinit var underTest: MigrateAlertService

  @Captor
  private lateinit var argumentCaptor: ArgumentCaptor<Alert>

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
    verify(alertRepository).flush()
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
  fun `returns existing migrated alert mapping`() {
    val request = migrateAlert()
    val alertCode = alertCodeVictim()
    val existingMigratedAlert = request.toAlertEntity(PRISON_NUMBER, alertCode).migratedAlert!!
    whenever(alertCodeRepository.findByCodeIn(any())).thenReturn(listOf(alertCode))
    whenever(migratedAlertRepository.findByOffenderBookIdAndAlertSeq(request.offenderBookId, request.alertSeq)).thenReturn(existingMigratedAlert)
    val migratedAlert = underTest.migratePrisonerAlerts(PRISON_NUMBER, listOf(request)).single()
    assertThat(migratedAlert).isEqualTo(
      MigratedAlert(
        offenderBookId = existingMigratedAlert.offenderBookId,
        bookingSeq = existingMigratedAlert.bookingSeq,
        alertSeq = existingMigratedAlert.alertSeq,
        alertUuid = existingMigratedAlert.alert.alertUuid,
      ),
    )
    verify(alertRepository, never()).save(any())
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
