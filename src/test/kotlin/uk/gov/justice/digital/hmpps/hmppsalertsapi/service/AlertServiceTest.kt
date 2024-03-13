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
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
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

  private val context = AlertRequestContext(
    username = TEST_USER,
    userDisplayName = TEST_USER_NAME,
  )

  @Test
  fun `Alert code not found`() {
    whenever(alertCodeRepository.findByCode(anyString())).thenReturn(null)
    val error = assertThrows<IllegalArgumentException> {
      underTest.createAlert(createAlertRequest(alertCode = "A"), context)
    }
    assertThat(error.message).isEqualTo("Alert code 'A' not found")
  }

  @Test
  fun `Alert code not active`() {
    whenever(mockAlertCode.isActive()).thenReturn(false)
    whenever(alertCodeRepository.findByCode(anyString())).thenReturn(mockAlertCode)
    val error = assertThrows<IllegalArgumentException> {
      underTest.createAlert(createAlertRequest(alertCode = "A"), context)
    }
    assertThat(error.message).isEqualTo("Alert code 'A' is inactive")
  }

  @Test
  fun `Existing active alert with code`() {
    whenever(alertCodeRepository.findByCode(anyString())).thenReturn(alertCodeVictim())
    whenever(alertRepository.findByPrisonNumberAndAlertCodeCode(anyString(), anyString()))
      .thenReturn(listOf(alertEntity(activeFrom = LocalDate.now(), activeTo = null)))
    val error = assertThrows<ExistingActiveAlertWithCodeException> {
      underTest.createAlert(createAlertRequest(), context)
    }
    assertThat(error.message).isEqualTo("Active alert with code '$ALERT_CODE_VICTIM' already exists for prison number '$PRISON_NUMBER'")
  }

  @Test
  fun `Existing alert with code that will become active`() {
    whenever(alertCodeRepository.findByCode(anyString())).thenReturn(alertCodeVictim())
    whenever(alertRepository.findByPrisonNumberAndAlertCodeCode(anyString(), anyString()))
      .thenReturn(listOf(alertEntity(activeFrom = LocalDate.now().plusDays(1), activeTo = null)))
    val error = assertThrows<ExistingActiveAlertWithCodeException> {
      underTest.createAlert(createAlertRequest(), context)
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
    underTest.createAlert(createAlertRequest(), context)
    verify(alertRepository).saveAndFlush(any<Alert>())
  }

  @Test
  fun `Prisoner not found`() {
    whenever(alertCodeRepository.findByCode(anyString())).thenReturn(alertCodeVictim())
    whenever(prisonerSearchClient.getPrisoner(anyString())).thenReturn(null)
    val error = assertThrows<IllegalArgumentException> {
      underTest.createAlert(createAlertRequest(), context)
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
    underTest.createAlert(request, context)
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
    val result = underTest.createAlert(request, context)
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
    underTest.createAlert(request, context)
    with(alertCaptor.firstValue.auditEvents().single()) {
      assertThat(action).isEqualTo(AuditEventAction.CREATED)
      assertThat(description).isEqualTo("Alert created")
      assertThat(actionedAt).isEqualTo(context.requestAt)
      assertThat(actionedBy).isEqualTo(context.username)
      assertThat(actionedByDisplayName).isEqualTo(context.userDisplayName)
    }
  }

  @Test
  fun `returns properties from request context`() {
    whenever(alertCodeRepository.findByCode(anyString())).thenReturn(alertCodeVictim())
    whenever(prisonerSearchClient.getPrisoner(anyString())).thenReturn(prisoner())
    whenever(alertRepository.saveAndFlush(any())).thenAnswer { it.arguments[0] }
    val request = createAlertRequest()
    val result = underTest.createAlert(request, context)
    with(result) {
      assertThat(createdAt).isEqualTo(context.requestAt)
      assertThat(createdBy).isEqualTo(context.username)
      assertThat(createdByDisplayName).isEqualTo(context.userDisplayName)
    }
  }

  @Test
  fun `converts request using toAlertEntity`() {
    whenever(alertCodeRepository.findByCode(anyString())).thenReturn(alertCodeVictim())
    whenever(prisonerSearchClient.getPrisoner(anyString())).thenReturn(prisoner())
    val alertCaptor = argumentCaptor<Alert>()
    whenever(alertRepository.saveAndFlush(alertCaptor.capture())).thenAnswer { alertCaptor.firstValue }
    val request = createAlertRequest()
    val result = underTest.createAlert(request, context)
    assertThat(alertCaptor.firstValue).isEqualTo(
      request.toAlertEntity(
        alertCode = alertCodeVictim(),
        createdAt = context.requestAt,
        createdBy = context.username,
        createdByDisplayName = context.userDisplayName,
        source = context.source,
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
    val result = underTest.createAlert(request, context)
    assertThat(result).isEqualTo(alertCaptor.firstValue.toAlertModel())
  }

  @Test
  fun `should throw if cannot find alert to update`() {
    whenever(alertRepository.findByAlertUuid(any())).thenReturn(null)
    val request = updateAlertRequestNoChange()
    val alertUuid = UUID.randomUUID()
    val exception = assertThrows<AlertNotFoundException> {
      underTest.updateAlert(alertUuid, request, context)
    }
    assertThat(exception.message).isEqualTo("Could not find alert with ID $alertUuid")
  }

  @Test
  fun `no audit event if nothing has changed`() {
    val uuid = UUID.randomUUID()
    val updateRequest = updateAlertRequestNoChange(comment = "")
    val alert = alert(updateAlert = updateRequest, uuid = uuid)
    whenever(alertRepository.findByAlertUuid(any())).thenReturn(alert)
    val alertCaptor = argumentCaptor<Alert>()
    whenever(alertRepository.saveAndFlush(alertCaptor.capture())).thenAnswer { alertCaptor.firstValue }

    underTest.updateAlert(uuid, updateRequest, context)
    val savedAlert = alertCaptor.firstValue
    assertThat(savedAlert.auditEvents()).hasSize(1)
    assertThat(savedAlert.auditEvents().first().action).isEqualTo(AuditEventAction.CREATED)
  }

  @Test
  fun `null activeFrom will not update value`() {
    val uuid = UUID.randomUUID()
    val updateRequest = updateAlertRequestNoChange(comment = "", activeFrom = null)
    val alert = alert(uuid = uuid)
    whenever(alertRepository.findByAlertUuid(any())).thenReturn(alert)
    val alertCaptor = argumentCaptor<Alert>()
    whenever(alertRepository.saveAndFlush(alertCaptor.capture())).thenAnswer { alertCaptor.firstValue }

    underTest.updateAlert(uuid, updateRequest, context)
    val savedAlert = alertCaptor.firstValue
    with(savedAlert) {
      assertThat(activeFrom).isEqualTo(alert.activeFrom)
    }
  }

  @Test
  fun `change written successfully`() {
    val uuid = UUID.randomUUID()
    val updateRequest = updateAlertRequestChange()
    val alert = alert(uuid = uuid)
    val unchangedAlert = alert(uuid = uuid)
    whenever(alertRepository.findByAlertUuid(any())).thenReturn(alert)
    val alertCaptor = argumentCaptor<Alert>()
    whenever(alertRepository.saveAndFlush(alertCaptor.capture())).thenAnswer { alertCaptor.firstValue }
    underTest.updateAlert(uuid, updateRequest, context)
    val savedAlert = alertCaptor.firstValue
    assertThat(savedAlert.activeTo).isEqualTo(updateRequest.activeTo)
    assertThat(savedAlert.activeFrom).isEqualTo(updateRequest.activeFrom)
    assertThat(savedAlert.activeTo).isEqualTo(updateRequest.activeTo)
    assertThat(savedAlert.description).isEqualTo(updateRequest.description)
    assertThat(savedAlert.authorisedBy).isEqualTo(updateRequest.authorisedBy)
    assertThat(savedAlert.comments()).hasSize(1)
    assertThat(savedAlert.comments()[0].comment).isEqualTo("Another update alert")
    assertThat(savedAlert.auditEvents()).hasSize(2)
    assertThat(savedAlert.auditEvents()[0].action).isEqualTo(AuditEventAction.UPDATED)
    assertThat(savedAlert.auditEvents()[0].description).isEqualTo(
      """Updated alert description from '${unchangedAlert.description}' to '${savedAlert.description}'
Updated authorised by from '${unchangedAlert.authorisedBy}' to '${savedAlert.authorisedBy}'
Updated active from from '${unchangedAlert.activeFrom}' to '${savedAlert.activeFrom}'
Updated active to from '${unchangedAlert.activeTo}' to '${savedAlert.activeTo}'
Comment '${updateRequest.appendComment}' was added
""",
    )
  }

  @Test
  fun `alert uuid not found`() {
    whenever(alertRepository.findByAlertUuid(any())).thenReturn(null)
    val alertUuid = UUID.randomUUID()
    val exception = assertThrows<AlertNotFoundException> {
      underTest.retrieveAlert(alertUuid)
    }
    assertThat(exception.message).isEqualTo("Could not find alert with uuid $alertUuid")
  }

  @Test
  fun `returns alert model if found`() {
    val alert = alert()
    whenever(alertRepository.findByAlertUuid(any())).thenReturn(alert)
    val result = underTest.retrieveAlert(UUID.randomUUID())
    assertThat(result).isEqualTo(alert.toAlertModel())
  }

  @Test
  fun `delete alert uuid not found throws exception`() {
    whenever(alertRepository.findByAlertUuid(any())).thenReturn(null)
    val alertUuid = UUID.randomUUID()
    val exception = assertThrows<AlertNotFoundException> {
      underTest.deleteAlert(alertUuid, context)
    }
    assertThat(exception.message).isEqualTo("Could not find alert with uuid $alertUuid")
  }

  @Test
  fun `delete alert should add audit event`() {
    val uuid = UUID.randomUUID()
    val alert = alert(uuid = uuid)
    whenever(alertRepository.findByAlertUuid(any())).thenReturn(alert)
    val alertCaptor = argumentCaptor<Alert>()
    whenever(alertRepository.saveAndFlush(alertCaptor.capture())).thenAnswer { alertCaptor.firstValue }
    underTest.deleteAlert(uuid, context)

    val savedAlert = alertCaptor.firstValue
    assertThat(savedAlert.deletedAt()).isEqualTo(context.requestAt)
    assertThat(savedAlert.auditEvents()).hasSize(2)
    assertThat(savedAlert.auditEvents()[0].action).isEqualTo(AuditEventAction.DELETED)
    assertThat(savedAlert.auditEvents()[0].description).isEqualTo("Alert deleted")
  }

  @Test
  fun `retrieve all alerts`() {
    val alert = alert()
    whenever(alertRepository.findAllByPrisonNumber(any(), any())).thenReturn(PageImpl(listOf(alert)))
    val result = underTest.retrieveAlertsForPrisonNumber("ABC123AA", PageRequest.of(0, 10))
    assertThat(result).containsExactly(alert.toAlertModel())
  }

  @Test
  fun `should throw exception if alert not found when retrieving audit events`() {
    whenever(alertRepository.findByAlertUuid(any())).thenReturn(null)
    val alertUuid = UUID.randomUUID()
    val exception = assertThrows<AlertNotFoundException> {
      underTest.retrieveAuditEventsForAlert(alertUuid)
    }
    assertThat(exception.message).isEqualTo("Could not find alert with uuid $alertUuid")
  }

  @Test
  fun `should return models if exist`() {
    val alertEntity = alertEntity()
    whenever(alertRepository.findByAlertUuid(any())).thenReturn(alertEntity)
    val alertUuid = UUID.randomUUID()
    val result = underTest.retrieveAuditEventsForAlert(alertUuid)
    assertThat(result).isNotEmpty()
    with(result.single()) {
      assertThat(action).isEqualTo(alertEntity.auditEvents().single().action)
      assertThat(description).isEqualTo(alertEntity.auditEvents().single().description)
      assertThat(actionedAt).isEqualTo(alertEntity.auditEvents().single().actionedAt)
      assertThat(actionedBy).isEqualTo(alertEntity.auditEvents().single().actionedBy)
      assertThat(actionedByDisplayName).isEqualTo(alertEntity.auditEvents().single().actionedByDisplayName)
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

  private fun updateAlertRequestNoChange(comment: String = "Update alert", activeFrom: LocalDate? = LocalDate.now().minusDays(2)) =
    UpdateAlert(
      description = "new description",
      authorisedBy = "B Bauthorizer",
      activeFrom = activeFrom,
      activeTo = LocalDate.now().plusDays(10),
      appendComment = comment,
    )

  private fun updateAlertRequestChange(comment: String = "Another update alert") =
    UpdateAlert(
      description = "another new description",
      authorisedBy = "C Cauthorizer",
      activeFrom = LocalDate.now().minusMonths(2),
      activeTo = LocalDate.now().plusMonths(10),
      appendComment = comment,
    )

  private fun alert(uuid: UUID = UUID.randomUUID(), updateAlert: UpdateAlert = updateAlertRequestNoChange()): Alert {
    return Alert(
      alertId = 1,
      alertUuid = uuid,
      alertCode = alertCodeVictim(),
      prisonNumber = PRISON_NUMBER,
      description = "new description",
      authorisedBy = "B Bauthorizer",
      activeTo = updateAlert.activeTo,
      activeFrom = updateAlert.activeFrom!!,

    ).apply { auditEvent(AuditEventAction.CREATED, "Created", LocalDateTime.now().minusDays(1), "Test", "Test") }
  }

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
