package uk.gov.justice.digital.hmpps.hmppsalertsapi.exceptions

import java.util.SortedSet

data class InvalidRowResponse(
  val message: String,
  val invalidRows: SortedSet<Int>,
)
