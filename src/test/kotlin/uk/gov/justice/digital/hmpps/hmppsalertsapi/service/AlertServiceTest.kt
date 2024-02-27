package uk.gov.justice.digital.hmpps.hmppsalertsapi.service

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchers.anyString
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.hmppsalertsapi.client.prisonersearch.PrisonerSearchClient
import uk.gov.justice.digital.hmpps.hmppsalertsapi.client.prisonersearch.dto.PrisonerDto
import uk.gov.justice.digital.hmpps.hmppsalertsapi.config.AlertNotFoundException
import uk.gov.justice.digital.hmpps.hmppsalertsapi.config.AlertRequestContext
import uk.gov.justice.digital.hmpps.hmppsalertsapi.config.ExistingActiveAlertWithCodeException
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
import uk.gov.justice.digital.hmpps.hmppsalertsapi.model.request.UpdateAlert
import uk.gov.justice.digital.hmpps.hmppsalertsapi.repository.AlertCodeRepository
import uk.gov.justice.digital.hmpps.hmppsalertsapi.repository.AlertRepository
import uk.gov.justice.digital.hmpps.hmppsalertsapi.utils.ALERT_CODE_VICTIM
import uk.gov.justice.digital.hmpps.hmppsalertsapi.utils.alertCodeVictim
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

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
  fun `Alert code not found`() {
    whenever(alertCodeRepository.findByCode(anyString())).thenReturn(null)
    val error = assertThrows<IllegalArgumentException> {
      underTest.createAlert(createAlertRequest(alertCode = "A"), requestContext)
    }
    assertThat(error.message).isEqualTo("Alert code 'A' not found")
  }

  @Test
  fun `Alert code not active`() {
    whenever(mockAlertCode.isActive()).thenReturn(false)
    whenever(alertCodeRepository.findByCode(anyString())).thenReturn(mockAlertCode)
    val error = assertThrows<IllegalArgumentException> {
      underTest.createAlert(createAlertRequest(alertCode = "A"), requestContext)
    }
    assertThat(error.message).isEqualTo("Alert code 'A' is inactive")
  }

  @Test
  fun `Existing active alert with code`() {
    whenever(alertCodeRepository.findByCode(anyString())).thenReturn(alertCodeVictim())
    whenever(alertRepository.findByPrisonNumberAndAlertCodeCode(anyString(), anyString()))
      .thenReturn(listOf(alertEntity(activeFrom = LocalDate.now(), activeTo = null)))
    val error = assertThrows<ExistingActiveAlertWithCodeException> {
      underTest.createAlert(createAlertRequest(), requestContext)
    }
    assertThat(error.message).isEqualTo("Active alert with code '$ALERT_CODE_VICTIM' already exists for prison number '$PRISON_NUMBER'")
  }

  @Test
  fun `Existing alert with code that will become active`() {
    whenever(alertCodeRepository.findByCode(anyString())).thenReturn(alertCodeVictim())
    whenever(alertRepository.findByPrisonNumberAndAlertCodeCode(anyString(), anyString()))
      .thenReturn(listOf(alertEntity(activeFrom = LocalDate.now().plusDays(1), activeTo = null)))
    val error = assertThrows<ExistingActiveAlertWithCodeException> {
      underTest.createAlert(createAlertRequest(), requestContext)
    }
    assertThat(error.message).isEqualTo("Active alert with code '$ALERT_CODE_VICTIM' already exists for prison number '$PRISON_NUMBER'")
  }

  @Test
  fun `Existing inactive alert with code`() {
    whenever(alertCodeRepository.findByCode(anyString())).thenReturn(alertCodeVictim())
    whenever(alertRepository.findByPrisonNumberAndAlertCodeCode(anyString(), anyString()))
      .thenReturn(listOf(alertEntity(activeFrom = LocalDate.now().minusDays(1), activeTo = LocalDate.now())))
    whenever(prisonerSearchClient.getPrisoner(anyString())).thenReturn(prisoner())
    whenever(alertRepository.saveAndFlush(any())).thenAnswer { it.arguments[0] }
    underTest.createAlert(createAlertRequest(), requestContext)
    verify(alertRepository).saveAndFlush(any<Alert>())
  }

  @Test
  fun `Prisoner not found`() {
    whenever(alertCodeRepository.findByCode(anyString())).thenReturn(alertCodeVictim())
    whenever(prisonerSearchClient.getPrisoner(anyString())).thenReturn(null)
    val error = assertThrows<IllegalArgumentException> {
      underTest.createAlert(createAlertRequest(), requestContext)
    }
    assertThat(error.message).isEqualTo("Prison number '${PRISON_NUMBER}' not found")
  }

  @Test
  fun `uses alert code from request`() {
    whenever(alertCodeRepository.findByCode(anyString())).thenReturn(alertCodeVictim())
    whenever(prisonerSearchClient.getPrisoner(anyString())).thenReturn(prisoner())
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
    whenever(alertCodeRepository.findByCode(anyString())).thenReturn(alertCodeVictim())
    whenever(prisonerSearchClient.getPrisoner(anyString())).thenReturn(prisoner())
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
    whenever(alertCodeRepository.findByCode(anyString())).thenReturn(alertCodeVictim())
    whenever(prisonerSearchClient.getPrisoner(anyString())).thenReturn(prisoner())
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
    whenever(alertCodeRepository.findByCode(anyString())).thenReturn(alertCodeVictim())
    whenever(prisonerSearchClient.getPrisoner(anyString())).thenReturn(prisoner())
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
    whenever(alertCodeRepository.findByCode(anyString())).thenReturn(alertCodeVictim())
    whenever(prisonerSearchClient.getPrisoner(anyString())).thenReturn(prisoner())
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
    whenever(alertCodeRepository.findByCode(anyString())).thenReturn(alertCodeVictim())
    whenever(prisonerSearchClient.getPrisoner(anyString())).thenReturn(prisoner())
    val alertCaptor = argumentCaptor<Alert>()
    whenever(alertRepository.saveAndFlush(alertCaptor.capture())).thenAnswer { alertCaptor.firstValue }
    val request = createAlertRequest()
    val result = underTest.createAlert(request, requestContext)
    assertThat(result).isEqualTo(alertCaptor.firstValue.toAlertModel())
  }

  @Test
  fun `should throw if cannot find alert to update`() {
    whenever(alertRepository.findByAlertUuid(any(UUID::class.java))).thenReturn(null)
    val request = updateAlertRequest()
    assertThrows<AlertNotFoundException> {
      underTest.updateAlert(UUID.randomUUID(), request, requestContext)
    }
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

  private fun updateAlertRequest() =
    UpdateAlert(
      description = "new description",
      authorisedBy = "B Bauthorizer",
      activeFrom = LocalDate.now().minusDays(2),
      activeTo = LocalDate.now().plusDays(10),
      appendComment = "Update alert",
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

  private fun alertEntity(
    activeFrom: LocalDate = LocalDate.now(),
    activeTo: LocalDate? = null,
  ) =
    Alert(
      alertUuid = UUID.randomUUID(),
      alertCode = alertCodeVictim(),
      prisonNumber = PRISON_NUMBER,
      description = "Alert description",
      authorisedBy = "A. Authorizer",
      activeFrom = activeFrom,
      activeTo = activeTo,
    ).apply {
      auditEvent(
        action = AuditEventAction.CREATED,
        description = "Alert created",
        actionedAt = LocalDateTime.now(),
        actionedBy = "CREATED_BY",
        actionedByDisplayName = "CREATED_BY_DISPLAY_NAME",
      )
    }
}
