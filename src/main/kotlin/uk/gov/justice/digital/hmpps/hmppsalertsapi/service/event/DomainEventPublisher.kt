package uk.gov.justice.digital.hmpps.hmppsalertsapi.service.event

import com.fasterxml.jackson.databind.ObjectMapper
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

    publishSqsClient.sendMessage(request).get()
  }
}
