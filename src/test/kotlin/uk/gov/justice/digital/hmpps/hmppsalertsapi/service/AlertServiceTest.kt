package uk.gov.justice.digital.hmpps.hmppsalertsapi.service

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.ArgumentMatchers.anyString
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.hmppsalertsapi.client.prisonersearch.PrisonerSearchClient
import uk.gov.justice.digital.hmpps.hmppsalertsapi.client.prisonersearch.dto.PrisonerDto
import uk.gov.justice.digital.hmpps.hmppsalertsapi.config.AlertRequestContext
import uk.gov.justice.digital.hmpps.hmppsalertsapi.domain.toAlertCodeSummary
import uk.gov.justice.digital.hmpps.hmppsalertsapi.domain.toAlertEntity
import uk.gov.justice.digital.hmpps.hmppsalertsapi.domain.toAlertModel
import uk.gov.justice.digital.hmpps.hmppsalertsapi.entity.Alert
import uk.gov.justice.digital.hmpps.hmppsalertsapi.entity.AlertCode
import uk.gov.justice.digital.hmpps.hmppsalertsapi.enumeration.AuditEventAction
import uk.gov.justice.digital.hmpps.hmppsalertsapi.integration.wiremock.PRISON_NUMBER
import uk.gov.justice.digital.hmpps.hmppsalertsapi.integration.wiremock.TEST_USER
import uk.gov.justice.digital.hmpps.hmppsalertsapi.integration.wiremock.TEST_USER_NAME
import uk.gov.justice.digital.hmpps.hmppsalertsapi.model.request.CreateAlert
import uk.gov.justice.digital.hmpps.hmppsalertsapi.repository.AlertCodeRepository
import uk.gov.justice.digital.hmpps.hmppsalertsapi.repository.AlertRepository
import uk.gov.justice.digital.hmpps.hmppsalertsapi.utils.ALERT_CODE_VICTIM
import uk.gov.justice.digital.hmpps.hmppsalertsapi.utils.alertCodeVictim
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

  private val requestContext = AlertRequestContext(
    username = TEST_USER,
    userDisplayName = TEST_USER_NAME,
  )

  @Test
  fun `Prisoner not found`() {
    whenever(prisonerSearchClient.getPrisoner(anyString())).thenReturn(null)
    val error = assertThrows<IllegalArgumentException> {
      underTest.createAlert(createAlertRequest(), requestContext)
    }
    assertThat(error.message).isEqualTo("Prison number '${PRISON_NUMBER}' not found")
  }

  @Test
  fun `Alert code not found`() {
    whenever(prisonerSearchClient.getPrisoner(anyString())).thenReturn(prisoner())
    whenever(alertCodeRepository.findByCode(anyString())).thenReturn(null)
    val error = assertThrows<IllegalArgumentException> {
      underTest.createAlert(createAlertRequest(alertCode = "A"), requestContext)
    }
    assertThat(error.message).isEqualTo("Alert code 'A' not found")
  }

  @Test
  fun `Alert code not active`() {
    whenever(prisonerSearchClient.getPrisoner(anyString())).thenReturn(prisoner())
    whenever(mockAlertCode.isActive()).thenReturn(false)
    whenever(alertCodeRepository.findByCode(anyString())).thenReturn(mockAlertCode)
    val error = assertThrows<IllegalArgumentException> {
      underTest.createAlert(createAlertRequest(alertCode = "A"), requestContext)
    }
    assertThat(error.message).isEqualTo("Alert code 'A' is inactive")
  }

  @Test
  fun `uses alert code from request`() {
    whenever(prisonerSearchClient.getPrisoner(anyString())).thenReturn(prisoner())
    whenever(alertCodeRepository.findByCode(anyString())).thenReturn(alertCodeVictim())
    val alertCaptor = argumentCaptor<Alert>()
    whenever(alertRepository.saveAndFlush(alertCaptor.capture())).thenAnswer { alertCaptor.firstValue }
    val request = createAlertRequest()
    underTest.createAlert(request, requestContext)
    with(alertCaptor.firstValue.alertCode) {
      assertThat(code).isEqualTo(request.alertCode)
      assertThat(this).isEqualTo(alertCodeVictim())
    }
  }

  @Test
  fun `returns alert code from request`() {
    whenever(prisonerSearchClient.getPrisoner(anyString())).thenReturn(prisoner())
    whenever(alertCodeRepository.findByCode(anyString())).thenReturn(alertCodeVictim())
    whenever(alertRepository.saveAndFlush(any())).thenAnswer { it.arguments[0] }
    val request = createAlertRequest()
    val result = underTest.createAlert(request, requestContext)
    with(result.alertCode) {
      assertThat(code).isEqualTo(request.alertCode)
      assertThat(this).isEqualTo(alertCodeVictim().toAlertCodeSummary())
    }
  }

  @Test
  fun `populates audit event from request context`() {
    whenever(prisonerSearchClient.getPrisoner(anyString())).thenReturn(prisoner())
    whenever(alertCodeRepository.findByCode(anyString())).thenReturn(alertCodeVictim())
    val alertCaptor = argumentCaptor<Alert>()
    whenever(alertRepository.saveAndFlush(alertCaptor.capture())).thenAnswer { alertCaptor.firstValue }
    val request = createAlertRequest()
    underTest.createAlert(request, requestContext)
    with(alertCaptor.firstValue.auditEvents().single()) {
      assertThat(action).isEqualTo(AuditEventAction.CREATED)
      assertThat(description).isEqualTo("Alert created")
      assertThat(actionedAt).isEqualTo(requestContext.requestAt)
      assertThat(actionedBy).isEqualTo(requestContext.username)
      assertThat(actionedByDisplayName).isEqualTo(requestContext.userDisplayName)
    }
  }

  @Test
  fun `returns properties from request context`() {
    whenever(prisonerSearchClient.getPrisoner(anyString())).thenReturn(prisoner())
    whenever(alertCodeRepository.findByCode(anyString())).thenReturn(alertCodeVictim())
    whenever(alertRepository.saveAndFlush(any())).thenAnswer { it.arguments[0] }
    val request = createAlertRequest()
    val result = underTest.createAlert(request, requestContext)
    with(result) {
      assertThat(createdAt).isEqualTo(requestContext.requestAt)
      assertThat(createdBy).isEqualTo(requestContext.username)
      assertThat(createdByDisplayName).isEqualTo(requestContext.userDisplayName)
    }
  }

  @Test
  fun `converts request using toAlertEntity`() {
    whenever(prisonerSearchClient.getPrisoner(anyString())).thenReturn(prisoner())
    whenever(alertCodeRepository.findByCode(anyString())).thenReturn(alertCodeVictim())
    val alertCaptor = argumentCaptor<Alert>()
    whenever(alertRepository.saveAndFlush(alertCaptor.capture())).thenAnswer { alertCaptor.firstValue }
    val request = createAlertRequest()
    val result = underTest.createAlert(request, requestContext)
    assertThat(alertCaptor.firstValue).isEqualTo(
      request.toAlertEntity(
        alertCode = alertCodeVictim(),
        createdAt = requestContext.requestAt,
        createdBy = requestContext.username,
        createdByDisplayName = requestContext.userDisplayName,
      ).copy(alertUuid = result.alertUuid),
    )
  }

  @Test
  fun `converts alert entity to model`() {
    whenever(prisonerSearchClient.getPrisoner(anyString())).thenReturn(prisoner())
    whenever(alertCodeRepository.findByCode(anyString())).thenReturn(alertCodeVictim())
    val alertCaptor = argumentCaptor<Alert>()
    whenever(alertRepository.saveAndFlush(alertCaptor.capture())).thenAnswer { alertCaptor.firstValue }
    val request = createAlertRequest()
    val result = underTest.createAlert(request, requestContext)
    assertThat(result).isEqualTo(alertCaptor.firstValue.toAlertModel())
  }

  private fun createAlertRequest(
    prisonNumber: String = PRISON_NUMBER,
    alertCode: String = ALERT_CODE_VICTIM,
  ) =
    CreateAlert(
      prisonNumber = prisonNumber,
      alertCode = alertCode,
      description = "Alert description",
      authorisedBy = "A. Authorizer",
      activeFrom = LocalDate.now().minusDays(3),
      activeTo = null,
    )

  private fun prisoner() =
    PrisonerDto(
      PRISON_NUMBER,
      123,
      "prisoner",
      "middle",
      "lastName",
      LocalDate.of(1988, 3, 4),
    )
}
