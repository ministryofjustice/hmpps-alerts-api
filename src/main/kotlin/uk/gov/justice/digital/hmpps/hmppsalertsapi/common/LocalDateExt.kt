package uk.gov.justice.digital.hmpps.hmppsalertsapi.common

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

fun LocalDate?.onOrBefore(date: LocalDate) = this != null && this <= date
fun LocalDate?.onOrAfter(date: LocalDate) = this != null && this >= date

val EuropeLondon: ZoneId = ZoneId.of("Europe/London")
fun LocalDateTime.toZoneDateTime() = this.atZone(EuropeLondon)
