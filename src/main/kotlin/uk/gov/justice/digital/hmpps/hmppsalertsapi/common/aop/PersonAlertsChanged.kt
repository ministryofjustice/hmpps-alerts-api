package uk.gov.justice.digital.hmpps.hmppsalertsapi.common.aop

import com.microsoft.applicationinsights.TelemetryClient
import jakarta.annotation.PostConstruct
import org.aspectj.lang.annotation.After
import org.aspectj.lang.annotation.Aspect
import org.aspectj.lang.annotation.Before
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.hmppsalertsapi.entity.event.PersonAlertsChangedEvent
import java.time.LocalDateTime.now
import java.time.format.DateTimeFormatter
import kotlin.time.measureTime

@Aspect
@Component
class PersonAlertsChanged(
  private val eventPublisher: ApplicationEventPublisher,
  private val telemetryClient: TelemetryClient,
) {

  @PostConstruct
  fun makeAccessible() {
    personAlertsChanged = this
  }

  private val prisonerNumbers: ThreadLocal<MutableSet<String>> = ThreadLocal()

  @Before("@annotation(uk.gov.justice.digital.hmpps.hmppsalertsapi.common.aop.PublishPersonAlertsChanged)")
  fun beforePublish() {
    telemetryClient.trackEvent(
      "PreparingAlertsChanged",
      mapOf("timestamp" to now().format(DateTimeFormatter.ISO_DATE_TIME)),
      mapOf(),
    )
    prisonerNumbers.set(mutableSetOf())
  }

  @Transactional(propagation = Propagation.MANDATORY)
  @After("@annotation(uk.gov.justice.digital.hmpps.hmppsalertsapi.common.aop.PublishPersonAlertsChanged)")
  fun publish() {
    telemetryClient.trackEvent(
      "PublishingInternalAlertsChanged",
      mapOf("timestamp" to now().format(DateTimeFormatter.ISO_DATE_TIME)),
      mapOf(),
    )
    val duration = measureTime {
      prisonerNumbers.get().forEach(::publishPersonAlertsChanged)
      prisonerNumbers.get().clear()
    }
    telemetryClient.trackEvent(
      "PublishedInternalAlertsChanged",
      mapOf(
        "duration" to duration.inWholeMilliseconds.toString(),
        "timestamp" to now().format(DateTimeFormatter.ISO_DATE_TIME),
      ),
      mapOf(),
    )
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
