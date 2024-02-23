package uk.gov.justice.digital.hmpps.hmppsalertsapi.utils

import uk.gov.justice.digital.hmpps.hmppsalertsapi.model.AlertCodeSummary

const val ALERT_CODE_ADULT_AT_RISK = "AAR"
const val ALERT_CODE_SOCIAL_CARE = "AS"
const val ALERT_CODE_VICTIM = "VI"
const val ALERT_CODE_ISOLATED_PRISONER = "VIP"
const val ALERT_CODE_POOR_COPER = "VU"
const val ALERT_CODE_SECURE_ALERT_OCG_NOMINAL = "DOCGM"

const val ALERT_CODE_INACTIVE_COVID_REFUSING_TO_SHIELD = "URS"

fun alertCodeVictimSummary() =
  AlertCodeSummary(
    "V",
    ALERT_CODE_VICTIM,
    "Victim",
    6,
    true,
  )
