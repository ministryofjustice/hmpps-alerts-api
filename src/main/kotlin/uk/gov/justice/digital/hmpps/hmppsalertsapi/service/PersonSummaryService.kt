package uk.gov.justice.digital.hmpps.hmppsalertsapi.service

import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.hmppsalertsapi.client.prisonersearch.PrisonerSearchClient
import uk.gov.justice.digital.hmpps.hmppsalertsapi.entity.PersonSummary
import uk.gov.justice.digital.hmpps.hmppsalertsapi.entity.PersonSummaryRepository

@Service
@Transactional
class PersonSummaryService(
  private val prisonerSearch: PrisonerSearchClient,
  private val personSummaryRepository: PersonSummaryRepository,
) {
  fun updateExistingDetails(prisonNumber: String) {
    personSummaryRepository.findByIdOrNull(prisonNumber)?.also {
      val prisoner = requireNotNull(prisonerSearch.getPrisoner(prisonNumber).block()) { "Prisoner number invalid" }
      it.update(
        prisoner.firstName,
        prisoner.lastName,
        prisoner.status,
        prisoner.restrictedPatient,
        prisoner.prisonId,
        prisoner.cellLocation,
        prisoner.supportingPrisonId,
      )
    }
  }

  fun savePersonSummary(personSummary: PersonSummary): PersonSummary =
    personSummaryRepository.findByIdOrNull(personSummary.prisonNumber) ?: personSummaryRepository.save(personSummary)

  fun getPersonSummaryByPrisonNumber(prisonNumber: String): PersonSummary {
    val person = personSummaryRepository.findByIdOrNull(prisonNumber)
    return if (person == null) {
      val prisoner = requireNotNull(prisonerSearch.getPrisoner(prisonNumber).block()) { "Prisoner number invalid" }
      personSummaryRepository.save(
        PersonSummary(
          prisoner.prisonerNumber,
          prisoner.firstName,
          prisoner.lastName,
          prisoner.status,
          prisoner.restrictedPatient,
          prisoner.prisonId,
          prisoner.cellLocation,
          prisoner.supportingPrisonId,
        ),
      )
    } else {
      person
    }
  }

  fun removePersonSummaryByPrisonNumber(prisonNumber: String) =
    personSummaryRepository.findByIdOrNull(prisonNumber)?.also(personSummaryRepository::delete)
}
