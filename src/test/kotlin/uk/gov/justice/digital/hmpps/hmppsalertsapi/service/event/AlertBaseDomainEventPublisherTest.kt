package uk.gov.justice.digital.hmpps.hmppsalertsapi.service.event

import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonMapperBuilder
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import software.amazon.awssdk.services.sns.SnsAsyncClient
import software.amazon.awssdk.services.sns.model.MessageAttributeValue
import software.amazon.awssdk.services.sns.model.PublishRequest
import uk.gov.justice.digital.hmpps.hmppsalertsapi.common.toZoneDateTime
import uk.gov.justice.digital.hmpps.hmppsalertsapi.entity.event.AlertAdditionalInformation
import uk.gov.justice.digital.hmpps.hmppsalertsapi.entity.event.AlertDomainEvent
import uk.gov.justice.digital.hmpps.hmppsalertsapi.enumeration.DomainEventType.ALERT_CREATED
import uk.gov.justice.digital.hmpps.hmppsalertsapi.enumeration.Source.NOMIS
import uk.gov.justice.digital.hmpps.hmppsalertsapi.utils.ALERT_CODE_VICTIM
import uk.gov.justice.hmpps.sqs.HmppsQueueService
import uk.gov.justice.hmpps.sqs.HmppsTopic
import java.time.LocalDateTime
import java.util.UUID

class AlertBaseDomainEventPublisherTest {
  private val hmppsQueueService = mock<HmppsQueueService>()
  private val domainEventsTopic = mock<HmppsTopic>()
  private val domainEventsSnsClient = mock<SnsAsyncClient>()
  private val objectMapper = jacksonMapperBuilder().addModule(JavaTimeModule()).build()

  private val domainEventsTopicArn = "arn:aws:sns:eu-west-2:000000000000:${UUID.randomUUID()}"
  private val baseUrl = "http://localhost:8080"

  @Test
  fun `throws IllegalStateException when topic not found`() {
    whenever(hmppsQueueService.findByTopicId("hmppseventtopic")).thenReturn(null)
    val domainEventPublisher = DomainEventPublisher(hmppsQueueService, objectMapper)
    val exception = assertThrows<IllegalStateException> { domainEventPublisher.publish(mock<AlertDomainEvent<AlertAdditionalInformation>>()) }
    assertThat(exception.message).isEqualTo("hmppseventtopic not found")
  }

  @Test
  fun `publish alert event`() {
    whenever(hmppsQueueService.findByTopicId("hmppseventtopic")).thenReturn(domainEventsTopic)
    whenever(domainEventsTopic.snsClient).thenReturn(domainEventsSnsClient)
    whenever(domainEventsTopic.arn).thenReturn(domainEventsTopicArn)
    val domainEventPublisher = DomainEventPublisher(hmppsQueueService, objectMapper)
    val alertUuid = UUID.randomUUID()
    val occurredAt = LocalDateTime.now()
    val domainEvent = AlertDomainEvent(
      eventType = ALERT_CREATED.eventType,
      additionalInformation = AlertAdditionalInformation(
        alertUuid = alertUuid,
        alertCode = ALERT_CODE_VICTIM,
        source = NOMIS,
      ),
      description = ALERT_CREATED.description,
      occurredAt = occurredAt.toZoneDateTime(),
    )

    domainEventPublisher.publish(domainEvent)

    verify(domainEventsSnsClient).publish(
      PublishRequest.builder()
        .topicArn(domainEventsTopic.arn)
        .message(objectMapper.writeValueAsString(domainEvent))
        .messageAttributes(mapOf("eventType" to MessageAttributeValue.builder().dataType("String").stringValue(domainEvent.eventType).build()))
        .build(),
    )
  }

  @Test
  fun `publish alert event - failure`() {
    whenever(hmppsQueueService.findByTopicId("hmppseventtopic")).thenReturn(domainEventsTopic)
    whenever(domainEventsTopic.snsClient).thenReturn(domainEventsSnsClient)
    whenever(domainEventsTopic.arn).thenReturn(domainEventsTopicArn)
    val domainEventPublisher = DomainEventPublisher(hmppsQueueService, objectMapper)
    val domainEvent = mock<AlertDomainEvent<AlertAdditionalInformation>>()
    whenever(domainEventsSnsClient.publish(any<PublishRequest>())).thenThrow(RuntimeException("Failed to publish"))

    domainEventPublisher.publish(domainEvent)
  }
}
