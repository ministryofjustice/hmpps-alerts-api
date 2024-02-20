package uk.gov.justice.digital.hmpps.hmppsalertsapi.model

import java.time.ZonedDateTime

data class AlertCode(
  val code: String,
  val description: String,
  val listSequence: Int,
  val isActive: Boolean,
  val createdAt: ZonedDateTime,
  val createdBy: String,
  val modifiedAt: ZonedDateTime?,
  val modifiedBy: String?,
  val deactivatedAt: ZonedDateTime?,
  val deactivatedBy: String?,
)
