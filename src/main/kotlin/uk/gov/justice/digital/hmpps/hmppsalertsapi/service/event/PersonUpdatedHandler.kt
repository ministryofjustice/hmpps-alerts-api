package uk.gov.justice.digital.hmpps.hmppsalertsapi.service.event

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.hmppsalertsapi.entity.event.HmppsDomainEvent
import uk.gov.justice.digital.hmpps.hmppsalertsapi.entity.event.PersonChanged.CATEGORIES_OF_INTEREST
import uk.gov.justice.digital.hmpps.hmppsalertsapi.entity.event.categoriesChanged
import uk.gov.justice.digital.hmpps.hmppsalertsapi.entity.event.nomsNumber
import uk.gov.justice.digital.hmpps.hmppsalertsapi.service.PersonSummaryService

@Transactional
@Service
class PersonUpdatedHandler(private val personSummaryService: PersonSummaryService) {
  fun handle(prisonerChanged: HmppsDomainEvent) {
    val matchingChanges = prisonerChanged.additionalInformation.categoriesChanged intersect CATEGORIES_OF_INTEREST
    if (matchingChanges.isNotEmpty()) {
      personSummaryService.updateExistingDetails(prisonerChanged.additionalInformation.nomsNumber)
    }
  }
}
