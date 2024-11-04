package uk.gov.justice.digital.hmpps.hmppsalertsapi.utils

import java.util.concurrent.atomic.AtomicLong

object IdGenerator {
  private val id = AtomicLong(1)
  private val letters = ('A'..'Z')

  fun newId() = id.getAndIncrement()
  fun prisonNumber(): String = "${letters.random()}${(1111..9999).random()}${letters.random()}${letters.random()}"
}
