package uk.gov.justice.digital.hmpps.hmppsalertsapi.service.event

import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.beans.factory.annotation.Value
import org.springframework.retry.RetryPolicy
import org.springframework.retry.backoff.BackOffPolicy
import org.springframework.retry.support.RetryTemplate
import org.springframework.stereotype.Service
import software.amazon.awssdk.services.sns.model.PublishBatchRequest
import software.amazon.awssdk.services.sns.model.PublishBatchRequestEntry
import software.amazon.awssdk.services.sns.model.PublishBatchResponse
import uk.gov.justice.digital.hmpps.hmppsalertsapi.IdGenerator
import uk.gov.justice.digital.hmpps.hmppsalertsapi.entity.event.DomainEvent
import uk.gov.justice.hmpps.sqs.DEFAULT_BACKOFF_POLICY
import uk.gov.justice.hmpps.sqs.DEFAULT_RETRY_POLICY
import uk.gov.justice.hmpps.sqs.HmppsQueueService
import uk.gov.justice.hmpps.sqs.HmppsTopic
import uk.gov.justice.hmpps.sqs.eventTypeSnsMap
import uk.gov.justice.hmpps.sqs.publish

@Service
class DomainEventPublisher(
  private val hmppsQueueService: HmppsQueueService,
  private val objectMapper: ObjectMapper,
  @Value("\${service.event.batch-size:10}") private val eventBatchSize: Int,
) {
  private val domainEventsTopic by lazy {
    hmppsQueueService.findByTopicId("hmppseventtopic") ?: throw IllegalStateException("hmppseventtopic not found")
  }

  fun publishSingle(domainEvent: DomainEvent) {
    domainEventsTopic.publish(domainEvent.eventType, objectMapper.writeValueAsString(domainEvent))
  }

  fun publishBatch(domainEvents: Set<DomainEvent>) {
    domainEvents.chunked(eventBatchSize).forEach { chunk ->
      domainEventsTopic.publishBatch(chunk)
    }
  }

  fun HmppsTopic.publishBatch(
    events: Iterable<DomainEvent>,
    retryPolicy: RetryPolicy = DEFAULT_RETRY_POLICY,
    backOffPolicy: BackOffPolicy = DEFAULT_BACKOFF_POLICY,
  ) {
    val retryTemplate = RetryTemplate().apply {
      setRetryPolicy(retryPolicy)
      setBackOffPolicy(backOffPolicy)
    }
    val publishRequest = PublishBatchRequest.builder().topicArn(arn).publishBatchRequestEntries(
      events.map {
        PublishBatchRequestEntry.builder()
          .id(IdGenerator.newUuid().toString())
          .message(objectMapper.writeValueAsString(it))
          .messageAttributes(eventTypeSnsMap(it.eventType, noTracing = true))
          .build()
      },
    ).build()
    retryTemplate.execute<PublishBatchResponse, RuntimeException> {
      snsClient.publishBatch(publishRequest).get()
    }
  }
}
