package uk.gov.justice.digital.hmpps.hmppsalertsapi.service

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.ArgumentMatchers.anyString
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
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
import uk.gov.justice.digital.hmpps.hmppsalertsapi.integration.wiremock.TEST_USER
import uk.gov.justice.digital.hmpps.hmppsalertsapi.integration.wiremock.TEST_USER_NAME
import uk.gov.justice.digital.hmpps.hmppsalertsapi.model.request.CreateAlert
import uk.gov.justice.digital.hmpps.hmppsalertsapi.repository.AlertCodeRepository
import uk.gov.justice.digital.hmpps.hmppsalertsapi.repository.AlertRepository
import uk.gov.justice.digital.hmpps.hmppsalertsapi.utils.ALERT_CODE_INACTIVE_COVID_REFUSING_TO_SHIELD
import uk.gov.justice.digital.hmpps.hmppsalertsapi.utils.ALERT_CODE_VICTIM
import uk.gov.justice.digital.hmpps.hmppsalertsapi.utils.alertCodeRefusingToShieldInactive
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

  private val alertCaptor = argumentCaptor<Alert>()

  @BeforeEach
  fun beforeEach() {
    whenever(alertCodeRepository.findByCode(ALERT_CODE_VICTIM)).thenReturn(alertCodeVictim())
    whenever(alertRepository.saveAndFlush(alertCaptor.capture())).thenAnswer { alertCaptor.firstValue }
  }

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

  @Test
  fun `throws IllegalArgumentException when alert code is not found`() {
    val request = createAlertRequest(alertCode = "NOT_FOUND")
    val exception = assertThrows<IllegalArgumentException> { service.createAlert(request, requestContext) }
    assertThat(exception.message).isEqualTo("Alert code 'NOT_FOUND' not found")
  }

  @Test
  fun `throws IllegalArgumentException when alert code is inactive`() {
    whenever(alertCodeRepository.findByCode(ALERT_CODE_INACTIVE_COVID_REFUSING_TO_SHIELD)).thenReturn(alertCodeRefusingToShieldInactive())
    val request = createAlertRequest(alertCode = ALERT_CODE_INACTIVE_COVID_REFUSING_TO_SHIELD)
    val exception = assertThrows<IllegalArgumentException> { service.createAlert(request, requestContext) }
    assertThat(exception.message).isEqualTo("Alert code '${ALERT_CODE_INACTIVE_COVID_REFUSING_TO_SHIELD}' is inactive")
  }

  @Test
  fun `uses alert code from request`() {
    val request = createAlertRequest()
    service.createAlert(request, requestContext)
    with(alertCaptor.firstValue.alertCode) {
      assertThat(code).isEqualTo(request.alertCode)
      assertThat(this).isEqualTo(alertCodeVictim())
    }
  }

  @Test
  fun `returns alert code from request`() {
    val request = createAlertRequest()
    val result = service.createAlert(request, requestContext)
    with(result.alertCode) {
      assertThat(code).isEqualTo(request.alertCode)
      assertThat(this).isEqualTo(alertCodeVictim().toAlertCodeSummary())
    }
  }

  @Test
  fun `populates audit event from request context`() {
    val request = createAlertRequest()
    service.createAlert(request, requestContext)
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
    val request = createAlertRequest()
    val result = service.createAlert(request, requestContext)
    with(result) {
      assertThat(createdAt).isEqualTo(requestContext.requestAt)
      assertThat(createdBy).isEqualTo(requestContext.username)
      assertThat(createdByDisplayName).isEqualTo(requestContext.userDisplayName)
    }
  }

  @Test
  fun `converts request using toAlertEntity`() {
    val request = createAlertRequest()
    val result = service.createAlert(request, requestContext)
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
    val request = createAlertRequest()
    val result = service.createAlert(request, requestContext)
    assertThat(result).isEqualTo(alertCaptor.firstValue.toAlertModel())
  }

  private fun createAlertRequest(
    prisonNumber: String = "A1234AA",
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
      "123AA12",
      123,
      "prisoner",
      "middle",
      "lastName",
      LocalDate.of(1988, 3, 4),
    )
}
