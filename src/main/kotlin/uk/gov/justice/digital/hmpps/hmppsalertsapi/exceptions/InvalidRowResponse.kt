package uk.gov.justice.digital.hmpps.hmppsalertsapi.exceptions

import java.util.SortedSet

data class InvalidRowResponse(
  val userMessage: String,
  val invalidRows: SortedSet<Int>,
)
