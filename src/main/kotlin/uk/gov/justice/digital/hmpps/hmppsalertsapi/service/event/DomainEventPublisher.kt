package uk.gov.justice.digital.hmpps.hmppsalertsapi.service.event

import com.fasterxml.jackson.databind.ObjectMapper
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import software.amazon.awssdk.services.sqs.model.SendMessageRequest
import uk.gov.justice.digital.hmpps.hmppsalertsapi.entity.event.AdditionalInformation
import uk.gov.justice.digital.hmpps.hmppsalertsapi.entity.event.DomainEvent
import uk.gov.justice.hmpps.sqs.HmppsQueue
import uk.gov.justice.hmpps.sqs.HmppsQueueService

@Service
class DomainEventPublisher(
  private val hmppsQueueService: HmppsQueueService,
  private val objectMapper: ObjectMapper,
) {
  private val publishQueue by lazy { hmppsQueueService.findByQueueId("publish") as HmppsQueue }
  private val publishSqsClient by lazy { publishQueue.sqsClient }
  private val publishQueueUrl by lazy { publishQueue.queueUrl }

  fun <T : AdditionalInformation> publish(domainEvent: DomainEvent<T>) {
    val request = SendMessageRequest.builder().queueUrl(publishQueueUrl)
      .messageBody(objectMapper.writeValueAsString(domainEvent))
      .build()

    runCatching {
      publishSqsClient.sendMessage(request).get()
    }.onFailure {
      log.error("Failed to publish '$domainEvent'", it)
    }
  }

  private companion object {
    private val log: Logger = LoggerFactory.getLogger(this::class.java)
  }
}
