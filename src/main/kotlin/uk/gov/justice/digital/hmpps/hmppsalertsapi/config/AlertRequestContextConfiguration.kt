package uk.gov.justice.digital.hmpps.hmppsalertsapi.config

import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import jakarta.validation.ValidationException
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Configuration
import org.springframework.security.access.AccessDeniedException
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.servlet.HandlerInterceptor
import org.springframework.web.servlet.config.annotation.InterceptorRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer
import uk.gov.justice.digital.hmpps.hmppsalertsapi.resource.USERNAME
import uk.gov.justice.digital.hmpps.hmppsalertsapi.service.UserService
import java.util.*

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
class AlertRequestContextInterceptor(
  private val userService: UserService,
) : HandlerInterceptor {
  override fun preHandle(request: HttpServletRequest, response: HttpServletResponse, handler: Any): Boolean {
    val userDetails = request.getUserDetails()

    request.setAttribute(
      AlertRequestContext::class.simpleName,
      AlertRequestContext(
        username = userDetails.username,
        userDisplayName = userDetails.name,
      ),
    )

    return true
  }

  private fun authentication(): AuthAwareAuthenticationToken =
    SecurityContextHolder.getContext().authentication as AuthAwareAuthenticationToken?
      ?: throw AccessDeniedException("User is not authenticated")

  private fun getUsernameFromClaim(): String? =
    authentication().let {
      it.tokenAttributes["user_name"] as String?
        ?: it.tokenAttributes["username"] as String?
    }?.trim()?.takeUnless(String::isBlank)

  private fun HttpServletRequest.getUsername(): String =
    getUsernameFromClaim()
      ?: getHeader(USERNAME)?.trim()?.takeUnless(String::isBlank)?.also { if (it.length > 33) throw ValidationException("Created by must be <= 32 characters") }
      ?: throw ValidationException("Could not find non empty username from user_name or username token claims or Username header")

  private fun HttpServletRequest.getUserDetails() =
    getUsername().let { username ->
      try {
        userService.getUserDetails(username)
      } catch (e: Exception) {
        throw DownstreamServiceException("Get user details request failed", e)
      } ?: throw ValidationException("User details for supplied username not found")
    }
}
