package uk.gov.justice.digital.hmpps.hmppsalertsapi.service.event

import com.fasterxml.jackson.databind.ObjectMapper
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import software.amazon.awssdk.services.sns.model.MessageAttributeValue
import software.amazon.awssdk.services.sns.model.PublishRequest
import uk.gov.justice.digital.hmpps.hmppsalertsapi.entity.event.AdditionalInformation
import uk.gov.justice.digital.hmpps.hmppsalertsapi.entity.event.DomainEvent
import uk.gov.justice.hmpps.sqs.HmppsQueueService

@Service
class DomainEventPublisher(
  private val hmppsQueueService: HmppsQueueService,
  private val objectMapper: ObjectMapper,
) {
  private val domainEventsTopic by lazy {
    hmppsQueueService.findByTopicId("hmppseventtopic") ?: throw IllegalStateException("hmppseventtopic not found")
  }

  fun <T : AdditionalInformation> publish(domainEvent: DomainEvent<T>) {
    val request = PublishRequest.builder()
      .topicArn(domainEventsTopic.arn)
      .message(objectMapper.writeValueAsString(domainEvent))
      .messageAttributes(domainEvent.attributes())
      .build()

    runCatching {
      domainEventsTopic.snsClient.publish(request)
        .get()
        .also { log.debug("Published {} with response {}", domainEvent, it) }
    }.onFailure {
      log.error("Failed to publish '$domainEvent'", it)
    }
  }

  private fun <T : AdditionalInformation> DomainEvent<T>.attributes() =
    mapOf("eventType" to MessageAttributeValue.builder().dataType("String").stringValue(eventType).build())

  private companion object {
    private val log: Logger = LoggerFactory.getLogger(this::class.java)
  }
}
