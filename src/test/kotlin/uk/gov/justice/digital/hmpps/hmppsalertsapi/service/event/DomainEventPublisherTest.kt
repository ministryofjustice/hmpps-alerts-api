package uk.gov.justice.digital.hmpps.hmppsalertsapi.service.event

import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import software.amazon.awssdk.services.sqs.SqsAsyncClient
import software.amazon.awssdk.services.sqs.model.SendMessageRequest
import uk.gov.justice.digital.hmpps.hmppsalertsapi.entity.event.AlertAdditionalInformation
import uk.gov.justice.digital.hmpps.hmppsalertsapi.entity.event.AlertDomainEvent
import uk.gov.justice.digital.hmpps.hmppsalertsapi.enumeration.DomainEventType.ALERT_CREATED
import uk.gov.justice.digital.hmpps.hmppsalertsapi.enumeration.Source.NOMIS
import uk.gov.justice.digital.hmpps.hmppsalertsapi.integration.wiremock.PRISON_NUMBER
import uk.gov.justice.digital.hmpps.hmppsalertsapi.utils.ALERT_CODE_VICTIM
import uk.gov.justice.digital.hmpps.hmppsalertsapi.utils.testObjectMapper
import uk.gov.justice.hmpps.sqs.HmppsQueue
import uk.gov.justice.hmpps.sqs.HmppsQueueService
import java.time.LocalDateTime
import java.util.UUID

class DomainEventPublisherTest {
  private val objectMapper = testObjectMapper()

  private val baseUrl = "http://localhost:8080"

  @Test
  fun `publish alert event`() {
    val hmppsQueueService = mock<HmppsQueueService>()
    val publishQueue = mock<HmppsQueue>()
    val publishSqsClient = mock<SqsAsyncClient>()
    val publishQueueUrl = "publish-queue-url"
    whenever(hmppsQueueService.findByQueueId("publish")).thenReturn(publishQueue)
    whenever(publishQueue.sqsClient).thenReturn(publishSqsClient)
    whenever(publishQueue.queueUrl).thenReturn(publishQueueUrl)
    val domainEventPublisher = DomainEventPublisher(hmppsQueueService, objectMapper)
    val alertUuid = UUID.randomUUID()
    val occurredAt = LocalDateTime.now()
    val domainEvent = AlertDomainEvent(
      eventType = ALERT_CREATED.eventType,
      additionalInformation = AlertAdditionalInformation(
        url = "$baseUrl/alerts/$alertUuid",
        alertUuid = alertUuid,
        prisonNumber = PRISON_NUMBER,
        alertCode = ALERT_CODE_VICTIM,
        source = NOMIS,
      ),
      description = ALERT_CREATED.description,
      occurredAt = occurredAt,
    )

    domainEventPublisher.publish(domainEvent)

    verify(publishSqsClient).sendMessage(
      SendMessageRequest.builder().queueUrl(publishQueueUrl)
        .messageBody(objectMapper.writeValueAsString(domainEvent))
        .build(),
    )
  }

  @Test
  fun `publish alert event - failure`() {
    val hmppsQueueService = mock<HmppsQueueService>()
    val publishQueue = mock<HmppsQueue>()
    val publishSqsClient = mock<SqsAsyncClient>()
    val publishQueueUrl = "publish-queue-url"
    whenever(hmppsQueueService.findByQueueId("publish")).thenReturn(publishQueue)
    whenever(publishQueue.sqsClient).thenReturn(publishSqsClient)
    whenever(publishQueue.queueUrl).thenReturn(publishQueueUrl)
    val domainEventPublisher = DomainEventPublisher(hmppsQueueService, objectMapper)
    val domainEvent = mock<AlertDomainEvent>()
    whenever(publishSqsClient.sendMessage(any<SendMessageRequest>())).thenThrow(RuntimeException("Failed to publish"))

    domainEventPublisher.publish(domainEvent)
  }
}
