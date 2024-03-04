package uk.gov.justice.digital.hmpps.hmppsalertsapi.service.event

import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.stereotype.Service
import software.amazon.awssdk.services.sqs.model.SendMessageRequest
import uk.gov.justice.digital.hmpps.hmppsalertsapi.enumeration.Source
import uk.gov.justice.hmpps.sqs.HmppsQueue
import uk.gov.justice.hmpps.sqs.HmppsQueueService
import java.time.LocalDateTime
import java.util.UUID

@Service
class DomainEventPublisher(
  private val hmppsQueueService: HmppsQueueService,
  private val objectMapper: ObjectMapper,
) {
  private val publishQueue by lazy { hmppsQueueService.findByQueueId("publish") as HmppsQueue }
  private val publishSqsClient by lazy { publishQueue.sqsClient }
  private val publishQueueUrl by lazy { publishQueue.queueUrl }

  fun publish(domainEvent: AlertDomainEvent) {
    val request = SendMessageRequest.builder().queueUrl(publishQueueUrl)
      .messageBody(objectMapper.writeValueAsString(domainEvent))
      .build()

    publishSqsClient.sendMessage(request).get()
  }
}

data class AlertAdditionalInformation(
  val url: String,
  val alertUuid: UUID,
  val prisonNumber: String,
  val alertCode: String,
  val source: Source,
)

data class AlertDomainEvent(
  val eventType: String,
  val additionalInformation: AlertAdditionalInformation,
  val version: Int = 1,
  val description: String,
  val occurredAt: LocalDateTime = LocalDateTime.now(),
)
