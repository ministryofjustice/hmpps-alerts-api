package uk.gov.justice.digital.hmpps.hmppsalertsapi.config

import jakarta.validation.ValidationException
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.within
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.junit.jupiter.params.provider.ValueSource
import org.mockito.ArgumentMatchers.anyString
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpServletResponse
import org.springframework.security.access.AccessDeniedException
import org.springframework.security.core.context.SecurityContextHolder
import reactor.core.publisher.Mono
import uk.gov.justice.digital.hmpps.hmppsalertsapi.client.prisonersearch.PrisonerSearchClient
import uk.gov.justice.digital.hmpps.hmppsalertsapi.client.prisonersearch.dto.PrisonerDto
import uk.gov.justice.digital.hmpps.hmppsalertsapi.enumeration.Source
import uk.gov.justice.digital.hmpps.hmppsalertsapi.enumeration.Source.DPS
import uk.gov.justice.digital.hmpps.hmppsalertsapi.enumeration.Source.NOMIS
import uk.gov.justice.digital.hmpps.hmppsalertsapi.integration.wiremock.NOMIS_SYS_USER
import uk.gov.justice.digital.hmpps.hmppsalertsapi.integration.wiremock.NOMIS_SYS_USER_DISPLAY_NAME
import uk.gov.justice.digital.hmpps.hmppsalertsapi.integration.wiremock.PRISON_CODE_LEEDS
import uk.gov.justice.digital.hmpps.hmppsalertsapi.integration.wiremock.PRISON_CODE_MOORLANDS
import uk.gov.justice.digital.hmpps.hmppsalertsapi.integration.wiremock.TEST_USER
import uk.gov.justice.digital.hmpps.hmppsalertsapi.integration.wiremock.TEST_USER_NAME
import uk.gov.justice.digital.hmpps.hmppsalertsapi.integration.wiremock.USER_NOT_FOUND
import uk.gov.justice.digital.hmpps.hmppsalertsapi.resource.SOURCE
import uk.gov.justice.digital.hmpps.hmppsalertsapi.resource.USERNAME
import uk.gov.justice.digital.hmpps.hmppsalertsapi.service.UserService
import uk.gov.justice.digital.hmpps.hmppsalertsapi.utils.userDetailsDto
import uk.gov.justice.hmpps.kotlin.auth.AuthAwareAuthenticationToken
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

class AlertRequestContextConfigurationTest {
  private val userService = mock<UserService>()
  private val prisonerSearchClient = mock<PrisonerSearchClient>()
  private val interceptor: AlertRequestContextInterceptor =
    AlertRequestContextInterceptor(userService, prisonerSearchClient)

  private val req = MockHttpServletRequest()
  private val res = MockHttpServletResponse()

  @BeforeEach
  fun beforeEach() {
    req.method = "POST"
    SecurityContextHolder.clearContext()
    whenever(userService.getUserDetails(TEST_USER)).thenReturn(Mono.just(userDetailsDto()))
  }

  @Test
  fun `uses now for request at`() {
    setSecurityContext(mapOf("subject" to TEST_USER))

    interceptor.preHandle(req, res, "null")
    val context = req.getAttribute(AlertRequestContext::class.simpleName!!) as AlertRequestContext

    assertThat(context.requestAt).isCloseTo(LocalDateTime.now(), within(3, ChronoUnit.SECONDS))
  }

  @Test
  fun `throws IllegalArgumentException when source value is invalid`() {
    req.addHeader(SOURCE, "INVALID")
    val exception = assertThrows<IllegalArgumentException> { interceptor.preHandle(req, res, "null") }
    assertThat(exception.message).isEqualTo("No enum constant uk.gov.justice.digital.hmpps.hmppsalertsapi.enumeration.Source.INVALID")
  }

  @Test
  fun `uses DPS as default value for source`() {
    setSecurityContext(mapOf("subject" to TEST_USER))

    interceptor.preHandle(req, res, "null")
    val context = req.getAttribute(AlertRequestContext::class.simpleName!!) as AlertRequestContext

    assertThat(context.source).isEqualTo(DPS)
  }

  @ParameterizedTest
  @MethodSource("sourceParameters")
  fun `parses source header as value for source`(sourceValue: String, expectedSource: Source) {
    req.addHeader(SOURCE, sourceValue)
    setSecurityContext(mapOf("subject" to TEST_USER))

    interceptor.preHandle(req, res, "null")
    val context = req.getAttribute(AlertRequestContext::class.simpleName!!) as AlertRequestContext

    assertThat(context.source).isEqualTo(expectedSource)
  }

  companion object {
    @JvmStatic
    fun sourceParameters(): List<Arguments> = listOf(
      Arguments.of(DPS.name, DPS),
      Arguments.of(NOMIS.name, NOMIS),
    )
  }

  @Test
  fun `throws AccessDeniedException when authentication is null`() {
    SecurityContextHolder.setContext(mock { on { authentication } doReturn null })
    val exception = assertThrows<AccessDeniedException> { interceptor.preHandle(req, res, "null") }
    assertThat(exception.message).isEqualTo("User is not authenticated")
  }

  @Test
  fun `accepts 64 character username from Username header`() {
    setSecurityContext(emptyMap())
    val username = "a".repeat(64)
    req.addHeader(USERNAME, username)
    whenever(userService.getUserDetails(username)).thenReturn(Mono.just(userDetailsDto(username)))

    interceptor.preHandle(req, res, "null")
    val context = req.getAttribute(AlertRequestContext::class.simpleName!!) as AlertRequestContext

    assertThat(context.username).isEqualTo(username)
    assertThat(context.userDisplayName).isEqualTo(TEST_USER_NAME)
  }

  @Test
  fun `throws ValidationException when source is DPS and username from Username header is not found`() {
    setSecurityContext(emptyMap())
    req.addHeader(SOURCE, DPS.name)
    req.addHeader(USERNAME, USER_NOT_FOUND)
    whenever(userService.getUserDetails(anyString())).thenReturn(Mono.empty())

    val exception = assertThrows<ValidationException> { interceptor.preHandle(req, res, "null") }
    assertThat(exception.message).isEqualTo("User details for supplied username not found")
  }

  @Test
  fun `uses username from Username header as display name when source is NOMIS and username is not found`() {
    setSecurityContext(emptyMap())
    req.addHeader(SOURCE, NOMIS.name)
    req.addHeader(USERNAME, USER_NOT_FOUND)
    whenever(userService.getUserDetails(USER_NOT_FOUND)).thenReturn(Mono.empty())

    interceptor.preHandle(req, res, "null")
    val context = req.getAttribute(AlertRequestContext::class.simpleName!!) as AlertRequestContext

    assertThat(context.username).isEqualTo(NOMIS_SYS_USER)
    assertThat(context.userDisplayName).isEqualTo(NOMIS_SYS_USER_DISPLAY_NAME)
  }

  @Test
  fun `does not set active case load id when source is NOMIS and username from Username header is not found`() {
    setSecurityContext(emptyMap())
    req.addHeader(SOURCE, NOMIS.name)
    req.addHeader(USERNAME, USER_NOT_FOUND)
    whenever(userService.getUserDetails(USER_NOT_FOUND)).thenReturn(Mono.empty())

    interceptor.preHandle(req, res, "null")
    val context = req.getAttribute(AlertRequestContext::class.simpleName!!) as AlertRequestContext

    assertThat(context.activeCaseLoadId).isNull()
  }

  @Test
  fun `takes username from Username header`() {
    setSecurityContext(emptyMap())
    req.addHeader(USERNAME, TEST_USER)

    interceptor.preHandle(req, res, "null")
    val context = req.getAttribute(AlertRequestContext::class.simpleName!!) as AlertRequestContext

    assertThat(context.username).isEqualTo(TEST_USER)
    assertThat(context.userDisplayName).isEqualTo(TEST_USER_NAME)
  }

  @Test
  fun `takes active case load id from user associated with Username header`() {
    setSecurityContext(emptyMap())
    req.addHeader(USERNAME, TEST_USER)

    interceptor.preHandle(req, res, "null")
    val context = req.getAttribute(AlertRequestContext::class.simpleName!!) as AlertRequestContext

    assertThat(context.activeCaseLoadId).isEqualTo(PRISON_CODE_MOORLANDS)
  }

  @ParameterizedTest
  @ValueSource(strings = ["POST", "PUT", "DELETE"])
  fun `modifying request methods populates context`(method: String) {
    setSecurityContext(mapOf("subject" to TEST_USER))
    req.method = method
    req.servletPath = "/prisoners/A1234BC/alerts"
    whenever(prisonerSearchClient.getPrisoner("A1234BC")).thenReturn(
      Mono.just(
        PrisonerDto(
          "A1234BC",
          null,
          "Alan",
          null,
          "Brown",
          LocalDate.now().minusYears(30),
          PRISON_CODE_LEEDS,
        ),
      ),
    )

    val result = interceptor.preHandle(req, res, "null")
    val context = req.getAttribute(AlertRequestContext::class.simpleName!!) as AlertRequestContext

    assertThat(result).isTrue()
    assertThat(context).isNotNull()
  }

  @Test
  fun `get request method does not require username populates context`() {
    setSecurityContext(emptyMap())
    req.method = "GET"

    val result = interceptor.preHandle(req, res, "null")
    val context = req.getAttribute(AlertRequestContext::class.simpleName!!) as AlertRequestContext?

    assertThat(result).isTrue()
    assertThat(context).isNull()
  }

  @Test
  fun `uses 'NOMIS' as username and display name when source is NOMIS and username is not supplied`() {
    req.addHeader(SOURCE, NOMIS.name)
    setSecurityContext(emptyMap())

    interceptor.preHandle(req, res, "null")
    val context = req.getAttribute(AlertRequestContext::class.simpleName!!) as AlertRequestContext

    assertThat(context.username).isEqualTo(NOMIS_SYS_USER)
    assertThat(context.userDisplayName).isEqualTo(NOMIS_SYS_USER_DISPLAY_NAME)
  }

  @Test
  fun `does not set active case load id when source is NOMIS and username is not supplied`() {
    req.addHeader(SOURCE, NOMIS.name)
    setSecurityContext(emptyMap())

    interceptor.preHandle(req, res, "null")
    val context = req.getAttribute(AlertRequestContext::class.simpleName!!) as AlertRequestContext

    assertThat(context.activeCaseLoadId).isNull()
  }

  private fun setSecurityContext(claims: Map<String, Any>) =
    mock<AuthAwareAuthenticationToken> { on { tokenAttributes } doReturn claims }.also { token ->
      SecurityContextHolder.setContext(mock { on { authentication } doReturn token })
      claims["subject"]?.also { whenever(token.name) doReturn (it as String) }
    }
}
