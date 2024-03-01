package uk.gov.justice.digital.hmpps.hmppsalertsapi.common

import java.time.LocalDate

fun LocalDate?.onOrBefore(date: LocalDate) = this != null && this <= date
fun LocalDate?.onOrAfter(date: LocalDate) = this != null && this >= date
