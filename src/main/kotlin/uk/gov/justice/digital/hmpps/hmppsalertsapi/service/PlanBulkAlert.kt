package uk.gov.justice.digital.hmpps.hmppsalertsapi.service

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppsalertsapi.entity.BulkPlanRepository
import uk.gov.justice.digital.hmpps.hmppsalertsapi.model.BulkPlan
import uk.gov.justice.digital.hmpps.hmppsalertsapi.entity.BulkPlan as Plan

@Service
class PlanBulkAlert(private val bulkPlanRepository: BulkPlanRepository) {
  fun createNewPlan(): BulkPlan = bulkPlanRepository.save(Plan()).toModel()
}

fun Plan.toModel() = BulkPlan(id)
