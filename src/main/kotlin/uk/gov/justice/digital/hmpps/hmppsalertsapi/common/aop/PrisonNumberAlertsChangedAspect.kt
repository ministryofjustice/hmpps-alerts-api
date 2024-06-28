package uk.gov.justice.digital.hmpps.hmppsalertsapi.common.aop

import org.aspectj.lang.annotation.Aspect
import org.aspectj.lang.annotation.Before
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Component
import org.springframework.transaction.support.TransactionTemplate
import uk.gov.justice.digital.hmpps.hmppsalertsapi.entity.event.PersonAlertsChangedEvent
import uk.gov.justice.digital.hmpps.hmppsalertsapi.model.request.BulkCreateAlerts
import uk.gov.justice.digital.hmpps.hmppsalertsapi.model.request.MergeAlerts
import uk.gov.justice.digital.hmpps.hmppsalertsapi.repository.AlertRepository
import java.util.UUID

@Aspect
@Component
class PrisonNumberAlertsChangedAspect(
  private val transactionTemplate: TransactionTemplate,
  private val eventPublisher: ApplicationEventPublisher,
  private val alertRepository: AlertRepository,
) {

  @Before("@annotation(uk.gov.justice.digital.hmpps.hmppsalertsapi.common.aop.PrisonerAlertsChangedByPrisonNumber) && execution(* *(String,..)) && args(prisonNumber,..)")
  fun personAlertsChanged(prisonNumber: String) {
    publishPersonAlertsChanged(prisonNumber)
  }

  @Before("@annotation(uk.gov.justice.digital.hmpps.hmppsalertsapi.common.aop.PrisonerAlertsChangedByAlertUuid) && execution(* *(java.util.UUID,..)) && args(alertUuid,..)")
  fun personAlertsChanged(alertUuid: UUID) {
    alertRepository.findByAlertUuid(alertUuid)?.prisonNumber?.also(::publishPersonAlertsChanged)
  }

  @Before("@annotation(uk.gov.justice.digital.hmpps.hmppsalertsapi.common.aop.PrisonerAlertsChangedByMerge) && execution(* *(uk.gov.justice.digital.hmpps.hmppsalertsapi.model.request.MergeAlerts,..)) && args(mergeAlerts,..)")
  fun personAlertsMerged(mergeAlerts: MergeAlerts) {
    listOf(mergeAlerts.prisonNumberMergeFrom, mergeAlerts.prisonNumberMergeTo).forEach(::publishPersonAlertsChanged)
  }

  @Before("@annotation(uk.gov.justice.digital.hmpps.hmppsalertsapi.common.aop.PrisonerAlertsChangedInBulk) && execution(* *(uk.gov.justice.digital.hmpps.hmppsalertsapi.model.request.BulkCreateAlerts,..)) && args(bulk,..)")
  fun personAlertsChangedInBulk(bulk: BulkCreateAlerts) {
    bulk.prisonNumbers.toSet().forEach(::publishPersonAlertsChanged)
  }

  private fun publishPersonAlertsChanged(prisonNumber: String) {
    transactionTemplate.execute {
      eventPublisher.publishEvent(PersonAlertsChangedEvent(prisonNumber))
    }
  }
}

@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class PrisonerAlertsChangedByPrisonNumber

@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class PrisonerAlertsChangedByAlertUuid

@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class PrisonerAlertsChangedByMerge

@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class PrisonerAlertsChangedInBulk
