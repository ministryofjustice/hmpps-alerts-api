package uk.gov.justice.digital.hmpps.hmppsalertsapi.config

import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Configuration
import org.springframework.security.access.AccessDeniedException
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.servlet.HandlerInterceptor
import org.springframework.web.servlet.config.annotation.InterceptorRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer

@Configuration
class AlertRequestContextConfiguration(private val alertRequestContextInterceptor: AlertRequestContextInterceptor) : WebMvcConfigurer {
  override fun addInterceptors(registry: InterceptorRegistry) {
    log.info("Adding alert request context interceptor")
    registry.addInterceptor(alertRequestContextInterceptor).addPathPatterns("/alerts/**")
  }

  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }
}

@Configuration
class AlertRequestContextInterceptor : HandlerInterceptor {
  override fun preHandle(request: HttpServletRequest, response: HttpServletResponse, handler: Any): Boolean {
    val username = getUsername()
    val userDisplayName = getUserDisplayName(username)
    // TODO: Add user's caseload to request context to authorise access to alerts via the prison the person is resident in

    request.setAttribute(
      AlertRequestContext::class.simpleName,
      AlertRequestContext(
        username = username,
        userDisplayName = userDisplayName,
      ),
    )

    return true
  }

  private fun authentication(): AuthAwareAuthenticationToken =
    SecurityContextHolder.getContext().authentication as AuthAwareAuthenticationToken?
      ?: throw AccessDeniedException("User is not authenticated")

  private fun getUsername(): String =
    authentication().let {
      it.tokenAttributes["user_name"] as String?
        ?: it.tokenAttributes["username"] as String?
        ?: throw AccessDeniedException("Token does not contain user_name or username")
    }

  private fun getUserDisplayName(username: String): String =
    // TODO: get user from API call
    "GET_FROM_API_CALL"
}
