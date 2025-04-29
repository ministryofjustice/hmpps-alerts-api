package uk.gov.justice.digital.hmpps.hmppsalertsapi.exceptions

import java.util.SortedSet

data class InvalidInputException(val name: String, val value: String) : IllegalArgumentException("$name is invalid")

data class InvalidRowException(val rows: SortedSet<Int>) : IllegalArgumentException()
