package uk.gov.justice.digital.hmpps.hmppsalertsapi.model.request

import jakarta.validation.constraints.Size

interface BulkRequest {
  @get:Size(min = 1, max = 100, message = "Alert code must be supplied and be <= 12 characters")
  val alertCode: String
  val description: String?
  val prisonNumbers: LinkedHashSet<String>
}
