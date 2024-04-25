package uk.gov.justice.digital.hmpps.hmppsalertsapi.enumeration

import io.swagger.v3.oas.annotations.media.Schema

enum class BulkCreateAlertCleanupMode {
  @Schema(
    description = "Keep all alerts for other people. Active alerts with the supplied code associated with people not " +
      "in the supplied list of prison numbers will be unaffected.",
  )
  KEEP_ALL,

  @Schema(
    description = "Remove any alerts with the supplied code for other people. Active alerts with the supplied code " +
      "associated with people not in the supplied list of prison numbers will be made inactive.",
  )
  EXPIRE_FOR_PRISON_NUMBERS_NOT_SPECIFIED,
}
