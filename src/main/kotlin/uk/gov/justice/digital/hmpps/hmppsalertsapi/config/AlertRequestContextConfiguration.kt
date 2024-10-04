package uk.gov.justice.digital.hmpps.hmppsalertsapi.config

import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import jakarta.validation.ValidationException
import org.springframework.context.annotation.Configuration
import org.springframework.security.access.AccessDeniedException
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.servlet.HandlerInterceptor
import org.springframework.web.servlet.config.annotation.InterceptorRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer
import uk.gov.justice.digital.hmpps.hmppsalertsapi.client.manageusers.dto.UserDetailsDto
import uk.gov.justice.digital.hmpps.hmppsalertsapi.config.AlertRequestContext.Companion.SYS_DISPLAY_NAME
import uk.gov.justice.digital.hmpps.hmppsalertsapi.config.AlertRequestContext.Companion.SYS_USER
import uk.gov.justice.digital.hmpps.hmppsalertsapi.enumeration.Source
import uk.gov.justice.digital.hmpps.hmppsalertsapi.enumeration.Source.DPS
import uk.gov.justice.digital.hmpps.hmppsalertsapi.enumeration.Source.NOMIS
import uk.gov.justice.digital.hmpps.hmppsalertsapi.resource.SOURCE
import uk.gov.justice.digital.hmpps.hmppsalertsapi.resource.USERNAME
import uk.gov.justice.digital.hmpps.hmppsalertsapi.service.UserService
import uk.gov.justice.digital.hmpps.hmppsalertsapi.utils.LanguageFormatUtils
import uk.gov.justice.hmpps.kotlin.auth.AuthAwareAuthenticationToken

@Configuration
class AlertRequestContextConfiguration(
  private val alertRequestContextInterceptor: AlertRequestContextInterceptor,
) : WebMvcConfigurer {
  override fun addInterceptors(registry: InterceptorRegistry) {
    registry.addInterceptor(alertRequestContextInterceptor)
      .addPathPatterns("/alerts/**")
      .addPathPatterns("/prisoners/**/alerts/**")
      .addPathPatterns("/alert-**/**")
      .addPathPatterns("/bulk-alerts/**")
  }
}

@Configuration
class AlertRequestContextInterceptor(
  private val userService: UserService,
) : HandlerInterceptor {
  override fun preHandle(request: HttpServletRequest, response: HttpServletResponse, handler: Any): Boolean {
    if (arrayOf("POST", "PUT", "PATCH", "DELETE").contains(request.method)) {
      val source = request.getSource()
      val username = request.getUsername(source)
      val userDetails = getUserDetails(username, source)
      request.setAttribute(
        AlertRequestContext::class.simpleName,
        AlertRequestContext(
          username = userDetails.username,
          userDisplayName = LanguageFormatUtils.formatDisplayName(userDetails.name),
          activeCaseLoadId = userDetails.activeCaseLoadId,
          source = source,
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

  private fun HttpServletRequest.getUsername(source: Source): String? =
    (getHeader(USERNAME) ?: if (source == NOMIS) null else authentication().name)?.trim()?.also {
      if (it.length > 64) throw ValidationException("Username by must be <= 64 characters")
    }

  private fun getUserDetails(username: String?, source: Source): UserDetailsDto {
    return if (username != null && source != NOMIS) {
      userService.getUserDetails(username) ?: throw ValidationException("User details for supplied username not found")
    } else {
      username?.let { userService.getUserDetails(it) }
        ?: UserDetailsDto(
          username = SYS_USER,
          active = true,
          name = SYS_DISPLAY_NAME,
          authSource = NOMIS.name,
          userId = SYS_USER,
          activeCaseLoadId = null,
          uuid = null,
        )
    }
  }
}
