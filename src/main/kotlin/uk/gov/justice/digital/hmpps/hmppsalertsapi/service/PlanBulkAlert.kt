package uk.gov.justice.digital.hmpps.hmppsalertsapi.service

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.hmppsalertsapi.client.prisonersearch.PrisonerSearchClient
import uk.gov.justice.digital.hmpps.hmppsalertsapi.config.AlertRequestContext.Companion.PRISON_NUMBER_REGEX
import uk.gov.justice.digital.hmpps.hmppsalertsapi.entity.BulkPlanRepository
import uk.gov.justice.digital.hmpps.hmppsalertsapi.entity.PersonSummary
import uk.gov.justice.digital.hmpps.hmppsalertsapi.entity.PersonSummaryRepository
import uk.gov.justice.digital.hmpps.hmppsalertsapi.entity.Status
import uk.gov.justice.digital.hmpps.hmppsalertsapi.entity.getPlan
import uk.gov.justice.digital.hmpps.hmppsalertsapi.entity.toPersonSummary
import uk.gov.justice.digital.hmpps.hmppsalertsapi.enumeration.BulkAlertCleanupMode
import uk.gov.justice.digital.hmpps.hmppsalertsapi.exceptions.InvalidRowException
import uk.gov.justice.digital.hmpps.hmppsalertsapi.model.BulkPlan
import uk.gov.justice.digital.hmpps.hmppsalertsapi.model.BulkPlanAffect
import uk.gov.justice.digital.hmpps.hmppsalertsapi.model.BulkPlanCounts
import uk.gov.justice.digital.hmpps.hmppsalertsapi.model.BulkPlanPrisoners
import uk.gov.justice.digital.hmpps.hmppsalertsapi.model.PrisonerSummary
import uk.gov.justice.digital.hmpps.hmppsalertsapi.model.request.AddPrisonNumbers
import uk.gov.justice.digital.hmpps.hmppsalertsapi.model.request.BulkAction
import uk.gov.justice.digital.hmpps.hmppsalertsapi.model.request.RemovePrisonNumbers
import uk.gov.justice.digital.hmpps.hmppsalertsapi.model.request.SetAlertCode
import uk.gov.justice.digital.hmpps.hmppsalertsapi.model.request.SetCleanupMode
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

  fun getAssociatedPrisoners(id: UUID): BulkPlanPrisoners {
    val plan = planRepository.getPlan(id)
    return BulkPlanPrisoners(plan.people.map { it.asPrisonerSummary() }.toSortedSet())
  }

  fun getPlanAffect(id: UUID): BulkPlanAffect {
    val plan = planRepository.getPlan(id)
    val alertCode = checkNotNull(plan.alertCode) {
      "Unable to calculate affect of plan until the alert code is selected"
    }
    val cleanupMode = checkNotNull(plan.cleanupMode) {
      "Unable to calculate affect of plan until the cleanup mode is selected"
    }
    val counts = planRepository.findPlanAffects(plan.id, alertCode.code).associate { it.status to it.count }
    return BulkPlanAffect(counts.asBulkPlanCounts(cleanupMode))
  }

  fun update(id: UUID, actions: Set<BulkAction>): BulkPlan {
    val plan = planRepository.getPlan(id)
    actions.forEach { action ->
      when (action) {
        is SetAlertCode -> plan.setAlertCode(action)
        is SetDescription -> plan.setDescription(action)
        is AddPrisonNumbers -> plan.addPrisonNumbers(action)
        is RemovePrisonNumbers -> plan.removePrisonNumbers(action)
        is SetCleanupMode -> plan.setCleanupMode(action)
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
    people += action.prisonNumbers.validate()
  }

  private fun Plan.removePrisonNumbers(action: RemovePrisonNumbers) {
    people.removeIf { it.prisonNumber in action.prisonNumbers }
  }

  private fun Plan.setCleanupMode(action: SetCleanupMode) {
    cleanupMode = action.cleanupMode
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
fun PersonSummary.asPrisonerSummary() = PrisonerSummary(prisonNumber, firstName, lastName, prisonCode, cellLocation)
fun Map<Status, Int>.asBulkPlanCounts(cleanupMode: BulkAlertCleanupMode): BulkPlanCounts = BulkPlanCounts(
  getOrDefault(Status.ACTIVE, 0),
  getOrDefault(Status.CREATE, 0),
  getOrDefault(Status.UPDATE, 0),
  when (cleanupMode) {
    BulkAlertCleanupMode.KEEP_ALL -> 0
    BulkAlertCleanupMode.EXPIRE_FOR_PRISON_NUMBERS_NOT_SPECIFIED -> getOrDefault(Status.EXPIRE, 0)
  },
)
