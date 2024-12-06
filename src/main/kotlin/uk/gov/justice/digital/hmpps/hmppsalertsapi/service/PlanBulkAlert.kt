package uk.gov.justice.digital.hmpps.hmppsalertsapi.service

import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.hmppsalertsapi.client.prisonersearch.PrisonerSearchClient
import uk.gov.justice.digital.hmpps.hmppsalertsapi.common.aop.PublishPersonAlertsChanged
import uk.gov.justice.digital.hmpps.hmppsalertsapi.config.AlertRequestContext
import uk.gov.justice.digital.hmpps.hmppsalertsapi.config.AlertRequestContext.Companion.PRISON_NUMBER_REGEX
import uk.gov.justice.digital.hmpps.hmppsalertsapi.entity.Alert
import uk.gov.justice.digital.hmpps.hmppsalertsapi.entity.BulkPlan.Companion.BULK_ALERT_DISPLAY_NAME
import uk.gov.justice.digital.hmpps.hmppsalertsapi.entity.BulkPlan.Companion.BULK_ALERT_USERNAME
import uk.gov.justice.digital.hmpps.hmppsalertsapi.entity.BulkPlanRepository
import uk.gov.justice.digital.hmpps.hmppsalertsapi.entity.PersonSummary
import uk.gov.justice.digital.hmpps.hmppsalertsapi.entity.PersonSummaryRepository
import uk.gov.justice.digital.hmpps.hmppsalertsapi.entity.Status
import uk.gov.justice.digital.hmpps.hmppsalertsapi.entity.Status.ACTIVE
import uk.gov.justice.digital.hmpps.hmppsalertsapi.entity.Status.CREATE
import uk.gov.justice.digital.hmpps.hmppsalertsapi.entity.Status.EXPIRE
import uk.gov.justice.digital.hmpps.hmppsalertsapi.entity.Status.UPDATE
import uk.gov.justice.digital.hmpps.hmppsalertsapi.entity.getPlan
import uk.gov.justice.digital.hmpps.hmppsalertsapi.entity.toPersonSummary
import uk.gov.justice.digital.hmpps.hmppsalertsapi.enumeration.BulkAlertCleanupMode
import uk.gov.justice.digital.hmpps.hmppsalertsapi.enumeration.BulkAlertCleanupMode.EXPIRE_FOR_PRISON_NUMBERS_NOT_SPECIFIED
import uk.gov.justice.digital.hmpps.hmppsalertsapi.enumeration.Source
import uk.gov.justice.digital.hmpps.hmppsalertsapi.exceptions.InvalidRowException
import uk.gov.justice.digital.hmpps.hmppsalertsapi.model.BulkPlan
import uk.gov.justice.digital.hmpps.hmppsalertsapi.model.BulkPlanAffect
import uk.gov.justice.digital.hmpps.hmppsalertsapi.model.BulkPlanCounts
import uk.gov.justice.digital.hmpps.hmppsalertsapi.model.BulkPlanPrisoners
import uk.gov.justice.digital.hmpps.hmppsalertsapi.model.BulkPlanStatus
import uk.gov.justice.digital.hmpps.hmppsalertsapi.model.PrisonerSummary
import uk.gov.justice.digital.hmpps.hmppsalertsapi.model.request.AddPrisonNumbers
import uk.gov.justice.digital.hmpps.hmppsalertsapi.model.request.BulkAction
import uk.gov.justice.digital.hmpps.hmppsalertsapi.model.request.RemovePrisonNumbers
import uk.gov.justice.digital.hmpps.hmppsalertsapi.model.request.SetAlertCode
import uk.gov.justice.digital.hmpps.hmppsalertsapi.model.request.SetCleanupMode
import uk.gov.justice.digital.hmpps.hmppsalertsapi.model.request.SetDescription
import uk.gov.justice.digital.hmpps.hmppsalertsapi.repository.AlertCodeRepository
import uk.gov.justice.digital.hmpps.hmppsalertsapi.repository.AlertRepository
import uk.gov.justice.digital.hmpps.hmppsalertsapi.repository.getByCode
import java.time.LocalDate
import java.util.UUID
import uk.gov.justice.digital.hmpps.hmppsalertsapi.entity.BulkPlan as Plan

@Transactional
@Service
class PlanBulkAlert(
  private val planRepository: BulkPlanRepository,
  private val alertCodeRepository: AlertCodeRepository,
  private val personSummaryRepository: PersonSummaryRepository,
  private val prisonerSearch: PrisonerSearchClient,
  private val alertRepository: AlertRepository,
) {
  fun createNew(): BulkPlan = planRepository.save(Plan()).toModel()

  fun getAssociatedPrisoners(id: UUID): BulkPlanPrisoners {
    val plan = planRepository.getPlan(id)
    return BulkPlanPrisoners(plan.people.map { it.asPrisonerSummary() }.toSortedSet())
  }

  fun getPlanAffect(id: UUID): BulkPlanAffect {
    val plan = planRepository.getPlan(id)
    val (alertCode, cleanupMode) = plan.ready()
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

  @Async
  @PublishPersonAlertsChanged
  fun start(id: UUID, context: AlertRequestContext) {
    val plan = planRepository.getPlan(id)
    val (alertCode, cleanupMode) = plan.ready()
    plan.start(context)
    val existingAlerts = alertRepository.findAllActiveByCode(alertCode.code).associateBy { it.prisonNumber }
    val toAction = plan.people.map {
      val alert = existingAlerts[it.prisonNumber]
      when {
        alert == null -> {
          CREATE to plan.createAlert(it.prisonNumber)
        }

        alert.activeTo == null -> ACTIVE to alert
        else -> {
          UPDATE to
            alert.update(
              alert.description,
              alert.authorisedBy,
              alert.activeFrom,
              null,
              updatedAt = context.requestAt,
              updatedBy = BULK_ALERT_USERNAME,
              updatedByDisplayName = BULK_ALERT_DISPLAY_NAME,
              source = Source.DPS,
              activeCaseLoadId = null,
            )
        }
      }
    }.groupBy({ it.first }, { it.second })

    alertRepository.saveAll(toAction.flatMap { it.value })

    val prisonNumbers = plan.people.map { it.prisonNumber }.toSet()
    val toExpire: List<Alert> = if (cleanupMode == EXPIRE_FOR_PRISON_NUMBERS_NOT_SPECIFIED) {
      val toExpire = existingAlerts.values.filter { it.prisonNumber !in prisonNumbers }.map {
        it.update(
          it.description,
          it.authorisedBy,
          it.activeFrom,
          LocalDate.now(),
          updatedAt = context.requestAt,
          updatedBy = BULK_ALERT_USERNAME,
          updatedByDisplayName = BULK_ALERT_DISPLAY_NAME,
          source = Source.DPS,
          activeCaseLoadId = null,
        )
      }
      alertRepository.saveAll(toExpire)
    } else {
      emptyList()
    }

    val counts = toAction.map { it.key to it.value.size }.toMap() + (EXPIRE to toExpire.size)
    plan.completed(
      counts.getOrDefault(CREATE, 0),
      counts.getOrDefault(UPDATE, 0),
      counts.getOrDefault(ACTIVE, 0),
      counts.getOrDefault(EXPIRE, 0),
    )
  }

  fun status(id: UUID): BulkPlanStatus = planRepository.getPlan(id).status()

  fun findPlansFromPrisonNumber(prisonNumber: String) = planRepository.findPlansWithPrisonNumber(prisonNumber)

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

fun Plan.createAlert(prisonNumber: String): Alert {
  val prisoner = people.first { it.prisonNumber == prisonNumber }
  return Alert(
    alertCode!!,
    prisonNumber,
    description,
    BULK_ALERT_USERNAME,
    LocalDate.now(),
    null,
    startedAt!!,
    prisoner.prisonCode,
  ).apply {
    create(
      description ?: "Alert created",
      createdBy = BULK_ALERT_USERNAME,
      createdByDisplayName = BULK_ALERT_DISPLAY_NAME,
      source = Source.DPS,
      activeCaseLoadId = null,
    )
  }
}

fun Plan.toModel() = BulkPlan(id)
fun PersonSummary.asPrisonerSummary() = PrisonerSummary(prisonNumber, firstName, lastName, prisonCode, cellLocation)
fun Map<Status, Int>.asBulkPlanCounts(cleanupMode: BulkAlertCleanupMode): BulkPlanCounts = BulkPlanCounts(
  getOrDefault(ACTIVE, 0),
  getOrDefault(CREATE, 0),
  getOrDefault(UPDATE, 0),
  when (cleanupMode) {
    BulkAlertCleanupMode.KEEP_ALL -> 0
    EXPIRE_FOR_PRISON_NUMBERS_NOT_SPECIFIED -> getOrDefault(Status.EXPIRE, 0)
  },
)

fun Plan.status() = BulkPlanStatus(
  createdAt,
  createdBy,
  createdByDisplayName,
  startedAt,
  startedBy,
  startedByDisplayName,
  completedAt,
  completedAt?.let { counts() },
)

fun Plan.counts() = BulkPlanCounts(unchangedCount ?: 0, createdCount ?: 0, updatedCount ?: 0, expiredCount ?: 0)
