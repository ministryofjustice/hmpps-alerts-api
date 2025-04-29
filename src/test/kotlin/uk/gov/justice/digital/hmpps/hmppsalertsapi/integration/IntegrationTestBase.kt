package uk.gov.justice.digital.hmpps.hmppsalertsapi.integration

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.test.web.reactive.server.expectBody
import org.springframework.transaction.support.TransactionTemplate
import software.amazon.awssdk.services.sqs.model.PurgeQueueRequest
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest
import uk.gov.justice.digital.hmpps.hmppsalertsapi.IdGenerator.newUuid
import uk.gov.justice.digital.hmpps.hmppsalertsapi.entity.Alert
import uk.gov.justice.digital.hmpps.hmppsalertsapi.entity.AlertCode
import uk.gov.justice.digital.hmpps.hmppsalertsapi.entity.AlertType
import uk.gov.justice.digital.hmpps.hmppsalertsapi.entity.BulkPlan
import uk.gov.justice.digital.hmpps.hmppsalertsapi.entity.BulkPlanRepository
import uk.gov.justice.digital.hmpps.hmppsalertsapi.entity.PersonSummary
import uk.gov.justice.digital.hmpps.hmppsalertsapi.entity.PersonSummaryRepository
import uk.gov.justice.digital.hmpps.hmppsalertsapi.entity.event.AlertBaseAdditionalInformation
import uk.gov.justice.digital.hmpps.hmppsalertsapi.entity.event.AlertDomainEvent
import uk.gov.justice.digital.hmpps.hmppsalertsapi.entity.event.HmppsDomainEvent
import uk.gov.justice.digital.hmpps.hmppsalertsapi.enumeration.BulkAlertCleanupMode
import uk.gov.justice.digital.hmpps.hmppsalertsapi.enumeration.Source
import uk.gov.justice.digital.hmpps.hmppsalertsapi.exceptions.InvalidRowResponse
import uk.gov.justice.digital.hmpps.hmppsalertsapi.integration.container.LocalStackContainer
import uk.gov.justice.digital.hmpps.hmppsalertsapi.integration.container.LocalStackContainer.setLocalStackProperties
import uk.gov.justice.digital.hmpps.hmppsalertsapi.integration.container.PostgresContainer
import uk.gov.justice.digital.hmpps.hmppsalertsapi.integration.wiremock.HmppsAuthApiExtension
import uk.gov.justice.digital.hmpps.hmppsalertsapi.integration.wiremock.ManageUsersExtension
import uk.gov.justice.digital.hmpps.hmppsalertsapi.integration.wiremock.PRISON_CODE_LEEDS
import uk.gov.justice.digital.hmpps.hmppsalertsapi.integration.wiremock.PrisonerSearchExtension
import uk.gov.justice.digital.hmpps.hmppsalertsapi.integration.wiremock.PrisonerSearchExtension.Companion.prisonerSearch
import uk.gov.justice.digital.hmpps.hmppsalertsapi.integration.wiremock.TEST_USER
import uk.gov.justice.digital.hmpps.hmppsalertsapi.integration.wiremock.TEST_USER_NAME
import uk.gov.justice.digital.hmpps.hmppsalertsapi.repository.AlertCodeRepository
import uk.gov.justice.digital.hmpps.hmppsalertsapi.repository.AlertRepository
import uk.gov.justice.digital.hmpps.hmppsalertsapi.repository.AlertTypeRepository
import uk.gov.justice.digital.hmpps.hmppsalertsapi.resource.SOURCE
import uk.gov.justice.digital.hmpps.hmppsalertsapi.resource.USERNAME
import uk.gov.justice.digital.hmpps.hmppsalertsapi.utils.EntityGenerator.AC_VICTIM
import uk.gov.justice.digital.hmpps.hmppsalertsapi.utils.IdGenerator.cellLocation
import uk.gov.justice.digital.hmpps.hmppsalertsapi.utils.IdGenerator.prisonNumber
import uk.gov.justice.digital.hmpps.hmppsalertsapi.utils.set
import uk.gov.justice.hmpps.kotlin.common.ErrorResponse
import uk.gov.justice.hmpps.sqs.HmppsQueue
import uk.gov.justice.hmpps.sqs.HmppsQueueService
import uk.gov.justice.hmpps.sqs.MissingQueueException
import uk.gov.justice.hmpps.sqs.MissingTopicException
import uk.gov.justice.hmpps.sqs.countAllMessagesOnQueue
import uk.gov.justice.hmpps.sqs.publish
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

@ExtendWith(HmppsAuthApiExtension::class, ManageUsersExtension::class, PrisonerSearchExtension::class)
@SpringBootTest(webEnvironment = RANDOM_PORT)
@ActiveProfiles("test")
abstract class IntegrationTestBase {
  @Autowired
  lateinit var webTestClient: WebTestClient

  @Autowired
  lateinit var jwtAuthHelper: JwtAuthHelper

  @Autowired
  lateinit var objectMapper: ObjectMapper

  @Autowired
  lateinit var alertRepository: AlertRepository

  @Autowired
  lateinit var alertCodeRepository: AlertCodeRepository

  @Autowired
  lateinit var alertTypeRepository: AlertTypeRepository

  @Autowired
  lateinit var bulkPlanRepository: BulkPlanRepository

  @Autowired
  lateinit var personSummaryRepository: PersonSummaryRepository

  @Autowired
  lateinit var transactionTemplate: TransactionTemplate

  @MockitoSpyBean
  lateinit var hmppsQueueService: HmppsQueueService

  internal val hmppsEventsQueue by lazy {
    hmppsQueueService.findByQueueId("hmppseventtestqueue")
      ?: throw MissingQueueException("hmppseventtestqueue queue not found")
  }

  internal val hmppsEventTopic by lazy {
    hmppsQueueService.findByTopicId("hmppseventtopic")
      ?: throw MissingQueueException("HmppsTopic hmpps event topic not found")
  }

  @BeforeEach
  fun `clear queues`() {
    hmppsEventsQueue.sqsClient.purgeQueue(PurgeQueueRequest.builder().queueUrl(hmppsEventsQueue.queueUrl).build()).get()
  }

  val domainEventsTopic by lazy {
    hmppsQueueService.findByTopicId("hmppseventtopic") ?: throw MissingTopicException("hmppseventtopic not found")
  }

  internal fun sendDomainEvent(event: HmppsDomainEvent) {
    domainEventsTopic.publish(event.eventType, objectMapper.writeValueAsString(event))
  }

  internal val hmppsDomainEventsQueue by lazy {
    hmppsQueueService.findByQueueId("hmppsdomaineventsqueue")
      ?: throw MissingQueueException("hmppsdomaineventsqueue queue not found")
  }

  internal fun setAuthorisation(
    user: String? = null,
    client: String = CLIENT_ID,
    roles: List<String> = listOf(),
    isUserToken: Boolean = true,
  ): (HttpHeaders) -> Unit = jwtAuthHelper.setAuthorisation(user, client, roles, isUserToken = isUserToken)

  internal fun setAlertRequestContext(
    source: Source? = null,
    username: String? = TEST_USER,
  ): (HttpHeaders) -> Unit = {
    it.set(SOURCE, source?.name)
    it.set(USERNAME, username)
  }

  internal fun HmppsQueue.countAllMessagesOnQueue() = sqsClient.countAllMessagesOnQueue(queueUrl).get()

  internal fun HmppsQueue.receiveAllMessages(): List<HmppsDomainEvent> {
    val count = countAllMessagesOnQueue()
    return (1..count).map { hmppsDomainEventOnQueue() }
  }

  internal fun HmppsQueue.receiveMessageOnQueue() = sqsClient.receiveMessage(ReceiveMessageRequest.builder().queueUrl(queueUrl).build()).get().messages().single()

  internal final fun HmppsQueue.receiveMessageTypeCounts(
    messageCount: Int = 1,
  ) = sqsClient.receiveMessage(
    ReceiveMessageRequest.builder().maxNumberOfMessages(messageCount).queueUrl(queueUrl).build(),
  ).get().messages()
    .map { objectMapper.readValue<MsgBody>(it.body()) }
    .map { objectMapper.readValue<EventType>(it.message) }
    .groupingBy { it.eventType }.eachCount()

  data class EventType(val eventType: String)

  internal final inline fun <reified T : AlertBaseAdditionalInformation> HmppsQueue.receiveAlertDomainEventOnQueue() = receiveMessageOnQueue()
    .let { objectMapper.readValue<MsgBody>(it.body()) }
    .let { objectMapper.readValue<AlertDomainEvent<T>>(it.message) }

  internal fun HmppsQueue.hmppsDomainEventOnQueue() = receiveMessageOnQueue()
    .let { objectMapper.readValue<MsgBody>(it.body()) }
    .let { objectMapper.readValue<HmppsDomainEvent>(it.message) }

  data class MsgBody(@JsonProperty("Message") val message: String)

  companion object {
    private val pgContainer = PostgresContainer.instance
    private val localStackContainer = LocalStackContainer.instance

    @JvmStatic
    @DynamicPropertySource
    fun properties(registry: DynamicPropertyRegistry) {
      pgContainer?.run {
        registry.add("spring.datasource.url", pgContainer::getJdbcUrl)
        registry.add("spring.datasource.username", pgContainer::getUsername)
        registry.add("spring.datasource.password", pgContainer::getPassword)
      }

      System.setProperty("aws.region", "eu-west-2")

      localStackContainer?.also { setLocalStackProperties(it, registry) }
    }
  }

  fun WebTestClient.ResponseSpec.errorResponse(httpStatus: HttpStatus) = expectStatus().isEqualTo(httpStatus)
    .expectBody<ErrorResponse>()
    .returnResult().responseBody!!

  fun WebTestClient.ResponseSpec.invalidRowResponse(httpStatus: HttpStatus) = expectStatus().isEqualTo(httpStatus)
    .expectBody<InvalidRowResponse>()
    .returnResult().responseBody!!

  final inline fun <reified T> WebTestClient.ResponseSpec.successResponse(httpStatus: HttpStatus = HttpStatus.OK): T = expectStatus().isEqualTo(httpStatus)
    .expectHeader().contentType(MediaType.APPLICATION_JSON)
    .expectBody(T::class.java)
    .returnResult().responseBody!!

  fun givenPrisoner(): String = givenPrisonerExists(prisonNumber())

  fun givenPrisonerExists(prisonNumber: String): String {
    prisonerSearch.stubGetPrisoner(prisonNumber)
    return prisonNumber
  }

  fun givenExistingAlertType(code: String): AlertType = requireNotNull(alertTypeRepository.findByCode(code))
  fun givenExistingAlertCode(code: String): AlertCode = requireNotNull(alertCodeRepository.findByCode(code))

  fun givenAlertCode(code: String? = null, active: Boolean = true): AlertCode = alertCodeRepository.findAll().first { it.isActive() == active && (code.isNullOrBlank() || it.code == code) }

  fun givenNewAlertType(alertType: AlertType): AlertType = alertTypeRepository.save(alertType)

  fun givenNewAlertCode(alertCode: AlertCode): AlertCode {
    val type = alertTypeRepository.findByCode(alertCode.alertType.code)
    return alertCodeRepository.save(alertCode.apply { set(::alertType, type) })
  }

  fun givenAlert(alert: Alert): Alert = alertRepository.save(
    alert.create(
      createdAt = alert.createdAt,
      createdBy = TEST_USER,
      createdByDisplayName = TEST_USER_NAME,
      source = Source.DPS,
      activeCaseLoadId = null,
      publishEvent = false,
    ),
  )

  fun alert(
    prisonNumber: String,
    alertCode: AlertCode = givenExistingAlertCode(AC_VICTIM.code),
    description: String = "A description of the prisoner alert",
    authorisedBy: String? = "A Person",
    activeFrom: LocalDate = LocalDate.now().minusDays(1),
    activeTo: LocalDate? = null,
    createdAt: LocalDateTime = LocalDateTime.now(),
    deletedAt: LocalDateTime? = null,
    prisonCode: String? = PRISON_CODE_LEEDS,
    alertUuid: UUID = newUuid(),
  ) = Alert(alertCode, prisonNumber, description, authorisedBy, activeFrom, activeTo, createdAt, prisonCode, alertUuid)
    .apply { set(::deletedAt, deletedAt) }

  fun plan(
    alertCode: AlertCode? = null,
    description: String? = null,
    cleanupMode: BulkAlertCleanupMode? = null,
    id: UUID = newUuid(),
  ) = BulkPlan(alertCode, description, cleanupMode, id)

  fun givenBulkPlan(plan: BulkPlan): BulkPlan = bulkPlanRepository.save(plan)

  fun personSummary(
    prisonNumber: String = prisonNumber(),
    firstName: String = "First",
    lastName: String = "Last",
    status: String = "ACTIVE IN",
    restrictedPatient: Boolean = false,
    prisonCode: String? = PRISON_CODE_LEEDS,
    cellLocation: String? = cellLocation(),
    supportingPrisonCode: String? = null,
  ): PersonSummary = PersonSummary(
    prisonNumber,
    firstName,
    lastName,
    status,
    restrictedPatient,
    prisonCode,
    cellLocation,
    supportingPrisonCode,
  )

  fun givenPersonSummary(personSummary: PersonSummary): PersonSummary = personSummaryRepository.save(personSummary)
}
