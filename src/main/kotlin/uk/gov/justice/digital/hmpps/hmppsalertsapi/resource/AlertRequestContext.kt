package uk.gov.justice.digital.hmpps.hmppsalertsapi.resource

import jakarta.servlet.http.HttpServletRequest
import uk.gov.justice.digital.hmpps.hmppsalertsapi.config.AlertRequestContext

fun HttpServletRequest.alertRequestContext() = getAttribute(AlertRequestContext::class.simpleName) as AlertRequestContext
