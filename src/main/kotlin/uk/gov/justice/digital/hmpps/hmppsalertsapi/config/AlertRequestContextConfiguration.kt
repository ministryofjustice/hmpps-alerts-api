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
import uk.gov.justice.digital.hmpps.hmppsalertsapi.client.manageusers.dto.UserDetailsDto
import uk.gov.justice.digital.hmpps.hmppsalertsapi.enumeration.Source
import uk.gov.justice.digital.hmpps.hmppsalertsapi.enumeration.Source.DPS
import uk.gov.justice.digital.hmpps.hmppsalertsapi.resource.SOURCE
import uk.gov.justice.digital.hmpps.hmppsalertsapi.resource.USERNAME
import uk.gov.justice.digital.hmpps.hmppsalertsapi.service.UserService
import uk.gov.justice.digital.hmpps.hmppsalertsapi.utils.LanguageFormatUtils
import uk.gov.justice.hmpps.kotlin.auth.AuthAwareAuthenticationToken

@Configuration
class AlertRequestContextConfiguration(private val alertRequestContextInterceptor: AlertRequestContextInterceptor) : WebMvcConfigurer {
  override fun addInterceptors(registry: InterceptorRegistry) {
    log.info("Adding alert request context interceptor")
    registry.addInterceptor(alertRequestContextInterceptor).addPathPatterns("/alerts/**")
    registry.addInterceptor(alertRequestContextInterceptor).addPathPatterns("/prisoners/**/alerts/**")
    registry.addInterceptor(alertRequestContextInterceptor).addPathPatterns("/alert-**/**")
    registry.addInterceptor(alertRequestContextInterceptor).addPathPatterns("/bulk-alerts/**")
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
    if (arrayOf("POST", "PUT", "PATCH", "DELETE").contains(request.method)) {
      val source = request.getSource()
      val userDetails = request.getUserDetails(source)

      request.setAttribute(
        AlertRequestContext::class.simpleName,
        AlertRequestContext(
          source = source,
          username = userDetails.username,
          userDisplayName = LanguageFormatUtils.formatDisplayName(userDetails.name),
          activeCaseLoadId = userDetails.activeCaseLoadId,
        ),
      )
    }

    return true
  }

  private fun HttpServletRequest.getSource(): Source =
    getHeader(SOURCE)?.let { Source.valueOf(it) } ?: DPS

  private fun authentication(): AuthAwareAuthenticationToken =
    SecurityContextHolder.getContext().authentication as AuthAwareAuthenticationToken?
      ?: throw AccessDeniedException("User is not authenticated")

  private fun getUsernameFromClaim(): String? =
    authentication().let {
      it.tokenAttributes["user_name"] as String?
        ?: it.tokenAttributes["username"] as String?
    }

  private fun HttpServletRequest.getUsername(source: Source): String =
    (getUsernameFromClaim() ?: getHeader(USERNAME))
      ?.trim()?.takeUnless(String::isBlank)?.also { if (it.length > 32) throw ValidationException("Created by must be <= 32 characters") }
      ?: if (source != DPS) {
        source.name
      } else {
        throw ValidationException("Could not find non empty username from user_name or username token claims or Username header")
      }

  private fun HttpServletRequest.getUserDetails(source: Source) =
    getUsername(source).let {
      userService.getUserDetails(it)
        ?: if (source != DPS) {
          UserDetailsDto(username = it, active = true, name = it, authSource = it, userId = it, activeCaseLoadId = null, uuid = null)
        } else {
          null
        }
    } ?: throw ValidationException("User details for supplied username not found")
}
