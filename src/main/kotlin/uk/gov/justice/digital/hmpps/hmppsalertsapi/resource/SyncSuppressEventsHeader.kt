package uk.gov.justice.digital.hmpps.hmppsalertsapi.resource

import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.enums.ParameterIn
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema

@Parameter(
  name = SYNC_SUPPRESS_EVENTS,
  `in` = ParameterIn.HEADER,
  description = "Prevents domain events being published if set to true. This should only be used when migrating existing " +
    "data from NOMIS to the new service. The value of this header will default to false if not provided.",
  required = false,
  content = [Content(schema = Schema(implementation = String::class))],
)
@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
internal annotation class SyncSuppressEventsHeader
