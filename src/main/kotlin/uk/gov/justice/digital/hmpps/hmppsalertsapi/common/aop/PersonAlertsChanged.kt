package uk.gov.justice.digital.hmpps.hmppsalertsapi.common.aop

import jakarta.annotation.PostConstruct
import org.aspectj.lang.annotation.After
import org.aspectj.lang.annotation.Aspect
import org.aspectj.lang.annotation.Before
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.hmppsalertsapi.entity.event.PersonAlertsChangedEvent

@Aspect
@Component
class PersonAlertsChanged(private val eventPublisher: ApplicationEventPublisher) {

  @PostConstruct
  fun makeAccessible() {
    personAlertsChanged = this
  }

  private val prisonerNumbers: ThreadLocal<MutableSet<String>> = ThreadLocal()

  @Before("@annotation(uk.gov.justice.digital.hmpps.hmppsalertsapi.common.aop.PublishPersonAlertsChanged)")
  fun beforePublish() {
    prisonerNumbers.set(mutableSetOf())
  }

  @Transactional(propagation = Propagation.MANDATORY)
  @After("@annotation(uk.gov.justice.digital.hmpps.hmppsalertsapi.common.aop.PublishPersonAlertsChanged)")
  fun publish() {
    prisonerNumbers.get().forEach(::publishPersonAlertsChanged)
  }

  private fun publishPersonAlertsChanged(prisonNumber: String) {
    eventPublisher.publishEvent(PersonAlertsChangedEvent(prisonNumber))
  }

  companion object {
    private var personAlertsChanged: PersonAlertsChanged? = null

    fun registerChange(prisonNumber: String) {
      personAlertsChanged?.prisonerNumbers?.get()?.add(prisonNumber)
    }
  }
}

@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class PublishPersonAlertsChanged
