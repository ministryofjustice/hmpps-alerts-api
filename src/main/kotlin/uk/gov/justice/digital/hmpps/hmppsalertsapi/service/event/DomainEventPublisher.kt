package uk.gov.justice.digital.hmpps.hmppsalertsapi.service.event

import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.stereotype.Service
import software.amazon.awssdk.services.sns.model.MessageAttributeValue
import uk.gov.justice.digital.hmpps.hmppsalertsapi.entity.event.DomainEvent
import uk.gov.justice.hmpps.sqs.HmppsQueueService
import uk.gov.justice.hmpps.sqs.publish

@Service
class DomainEventPublisher(
  private val hmppsQueueService: HmppsQueueService,
  private val objectMapper: ObjectMapper,
) {
  private val domainEventsTopic by lazy {
    hmppsQueueService.findByTopicId("hmppseventtopic") ?: throw IllegalStateException("hmppseventtopic not found")
  }

  fun publish(domainEvent: DomainEvent) {
    domainEventsTopic.publish(
      domainEvent.eventType,
      objectMapper.writeValueAsString(domainEvent),
      domainEvent.attributes(),
    )
  }

  private fun DomainEvent.attributes() =
    mapOf("eventType" to MessageAttributeValue.builder().dataType("String").stringValue(eventType).build())
}
