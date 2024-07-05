package uk.gov.justice.digital.hmpps.hmppsalertsapi.exceptions

data class InvalidInputException(val name: String, val value: String) : IllegalArgumentException("$name is invalid")
