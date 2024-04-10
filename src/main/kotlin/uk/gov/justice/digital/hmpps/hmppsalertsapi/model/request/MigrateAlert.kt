package uk.gov.justice.digital.hmpps.hmppsalertsapi.model.request

data class MigrateAlert(
  val offenderBookId: Int,
  val bookingSeq: Int,
  val alertSeq: Int,
)
