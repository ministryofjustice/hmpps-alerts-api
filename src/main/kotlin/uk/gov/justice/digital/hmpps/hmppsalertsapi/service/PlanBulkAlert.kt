package uk.gov.justice.digital.hmpps.hmppsalertsapi.service

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.hmppsalertsapi.client.prisonersearch.PrisonerSearchClient
import uk.gov.justice.digital.hmpps.hmppsalertsapi.config.AlertRequestContext.Companion.PRISON_NUMBER_REGEX
import uk.gov.justice.digital.hmpps.hmppsalertsapi.entity.BulkPlanRepository
import uk.gov.justice.digital.hmpps.hmppsalertsapi.entity.PersonSummary
import uk.gov.justice.digital.hmpps.hmppsalertsapi.entity.PersonSummaryRepository
import uk.gov.justice.digital.hmpps.hmppsalertsapi.entity.getPlan
import uk.gov.justice.digital.hmpps.hmppsalertsapi.entity.toPersonSummary
import uk.gov.justice.digital.hmpps.hmppsalertsapi.exceptions.InvalidRowException
import uk.gov.justice.digital.hmpps.hmppsalertsapi.model.BulkPlan
import uk.gov.justice.digital.hmpps.hmppsalertsapi.model.request.AddPrisonNumbers
import uk.gov.justice.digital.hmpps.hmppsalertsapi.model.request.BulkAction
import uk.gov.justice.digital.hmpps.hmppsalertsapi.model.request.SetAlertCode
import uk.gov.justice.digital.hmpps.hmppsalertsapi.model.request.SetDescription
import uk.gov.justice.digital.hmpps.hmppsalertsapi.repository.AlertCodeRepository
import uk.gov.justice.digital.hmpps.hmppsalertsapi.repository.getByCode
import java.util.UUID
import uk.gov.justice.digital.hmpps.hmppsalertsapi.entity.BulkPlan as Plan

@Transactional
@Service
class PlanBulkAlert(
  private val planRepository: BulkPlanRepository,
  private val alertCodeRepository: AlertCodeRepository,
  private val personSummaryRepository: PersonSummaryRepository,
  private val prisonerSearch: PrisonerSearchClient,
) {
  fun createNew(): BulkPlan = planRepository.save(Plan()).toModel()
  fun update(id: UUID, actions: Set<BulkAction>): BulkPlan {
    val plan = planRepository.getPlan(id)
    actions.forEach { action ->
      when (action) {
        is SetAlertCode -> plan.setAlertCode(action)
        is SetDescription -> plan.setDescription(action)
        is AddPrisonNumbers -> plan.addPrisonNumbers(action)
      }
    }
    return plan.toModel()
  }

  private fun Plan.setAlertCode(action: SetAlertCode) {
    alertCode = alertCodeRepository.getByCode(action.alertCode)
  }

  private fun Plan.setDescription(action: SetDescription) {
    description = action.description
  }

  private fun Plan.addPrisonNumbers(action: AddPrisonNumbers) {
    this.people += action.prisonNumbers.validate()
    // TODO further logic to add the actual alerts
  }

  private fun LinkedHashSet<String>.validate(): List<PersonSummary> {
    val invalidRows = mapIndexedNotNull { index, pn ->
      if (pn.matches(PRISON_NUMBER_REGEX.toRegex())) null else (index + 1)
    }.toSet()
    if (invalidRows.isNotEmpty()) {
      throw InvalidRowException(invalidRows)
    }
    val existingPeople = personSummaryRepository.findAllById(this)
    val existingPrisonNumbers = existingPeople.map { it.prisonNumber }.toSet()
    return prisonerSearch.getPrisoners(this - existingPrisonNumbers).let { prisoners ->
      val newPeople = personSummaryRepository.saveAll(prisoners.map { it.toPersonSummary() })
      val diff = this - existingPrisonNumbers - prisoners.map { it.prisonerNumber }.toSet()
      check(diff.isEmpty()) {
        val pnIndex = mapIndexed { index, pn -> pn to (index + 1) }.toMap()
        throw InvalidRowException(diff.mapNotNull { pnIndex[it] }.toSet())
      }
      existingPeople + newPeople
    }
  }
}

fun Plan.toModel() = BulkPlan(id)
