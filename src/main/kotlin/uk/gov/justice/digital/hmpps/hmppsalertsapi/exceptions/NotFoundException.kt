package uk.gov.justice.digital.hmpps.hmppsalertsapi.exceptions

data class NotFoundException(val resource: String, val identifier: String) : RuntimeException("$resource not found")

fun <T, E : RuntimeException> verifyExists(value: T?, exception: () -> E): T {
  return value ?: throw exception()
}
