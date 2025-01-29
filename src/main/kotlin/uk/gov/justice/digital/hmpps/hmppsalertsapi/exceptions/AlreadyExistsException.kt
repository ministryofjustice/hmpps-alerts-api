package uk.gov.justice.digital.hmpps.hmppsalertsapi.exceptions

data class AlreadyExistsException(val resource: String, val identifier: String) : RuntimeException("$resource already exists")

fun <T, E : RuntimeException> verifyDoesNotExist(value: T?, exception: () -> E) {
  if (value != null) throw exception()
}
