package uk.gov.justice.digital.hmpps.hmppsalertsapi.utils

object IdGenerator {
  private val letters = ('A'..'Z')
  fun prisonNumber(): String = "${letters.random()}${(1111..9999).random()}${letters.random()}${letters.random()}"
  fun cellLocation(): String = "${letters.random()}-${(1..9).random()}-${(111..999).random()}"
}
