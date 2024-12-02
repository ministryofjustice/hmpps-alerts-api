package uk.gov.justice.digital.hmpps.hmppsalertsapi.model

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
