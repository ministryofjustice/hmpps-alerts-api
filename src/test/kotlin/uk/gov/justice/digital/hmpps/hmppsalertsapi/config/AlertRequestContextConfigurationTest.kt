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
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpServletResponse
import org.springframework.security.access.AccessDeniedException
import org.springframework.security.core.context.SecurityContextHolder
import uk.gov.justice.digital.hmpps.hmppsalertsapi.enumeration.Source
import uk.gov.justice.digital.hmpps.hmppsalertsapi.enumeration.Source.DPS
import uk.gov.justice.digital.hmpps.hmppsalertsapi.enumeration.Source.NOMIS
import uk.gov.justice.digital.hmpps.hmppsalertsapi.integration.wiremock.TEST_USER
import uk.gov.justice.digital.hmpps.hmppsalertsapi.integration.wiremock.TEST_USER_NAME
import uk.gov.justice.digital.hmpps.hmppsalertsapi.integration.wiremock.USER_NOT_FOUND
import uk.gov.justice.digital.hmpps.hmppsalertsapi.resource.SOURCE
import uk.gov.justice.digital.hmpps.hmppsalertsapi.resource.USERNAME
import uk.gov.justice.digital.hmpps.hmppsalertsapi.service.UserService
import uk.gov.justice.digital.hmpps.hmppsalertsapi.utils.userDetailsDto
import uk.gov.justice.hmpps.kotlin.auth.AuthAwareAuthenticationToken
import java.time.temporal.ChronoUnit

class AlertRequestContextConfigurationTest {
  private val userService = mock<UserService>()
  private val interceptor: AlertRequestContextInterceptor = AlertRequestContextInterceptor(userService)

  private val req = MockHttpServletRequest()
  private val res = MockHttpServletResponse()

  @BeforeEach
  fun beforeEach() {
    req.method = "POST"
    SecurityContextHolder.clearContext()
    whenever(userService.getUserDetails(TEST_USER)).thenReturn(userDetailsDto())
  }

  @Test
  fun `uses now for request at`() {
    setSecurityContext(mapOf("user_name" to TEST_USER))

    interceptor.preHandle(req, res, "null")
    val context = req.getAttribute(AlertRequestContext::class.simpleName!!) as AlertRequestContext

    assertThat(context.requestAt).isCloseToUtcNow(within(3, ChronoUnit.SECONDS))
  }

  @Test
  fun `throws IllegalArgumentException when source value is invalid`() {
    req.addHeader(SOURCE, "INVALID")
    val exception = assertThrows<IllegalArgumentException> { interceptor.preHandle(req, res, "null") }
    assertThat(exception.message).isEqualTo("No enum constant uk.gov.justice.digital.hmpps.hmppsalertsapi.enumeration.Source.INVALID")
  }

  @Test
  fun `uses DPS as default value for source`() {
    setSecurityContext(mapOf("user_name" to TEST_USER))

    interceptor.preHandle(req, res, "null")
    val context = req.getAttribute(AlertRequestContext::class.simpleName!!) as AlertRequestContext

    assertThat(context.source).isEqualTo(DPS)
  }

  @ParameterizedTest
  @MethodSource("sourceParameters")
  fun `parses source header as value for source`(sourceValue: String, expectedSource: Source) {
    req.addHeader(SOURCE, sourceValue)
    setSecurityContext(mapOf("user_name" to TEST_USER))

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
  fun `throws ValidationException when username is not supplied`() {
    setSecurityContext(emptyMap())
    val exception = assertThrows<ValidationException> { interceptor.preHandle(req, res, "null") }
    assertThat(exception.message).isEqualTo("Could not find non empty username from user_name or username token claims or Username header")
  }

  @Test
  fun `throws ValidationException when username from user_name claim is empty string`() {
    setSecurityContext(mapOf("user_name" to ""))
    val exception = assertThrows<ValidationException> { interceptor.preHandle(req, res, "null") }
    assertThat(exception.message).isEqualTo("Could not find non empty username from user_name or username token claims or Username header")
  }

  @Test
  fun `throws ValidationException when trimmed username from user_name claim is empty string`() {
    setSecurityContext(mapOf("user_name" to "   "))
    val exception = assertThrows<ValidationException> { interceptor.preHandle(req, res, "null") }
    assertThat(exception.message).isEqualTo("Could not find non empty username from user_name or username token claims or Username header")
  }

  @Test
  fun `throws ValidationException when username from user_name claim is greater than 32 characters`() {
    setSecurityContext(mapOf("user_name" to "a".repeat(33)))
    val exception = assertThrows<ValidationException> { interceptor.preHandle(req, res, "null") }
    assertThat(exception.message).isEqualTo("Created by must be <= 32 characters")
  }

  @Test
  fun `accepts 32 character username from user_name claim`() {
    val username = "a".repeat(32)
    setSecurityContext(mapOf("user_name" to username))
    whenever(userService.getUserDetails(username)).thenReturn(userDetailsDto(username))

    interceptor.preHandle(req, res, "null")
    val context = req.getAttribute(AlertRequestContext::class.simpleName!!) as AlertRequestContext

    assertThat(context.username).isEqualTo(username)
    assertThat(context.userDisplayName).isEqualTo(TEST_USER_NAME)
  }

  @Test
  fun `throws ValidationException when source is DPS and username from user_name claim is not found`() {
    req.addHeader(SOURCE, DPS.name)
    setSecurityContext(mapOf("user_name" to USER_NOT_FOUND))
    val exception = assertThrows<ValidationException> { interceptor.preHandle(req, res, "null") }
    assertThat(exception.message).isEqualTo("User details for supplied username not found")
  }

  @Test
  fun `uses username from user_name claim as display name when source is NOMIS and username is not found`() {
    req.addHeader(SOURCE, NOMIS.name)
    setSecurityContext(mapOf("user_name" to USER_NOT_FOUND))

    interceptor.preHandle(req, res, "null")
    val context = req.getAttribute(AlertRequestContext::class.simpleName!!) as AlertRequestContext

    assertThat(context.username).isEqualTo(USER_NOT_FOUND)
    assertThat(context.userDisplayName).isEqualTo(USER_NOT_FOUND)
  }

  @Test
  fun `takes username from user_name claim`() {
    setSecurityContext(mapOf("user_name" to TEST_USER))

    interceptor.preHandle(req, res, "null")
    val context = req.getAttribute(AlertRequestContext::class.simpleName!!) as AlertRequestContext

    assertThat(context.username).isEqualTo(TEST_USER)
    assertThat(context.userDisplayName).isEqualTo(TEST_USER_NAME)
  }

  @Test
  fun `trims username from user_name claim`() {
    setSecurityContext(mapOf("user_name" to " $TEST_USER  "))

    interceptor.preHandle(req, res, "null")
    val context = req.getAttribute(AlertRequestContext::class.simpleName!!) as AlertRequestContext

    assertThat(context.username).isEqualTo(TEST_USER)
    assertThat(context.userDisplayName).isEqualTo(TEST_USER_NAME)
  }

  @Test
  fun `throws ValidationException when username from username claim is empty string`() {
    setSecurityContext(mapOf("username" to ""))
    val exception = assertThrows<ValidationException> { interceptor.preHandle(req, res, "null") }
    assertThat(exception.message).isEqualTo("Could not find non empty username from user_name or username token claims or Username header")
  }

  @Test
  fun `throws ValidationException when trimmed username from username claim is empty string`() {
    setSecurityContext(mapOf("username" to "   "))
    val exception = assertThrows<ValidationException> { interceptor.preHandle(req, res, "null") }
    assertThat(exception.message).isEqualTo("Could not find non empty username from user_name or username token claims or Username header")
  }

  @Test
  fun `throws ValidationException when username from username claim is greater than 32 characters`() {
    setSecurityContext(mapOf("username" to "a".repeat(33)))
    val exception = assertThrows<ValidationException> { interceptor.preHandle(req, res, "null") }
    assertThat(exception.message).isEqualTo("Created by must be <= 32 characters")
  }

  @Test
  fun `accepts 32 character username from username claim`() {
    val username = "a".repeat(32)
    setSecurityContext(mapOf("username" to username))
    whenever(userService.getUserDetails(username)).thenReturn(userDetailsDto(username))

    interceptor.preHandle(req, res, "null")
    val context = req.getAttribute(AlertRequestContext::class.simpleName!!) as AlertRequestContext

    assertThat(context.username).isEqualTo(username)
    assertThat(context.userDisplayName).isEqualTo(TEST_USER_NAME)
  }

  @Test
  fun `throws ValidationException when source is DPS and username from username claim is not found`() {
    req.addHeader(SOURCE, DPS.name)
    setSecurityContext(mapOf("username" to USER_NOT_FOUND))
    val exception = assertThrows<ValidationException> { interceptor.preHandle(req, res, "null") }
    assertThat(exception.message).isEqualTo("User details for supplied username not found")
  }

  @Test
  fun `uses username from username claim as display name when source is NOMIS and username is not found`() {
    req.addHeader(SOURCE, NOMIS.name)
    setSecurityContext(mapOf("username" to USER_NOT_FOUND))

    interceptor.preHandle(req, res, "null")
    val context = req.getAttribute(AlertRequestContext::class.simpleName!!) as AlertRequestContext

    assertThat(context.username).isEqualTo(USER_NOT_FOUND)
    assertThat(context.userDisplayName).isEqualTo(USER_NOT_FOUND)
  }

  @Test
  fun `takes username from username claim`() {
    setSecurityContext(mapOf("username" to TEST_USER))

    interceptor.preHandle(req, res, "null")
    val context = req.getAttribute(AlertRequestContext::class.simpleName!!) as AlertRequestContext

    assertThat(context.username).isEqualTo(TEST_USER)
    assertThat(context.userDisplayName).isEqualTo(TEST_USER_NAME)
  }

  @Test
  fun `trims username from username claim`() {
    setSecurityContext(mapOf("username" to " $TEST_USER  "))

    interceptor.preHandle(req, res, "null")
    val context = req.getAttribute(AlertRequestContext::class.simpleName!!) as AlertRequestContext

    assertThat(context.username).isEqualTo(TEST_USER)
    assertThat(context.userDisplayName).isEqualTo(TEST_USER_NAME)
  }

  @Test
  fun `throws ValidationException when username from Username header is empty string`() {
    setSecurityContext(emptyMap())
    req.addHeader(USERNAME, "")
    val exception = assertThrows<ValidationException> { interceptor.preHandle(req, res, "null") }
    assertThat(exception.message).isEqualTo("Could not find non empty username from user_name or username token claims or Username header")
  }

  @Test
  fun `throws ValidationException when trimmed username from Username header is empty string`() {
    setSecurityContext(emptyMap())
    req.addHeader(USERNAME, "   ")
    val exception = assertThrows<ValidationException> { interceptor.preHandle(req, res, "null") }
    assertThat(exception.message).isEqualTo("Could not find non empty username from user_name or username token claims or Username header")
  }

  @Test
  fun `throws ValidationException when username from Username header is greater than 32 characters`() {
    setSecurityContext(emptyMap())
    req.addHeader(USERNAME, "a".repeat(33))
    val exception = assertThrows<ValidationException> { interceptor.preHandle(req, res, "null") }
    assertThat(exception.message).isEqualTo("Created by must be <= 32 characters")
  }

  @Test
  fun `accepts 32 character username from Username header`() {
    setSecurityContext(emptyMap())
    val username = "a".repeat(32)
    req.addHeader(USERNAME, username)
    whenever(userService.getUserDetails(username)).thenReturn(userDetailsDto(username))

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
    val exception = assertThrows<ValidationException> { interceptor.preHandle(req, res, "null") }
    assertThat(exception.message).isEqualTo("User details for supplied username not found")
  }

  @Test
  fun `uses username from Username header as display name when source is NOMIS and username is not found`() {
    setSecurityContext(emptyMap())
    req.addHeader(SOURCE, NOMIS.name)
    req.addHeader(USERNAME, USER_NOT_FOUND)

    interceptor.preHandle(req, res, "null")
    val context = req.getAttribute(AlertRequestContext::class.simpleName!!) as AlertRequestContext

    assertThat(context.username).isEqualTo(USER_NOT_FOUND)
    assertThat(context.userDisplayName).isEqualTo(USER_NOT_FOUND)
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
  fun `trims username from Username header`() {
    setSecurityContext(emptyMap())
    req.addHeader(USERNAME, " $TEST_USER  ")

    interceptor.preHandle(req, res, "null")
    val context = req.getAttribute(AlertRequestContext::class.simpleName!!) as AlertRequestContext

    assertThat(context.username).isEqualTo(TEST_USER)
    assertThat(context.userDisplayName).isEqualTo(TEST_USER_NAME)
  }

  @ParameterizedTest
  @ValueSource(strings = ["POST", "PUT", "DELETE"])
  fun `modifying request methods populates context`(method: String) {
    setSecurityContext(mapOf("user_name" to TEST_USER))
    req.method = method

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

  private fun setSecurityContext(claims: Map<String, Any>) =
    mock<AuthAwareAuthenticationToken> { on { tokenAttributes } doReturn claims }.also {
        token ->
      SecurityContextHolder.setContext(mock { on { authentication } doReturn token })
    }
}
