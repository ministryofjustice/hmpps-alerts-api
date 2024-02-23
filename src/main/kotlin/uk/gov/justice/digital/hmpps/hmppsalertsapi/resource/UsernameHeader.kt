package uk.gov.justice.digital.hmpps.hmppsalertsapi.resource

import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.enums.ParameterIn
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema

@Parameter(
  name = USERNAME,
  `in` = ParameterIn.HEADER,
  description = "The username of the user interacting with the client service. " +
    "This can be used instead of the `user_name` or `username` token claim when the client service is acting on behalf of a user. " +
    "The value passed in the username header will only be used if a `user_name` or `username` token claim is not present.",
  required = false,
  content = [Content(schema = Schema(implementation = String::class))],
)
@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
internal annotation class UsernameHeader
