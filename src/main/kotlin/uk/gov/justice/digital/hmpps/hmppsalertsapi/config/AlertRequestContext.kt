package uk.gov.justice.digital.hmpps.hmppsalertsapi.config

import org.springframework.web.context.request.RequestContextHolder
import uk.gov.justice.digital.hmpps.hmppsalertsapi.enumeration.Source
import uk.gov.justice.digital.hmpps.hmppsalertsapi.enumeration.Source.DPS
import java.time.LocalDateTime

data class AlertRequestContext(
  val username: String,
  val userDisplayName: String,
  val activeCaseLoadId: String? = null,
  val source: Source = DPS,
  val requestAt: LocalDateTime = LocalDateTime.now(),
) {
  companion object {
    const val SYS_USER = "SYS"
    const val SYS_DISPLAY_NAME = "Sys"

    fun get(): AlertRequestContext = RequestContextHolder.getRequestAttributes()
      ?.getAttribute(AlertRequestContext::class.simpleName!!, 0) as AlertRequestContext?
      ?: let {
        val context = AlertRequestContext(username = SYS_USER, userDisplayName = SYS_DISPLAY_NAME)
        RequestContextHolder.getRequestAttributes()?.setAttribute(AlertRequestContext::class.simpleName!!, context, 0)
        context
      }
  }
}
