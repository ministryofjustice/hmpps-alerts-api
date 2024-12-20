package uk.gov.justice.digital.hmpps.hmppsalertsapi.service.event

import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonMapperBuilder
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import software.amazon.awssdk.services.sns.SnsAsyncClient
import software.amazon.awssdk.services.sns.model.PublishRequest
import uk.gov.justice.digital.hmpps.hmppsalertsapi.entity.event.AlertAdditionalInformation
import uk.gov.justice.digital.hmpps.hmppsalertsapi.entity.event.AlertDomainEvent
import uk.gov.justice.hmpps.sqs.HmppsQueueService
import uk.gov.justice.hmpps.sqs.HmppsTopic
import java.util.UUID

class AlertBaseDomainEventPublisherTest {
  private val hmppsQueueService = mock<HmppsQueueService>()
  private val domainEventsTopic = mock<HmppsTopic>()
  private val domainEventsSnsClient = mock<SnsAsyncClient>()
  private val objectMapper = jacksonMapperBuilder().addModule(JavaTimeModule()).build()

  private val domainEventsTopicArn = "arn:aws:sns:eu-west-2:000000000000:${UUID.randomUUID()}"

  @Test
  fun `throws IllegalStateException when topic not found`() {
    whenever(hmppsQueueService.findByTopicId("hmppseventtopic")).thenReturn(null)
    val domainEventPublisher = DomainEventPublisher(hmppsQueueService, objectMapper, 1000)
    val exception = assertThrows<IllegalStateException> { domainEventPublisher.publishSingle(mock<AlertDomainEvent<AlertAdditionalInformation>>()) }
    assertThat(exception.message).isEqualTo("hmppseventtopic not found")
  }

  @Test
  fun `publish alert event - failure`() {
    whenever(hmppsQueueService.findByTopicId("hmppseventtopic")).thenReturn(domainEventsTopic)
    whenever(domainEventsTopic.snsClient).thenReturn(domainEventsSnsClient)
    whenever(domainEventsTopic.arn).thenReturn(domainEventsTopicArn)
    val domainEventPublisher = DomainEventPublisher(hmppsQueueService, objectMapper, 1000)
    val domainEvent = mock<AlertDomainEvent<AlertAdditionalInformation>>()
    whenever(domainEvent.eventType).thenReturn("some.event.type")
    whenever(domainEventsSnsClient.publish(any<PublishRequest>())).thenThrow(RuntimeException("Failed to publish"))

    val ex = assertThrows<RuntimeException> { domainEventPublisher.publishSingle(domainEvent) }
    assertThat(ex.message).isEqualTo("Failed to publish")
  }
}
