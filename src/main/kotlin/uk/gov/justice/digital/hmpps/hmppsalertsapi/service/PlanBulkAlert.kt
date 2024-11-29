package uk.gov.justice.digital.hmpps.hmppsalertsapi.service

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppsalertsapi.entity.BulkPlanRepository
import uk.gov.justice.digital.hmpps.hmppsalertsapi.entity.getPlan
import uk.gov.justice.digital.hmpps.hmppsalertsapi.model.BulkPlan
import uk.gov.justice.digital.hmpps.hmppsalertsapi.model.request.BulkRequest
import java.util.UUID
import uk.gov.justice.digital.hmpps.hmppsalertsapi.entity.BulkPlan as Plan

@Service
class PlanBulkAlert(private val planRepository: BulkPlanRepository) {
  fun createNew(): BulkPlan = planRepository.save(Plan()).toModel()
  fun update(id: UUID, request: BulkRequest): BulkPlan {
    val plan = planRepository.getPlan(id)

    return plan.toModel()
  }
}

fun Plan.toModel() = BulkPlan(id)
