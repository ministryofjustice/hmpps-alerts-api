package uk.gov.justice.digital.hmpps.hmppsalertsapi.service.event

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.hmppsalertsapi.entity.PersonSummaryRepository
import uk.gov.justice.digital.hmpps.hmppsalertsapi.entity.event.HmppsDomainEvent
import uk.gov.justice.digital.hmpps.hmppsalertsapi.entity.event.removedNomsNumber
import uk.gov.justice.digital.hmpps.hmppsalertsapi.model.request.RemovePrisonNumbers
import uk.gov.justice.digital.hmpps.hmppsalertsapi.service.PlanBulkAlert
import java.util.LinkedHashSet.newLinkedHashSet

@Transactional
@Service
class PersonMergedHandler(
  private val personSummaryRepository: PersonSummaryRepository,
  private val planBulkAlert: PlanBulkAlert,
) {
  fun handle(personMerged: HmppsDomainEvent) {
    val toRemove = personMerged.additionalInformation.removedNomsNumber
    planBulkAlert.findPlansFromPrisonNumber(toRemove).forEach {
      planBulkAlert.update(
        it.id,
        setOf(
          RemovePrisonNumbers(newLinkedHashSet<String>(1).apply { add(toRemove) }),
        ),
      )
    }
    personSummaryRepository.deleteById(toRemove)
  }
}
