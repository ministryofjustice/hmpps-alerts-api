package uk.gov.justice.digital.hmpps.hmppsalertsapi.service

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.ArgumentCaptor
import org.mockito.Captor
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.firstValue
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.hmppsalertsapi.config.AlertRequestContext
import uk.gov.justice.digital.hmpps.hmppsalertsapi.entity.AlertCode
import uk.gov.justice.digital.hmpps.hmppsalertsapi.entity.AlertType
import uk.gov.justice.digital.hmpps.hmppsalertsapi.exceptions.AlertTypeNotFound
import uk.gov.justice.digital.hmpps.hmppsalertsapi.model.request.CreateAlertCodeRequest
import uk.gov.justice.digital.hmpps.hmppsalertsapi.repository.AlertCodeRepository
import uk.gov.justice.digital.hmpps.hmppsalertsapi.repository.AlertTypeRepository
import uk.gov.justice.digital.hmpps.hmppsalertsapi.utils.alertCodeVictim
import java.time.LocalDateTime

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
  fun `retrieve all alert codes for alert type`() {
    whenever(alertTypeRepository.findByCode(any())).thenReturn(alertType())
    whenever(alertCodeRepository.findAllByAlertTypeCode(any())).thenReturn(listOf(alertCodeVictim()))
    val result = underTest.retrieveAlertCodeForAlertType("123")
    assertThat(result).isNotNull()
    assertThat(result.first().code).isEqualTo(alertCodeVictim().code)
  }

  @Test
  fun `retrieve all alert codes inactive alert type`() {
    whenever(alertTypeRepository.findByCode(any())).thenReturn(inactiveAlertType())
    assertThrows<AlertTypeNotFound> {
      underTest.retrieveAlertCodeForAlertType("123")
    }
    verify(alertCodeRepository, never()).findAllByAlertTypeCode(eq("123"))
  }

  @Test
  fun `retrieve all alert codes no alert type`() {
    whenever(alertTypeRepository.findByCode(any())).thenReturn(null)
    assertThrows<AlertTypeNotFound> {
      underTest.retrieveAlertCodeForAlertType("123")
    }
    verify(alertCodeRepository, never()).findAllByAlertTypeCode(eq("123"))
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

  private fun inactiveAlertType() =
    alertType().apply {
      deactivatedAt = LocalDateTime.now().minusDays(1)
      deactivatedBy = "DEACTIVATED_BY"
    }
}
