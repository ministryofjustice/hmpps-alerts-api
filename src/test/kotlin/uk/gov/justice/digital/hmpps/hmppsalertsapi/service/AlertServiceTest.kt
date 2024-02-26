package uk.gov.justice.digital.hmpps.hmppsalertsapi.service

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.ArgumentMatchers.anyString
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.hmppsalertsapi.client.prisonersearch.PrisonerSearchClient
import uk.gov.justice.digital.hmpps.hmppsalertsapi.client.prisonersearch.dto.PrisonerDto
import uk.gov.justice.digital.hmpps.hmppsalertsapi.config.AlertRequestContext
import uk.gov.justice.digital.hmpps.hmppsalertsapi.entity.AlertCode
import uk.gov.justice.digital.hmpps.hmppsalertsapi.model.request.CreateAlert
import uk.gov.justice.digital.hmpps.hmppsalertsapi.repository.AlertCodeRepository
import uk.gov.justice.digital.hmpps.hmppsalertsapi.repository.AlertRepository
import java.time.LocalDate

@ExtendWith(MockitoExtension::class)
class AlertServiceTest {

  @Mock
  lateinit var alertCodeRepository: AlertCodeRepository

  @Mock
  lateinit var alertRepository: AlertRepository

  @Mock
  lateinit var mockAlertCode: AlertCode

  @Mock
  lateinit var prisonerSearchClient: PrisonerSearchClient

  @InjectMocks
  lateinit var underTest: AlertService

  @Test
  fun `Alert code not found`() {
    whenever(prisonerSearchClient.getPrisoner(anyString())).thenReturn(prisoner())
    whenever(alertCodeRepository.findByCode(anyString())).thenReturn(null)
    val error = assertThrows<IllegalArgumentException> {
      underTest.createAlert(CreateAlert("123AA12", "A", "Description", "A. Authoriser", null, null), AlertRequestContext(username = "username", userDisplayName = "A. User"))
    }
    assertThat(error.message).isEqualTo("Alert code not found: A")
  }

  @Test
  fun `Alert code not active`() {
    whenever(prisonerSearchClient.getPrisoner(anyString())).thenReturn(prisoner())
    whenever(mockAlertCode.isActive()).thenReturn(false)
    whenever(alertCodeRepository.findByCode(anyString())).thenReturn(mockAlertCode)
    val error = assertThrows<IllegalArgumentException> {
      underTest.createAlert(CreateAlert("123AA12", "A", "Description", "A. Authoriser", null, null), AlertRequestContext(username = "username", userDisplayName = "A. User"))
    }
    assertThat(error.message).isEqualTo("Alert code is not active: A")
  }

  @Test
  fun `Prisoner not found`() {
    whenever(prisonerSearchClient.getPrisoner(anyString())).thenReturn(null)
    val error = assertThrows<IllegalArgumentException> {
      underTest.createAlert(CreateAlert("123AA12", "A", "Description", "A. Authoriser", null, null), AlertRequestContext(username = "username", userDisplayName = "A. User"))
    }
    assertThat(error.message).isEqualTo("Prisoner not found for prison number: 123AA12")
  }

  private fun prisoner() =
    PrisonerDto(
      "123AA12",
      123,
      "prisoner",
      "middle",
      "lastName",
      LocalDate.of(1988, 3, 4),
    )
}
