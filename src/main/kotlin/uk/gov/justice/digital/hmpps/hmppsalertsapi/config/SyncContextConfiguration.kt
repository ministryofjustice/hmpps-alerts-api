package uk.gov.justice.digital.hmpps.hmppsalertsapi.config

import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Configuration
import org.springframework.web.servlet.HandlerInterceptor
import org.springframework.web.servlet.config.annotation.InterceptorRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer
import uk.gov.justice.digital.hmpps.hmppsalertsapi.resource.SYNC_SUPPRESS_EVENTS

@Configuration
class SyncContextConfiguration(
  private val syncContextInterceptor: SyncContextInterceptor,
) : WebMvcConfigurer {
  override fun addInterceptors(registry: InterceptorRegistry) {
    log.info("Adding sync context interceptor")
    registry.addInterceptor(syncContextInterceptor).addPathPatterns("/nomis-alerts/**")
  }

  private companion object {
    val log = LoggerFactory.getLogger(this::class.java)
  }
}

@Configuration
class SyncContextInterceptor : HandlerInterceptor {
  override fun preHandle(request: HttpServletRequest, response: HttpServletResponse, handler: Any): Boolean {
    val suppressEvents = request.getHeader(SYNC_SUPPRESS_EVENTS)?.trim()?.toBoolean() ?: false

    request.setAttribute(SyncContext::class.simpleName, SyncContext(suppressEvents))

    return true
  }
}
