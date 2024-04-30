package uk.gov.justice.digital.hmpps.hmppsalertsapi.enumeration

import io.swagger.v3.oas.annotations.media.Schema

enum class BulkCreateAlertMode {
  @Schema(
    description = "Add an alert to the people who do not already have an active alert with the supplied code. " +
      "This retains any existing alerts with the selected code maintaining their dates, comments, authorised by etc. " +
      "Note that this option will make any existing alerts that will become active in the future active from today.",
  )
  ADD_MISSING,

  @Schema(
    description = "Replace any existing alerts for the people. This expires any existing active alerts with the " +
      "supplied code and adds a new active alert. This functionally resets all dates, comments, authorised by etc.",
  )
  EXPIRE_AND_REPLACE,
}
