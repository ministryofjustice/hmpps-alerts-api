package uk.gov.justice.digital.hmpps.hmppsalertsapi.listener

import com.fasterxml.jackson.databind.ObjectMapper
import io.awspring.cloud.sqs.annotation.SqsListener
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppsalertsapi.entity.event.DomainEvent

@Service
class DomainEventListener (private val objectMapper: ObjectMapper) {

  @SqsListener(queueNames = ["hmppseventtopic"], factory = "hmppsQueueContainerFactoryProxy")
  fun processDomainEvent(message: Message) {
    val event = objectMapper.readValue(message.Message, DomainEvent::class.java)

  }

  data class EventType(val Value: String, val Type: String)
  data class MessageAttributes(val eventType: EventType)
  data class Message(
    val Message: String,
    val MessageId: String,
    val MessageAttributes: MessageAttributes,
  )
}