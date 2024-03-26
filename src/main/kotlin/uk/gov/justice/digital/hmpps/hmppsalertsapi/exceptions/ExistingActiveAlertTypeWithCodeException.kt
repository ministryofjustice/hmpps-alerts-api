package uk.gov.justice.digital.hmpps.hmppsalertsapi.exceptions

data class ExistingActiveAlertTypeWithCodeException(val code: String) : Exception(code)
