package uk.gov.justice.digital.hmpps.hmppsalertsapi.resource

import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.enums.ParameterIn
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import uk.gov.justice.digital.hmpps.hmppsalertsapi.enumeration.Source

@Parameter(
  name = SOURCE,
  `in` = ParameterIn.HEADER,
  description = "The source of the request. Will default to 'ALERTS_SERVICE' if not supplied" +
    "This value will be assigned to the additionalInformation.source property in published domain events. " +
    "A source value of 'NOMIS' will allow any username value that is less than 32 characters to be supplied. " +
    "If this username is not found, its value will be used for the user display name property. " +
    "A source value of 'MIGRATION' will suppress all domain event publishing.",
  required = false,
  content = [Content(schema = Schema(implementation = Source::class))],
)
@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
internal annotation class SourceHeader
