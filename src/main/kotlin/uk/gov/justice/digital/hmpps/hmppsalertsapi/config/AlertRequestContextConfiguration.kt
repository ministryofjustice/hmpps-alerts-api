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
import reactor.core.publisher.Mono
import uk.gov.justice.digital.hmpps.hmppsalertsapi.client.manageusers.dto.UserDetailsDto
import uk.gov.justice.digital.hmpps.hmppsalertsapi.client.prisonersearch.PrisonerSearchClient
import uk.gov.justice.digital.hmpps.hmppsalertsapi.client.prisonersearch.dto.PrisonerDetails
import uk.gov.justice.digital.hmpps.hmppsalertsapi.config.AlertRequestContext.Companion.PRISON_NUMBER_REGEX
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
      .excludePathPatterns("/alerts/inactive")
  }
}

@Configuration
class AlertRequestContextInterceptor(
  private val userService: UserService,
  private val prisonerSearchClient: PrisonerSearchClient,
) : HandlerInterceptor {
  private val getPathPatterns = listOf("/alerts/.*", "/alert-codes/.*", "/alert-types/.*", "/prisoners/.*")
    .map { it.toRegex() }

  override fun preHandle(request: HttpServletRequest, response: HttpServletResponse, handler: Any): Boolean {
    if (arrayOf("POST", "PUT", "PATCH", "DELETE").contains(request.method) || shouldApplyToGetPath(request)) {
      val source = request.getSource()
      val username = request.getUsername(source)
      if (request.method == "POST" && request.servletPath.matches("/prisoners/$PRISON_NUMBER_REGEX/alerts".toRegex())) {
        getUserDetails(username, source).zipWith(request.getPrisoner())
          .doOnNext {
            request.setAttribute(
              AlertRequestContext::class.simpleName,
              AlertRequestContext(
                username = it.t1.username,
                userDisplayName = LanguageFormatUtils.formatDisplayName(it.t1.name),
                activeCaseLoadId = it.t1.activeCaseLoadId,
                source = source,
                prisoner = it.t2,
              ),
            )
          }.block()
      } else {
        getUserDetails(username, source)
          .doOnNext {
            request.setAttribute(
              AlertRequestContext::class.simpleName,
              AlertRequestContext(
                username = it.username,
                userDisplayName = LanguageFormatUtils.formatDisplayName(it.name),
                activeCaseLoadId = it.activeCaseLoadId,
                source = source,
                prisoner = null,
              ),
            )
          }.block()
      }
    }
    return true
  }

  private fun shouldApplyToGetPath(request: HttpServletRequest): Boolean = request.method == "GET" && getPathPatterns.any { request.servletPath.matches(it) }

  private fun HttpServletRequest.getSource(): Source = getHeader(SOURCE)?.let { Source.valueOf(it) } ?: DPS

  private fun authentication(): AuthAwareAuthenticationToken = SecurityContextHolder.getContext().authentication as AuthAwareAuthenticationToken?
    ?: throw AccessDeniedException("User is not authenticated")

  private fun HttpServletRequest.getUsername(source: Source): String? = (getHeader(USERNAME) ?: if (source == NOMIS) null else authentication().name)?.trim()?.also {
    if (it.length > 64) throw ValidationException("Username by must be <= 64 characters")
  }

  private fun getUserDetails(username: String?, source: Source): Mono<UserDetailsDto> = if (username != null && source != NOMIS) {
    userService.getUserDetails(username)
      .switchIfEmpty(Mono.error(ValidationException("User details for supplied username not found")))
  } else {
    val system = Mono.just(
      UserDetailsDto(
        username = SYS_USER,
        active = true,
        name = SYS_DISPLAY_NAME,
        authSource = NOMIS.name,
        userId = SYS_USER,
        activeCaseLoadId = null,
        uuid = null,
      ),
    )
    username?.let { userService.getUserDetails(it).switchIfEmpty(system) } ?: system
  }

  private fun HttpServletRequest.getPrisoner(): Mono<PrisonerDetails> = servletPath.split("/").first { it.matches(PRISON_NUMBER_REGEX.toRegex()) }.let { pn ->
    prisonerSearchClient.getPrisoner(pn).switchIfEmpty(Mono.error(ValidationException("Prison number not found")))
  }
}
