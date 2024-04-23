package uk.gov.justice.digital.hmpps.hmppsalertsapi.service

import org.assertj.core.api.Assertions
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.ArgumentCaptor
import org.mockito.Captor
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.firstValue
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.hmppsalertsapi.config.AlertRequestContext
import uk.gov.justice.digital.hmpps.hmppsalertsapi.entity.AlertCode
import uk.gov.justice.digital.hmpps.hmppsalertsapi.entity.AlertType
import uk.gov.justice.digital.hmpps.hmppsalertsapi.model.request.CreateAlertCodeRequest
import uk.gov.justice.digital.hmpps.hmppsalertsapi.repository.AlertCodeRepository
import uk.gov.justice.digital.hmpps.hmppsalertsapi.repository.AlertTypeRepository
import uk.gov.justice.digital.hmpps.hmppsalertsapi.utils.alertCodeVictim
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

@ExtendWith(MockitoExtension::class)
class AlertCodeServiceTest {
  @Mock
  lateinit var alertCodeRepository: AlertCodeRepository

  @Mock
  lateinit var alertTypeRepository: AlertTypeRepository

  @InjectMocks
  lateinit var underTest: AlertCodeService

  @Captor
  lateinit var entityCaptor: ArgumentCaptor<AlertCode>

  @Test
  fun `save an alert code`() {
    whenever(alertTypeRepository.findByCode(any())).thenReturn(alertType())
    whenever(alertCodeRepository.saveAndFlush(any())).thenReturn(alertCodeVictim())
    underTest.createAlertCode(
      CreateAlertCodeRequest(code = "A", description = "Alert code A", parent = "T"),
      AlertRequestContext(username = "USER", userDisplayName = "USER", activeCaseLoadId = null),
    )
    verify(alertCodeRepository).saveAndFlush(entityCaptor.capture())
    val value = entityCaptor.firstValue
    assertThat(value).isNotNull
    assertThat(value.createdBy).isEqualTo("USER")
    assertThat(value.code).isEqualTo("A")
    assertThat(value.description).isEqualTo("Alert code A")
  }

  @Test
  fun `delete an alert code`() {
    whenever(alertCodeRepository.findByCode(any())).thenReturn(alertCodeVictim())
    underTest.deactivateAlertCode(
      "VI",
      AlertRequestContext(username = "USER", userDisplayName = "USER", activeCaseLoadId = null),
    )
    verify(alertCodeRepository).saveAndFlush(entityCaptor.capture())
    val value = entityCaptor.firstValue
    assertThat(value).isNotNull
    assertThat(value.deactivatedBy).isEqualTo("USER")
    assertThat(value.deactivatedAt).isCloseTo(LocalDateTime.now(), Assertions.within(3, ChronoUnit.SECONDS))
  }

  private fun alertType(alertTypeId: Long = 1, code: String = "A") =
    AlertType(
      alertTypeId,
      code,
      "Alert type $code",
      1,
      LocalDateTime.now().minusDays(3),
      "CREATED_BY",
    ).apply {
      modifiedAt = LocalDateTime.now().minusDays(2)
      modifiedBy = "MODIFIED_BY"
    }
}
