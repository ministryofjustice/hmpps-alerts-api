package uk.gov.justice.digital.hmpps.hmppsalertsapi.model

import java.time.LocalDateTime
import java.util.SequencedSet
import java.util.UUID

data class BulkPlan(val id: UUID)
data class BulkPlanPrisoners(val prisoners: SequencedSet<PrisonerSummary>)
data class PrisonerSummary(
  val prisonNumber: String,
  val firstName: String,
  val lastName: String,
  val prisonCode: String?,
  val cellLocation: String?,
) : Comparable<PrisonerSummary> {
  override fun compareTo(other: PrisonerSummary): Int {
    var res = lastName.compareTo(other.lastName)
    if (res == 0) {
      res = firstName.compareTo(other.firstName)
    }
    if (res == 0) {
      res = prisonNumber.compareTo(other.prisonNumber)
    }
    return res
  }
}

data class BulkPlanAffect(val counts: BulkPlanCounts)
data class BulkPlanCounts(val existingAlerts: Int, val created: Int, val updated: Int, val expired: Int)

data class BulkPlanStatus(
  val createdAt: LocalDateTime?,
  val createdBy: String?,
  var createdByDisplayName: String?,
  val startedAt: LocalDateTime?,
  val startedBy: String?,
  var startedByDisplayName: String?,
  val completedAt: LocalDateTime?,
  val counts: BulkPlanCounts?,
)
