package uk.gov.justice.digital.hmpps.hmppsalertsapi.common

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZonedDateTime

fun LocalDate?.onOrBefore(date: LocalDate) = this != null && this <= date
fun LocalDate?.onOrAfter(date: LocalDate) = this != null && this >= date

val EuropeLondon: ZoneId = ZoneId.of("Europe/London")
fun LocalDateTime.toZoneDateTime(): ZonedDateTime = this.atZone(EuropeLondon)
