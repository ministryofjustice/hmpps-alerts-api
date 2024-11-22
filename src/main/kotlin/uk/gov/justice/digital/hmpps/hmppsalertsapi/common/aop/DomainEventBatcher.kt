package uk.gov.justice.digital.hmpps.hmppsalertsapi.common.aop

import org.aspectj.lang.annotation.After
import org.aspectj.lang.annotation.Aspect
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.hmppsalertsapi.entity.event.DomainEvent
import uk.gov.justice.digital.hmpps.hmppsalertsapi.service.event.DomainEventPublisher
import java.lang.ThreadLocal.withInitial

@Aspect
@Component
class DomainEventBatcher(
  private val domainEventPublisher: DomainEventPublisher,
  @Value("\${service.event.batch-size:10}") private val eventBatchSize: Int,
) {
  private val batchedEvents = withInitial { mutableSetOf<DomainEvent>() }

  fun batchEvent(event: DomainEvent) {
    batchedEvents.get().add(event)
    if (batchedEvents.get().size >= eventBatchSize) batchComplete()
  }

  @After(
    "@annotation(org.springframework.web.bind.annotation.PostMapping) || @annotation(org.springframework.web.bind.annotation.PutMapping) " +
      "|| @annotation(org.springframework.web.bind.annotation.PatchMapping) || @annotation(org.springframework.web.bind.annotation.DeleteMapping)",
  )
  fun batchComplete() {
    with(batchedEvents.get()) {
      if (isNotEmpty()) {
        domainEventPublisher.publishBatch(this)
        clear()
      }
    }
  }
}
