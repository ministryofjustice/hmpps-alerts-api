package uk.gov.justice.digital.hmpps.hmppsalertsapi.config

import jakarta.validation.ValidationException
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.within
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpServletResponse
import org.springframework.security.access.AccessDeniedException
import org.springframework.security.core.context.SecurityContextHolder
import uk.gov.justice.digital.hmpps.hmppsalertsapi.integration.wiremock.TEST_USER
import uk.gov.justice.digital.hmpps.hmppsalertsapi.integration.wiremock.TEST_USER_NAME
import uk.gov.justice.digital.hmpps.hmppsalertsapi.integration.wiremock.USER_NOT_FOUND
import uk.gov.justice.digital.hmpps.hmppsalertsapi.integration.wiremock.USER_THROW_EXCEPTION
import uk.gov.justice.digital.hmpps.hmppsalertsapi.resource.USERNAME
import uk.gov.justice.digital.hmpps.hmppsalertsapi.service.UserService
import uk.gov.justice.digital.hmpps.hmppsalertsapi.utils.userDetailsDto
import java.time.temporal.ChronoUnit

class AlertRequestContextConfigurationTest {
  private val userService = mock<UserService>()
  private val interceptor: AlertRequestContextInterceptor = AlertRequestContextInterceptor(userService)

  private val req = MockHttpServletRequest()
  private val res = MockHttpServletResponse()

  @BeforeEach
  fun beforeEach() {
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
    assertThat(context.userDisplayName).isEqualTo("First Last")
  }

  @Test
  fun `throws ValidationException when username from user_name claim is not found`() {
    setSecurityContext(mapOf("user_name" to USER_NOT_FOUND))
    val exception = assertThrows<ValidationException> { interceptor.preHandle(req, res, "null") }
    assertThat(exception.message).isEqualTo("User details for supplied username not found")
  }

  @Test
  fun `throws DownstreamServiceException when get user for username from user_name claim call throws exception`() {
    setSecurityContext(mapOf("user_name" to USER_THROW_EXCEPTION))
    val cause = RuntimeException("Test exception")
    whenever(userService.getUserDetails(USER_THROW_EXCEPTION)).thenThrow(cause)
    val exception = assertThrows<DownstreamServiceException> { interceptor.preHandle(req, res, "null") }
    assertThat(exception.message).isEqualTo("Get user details request failed")
    assertThat(exception.cause).isEqualTo(cause)
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
    assertThat(context.userDisplayName).isEqualTo("First Last")
  }

  @Test
  fun `throws ValidationException when username from username claim is not found`() {
    setSecurityContext(mapOf("username" to USER_NOT_FOUND))
    val exception = assertThrows<ValidationException> { interceptor.preHandle(req, res, "null") }
    assertThat(exception.message).isEqualTo("User details for supplied username not found")
  }

  @Test
  fun `throws DownstreamServiceException when get user for username from username claim call throws exception`() {
    setSecurityContext(mapOf("username" to USER_THROW_EXCEPTION))
    val cause = RuntimeException("Test exception")
    whenever(userService.getUserDetails(USER_THROW_EXCEPTION)).thenThrow(cause)
    val exception = assertThrows<DownstreamServiceException> { interceptor.preHandle(req, res, "null") }
    assertThat(exception.message).isEqualTo("Get user details request failed")
    assertThat(exception.cause).isEqualTo(cause)
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
    assertThat(context.userDisplayName).isEqualTo("First Last")
  }

  @Test
  fun `throws ValidationException when username from Username header is not found`() {
    setSecurityContext(emptyMap())
    req.addHeader(USERNAME, USER_NOT_FOUND)
    val exception = assertThrows<ValidationException> { interceptor.preHandle(req, res, "null") }
    assertThat(exception.message).isEqualTo("User details for supplied username not found")
  }

  @Test
  fun `throws DownstreamServiceException when get user for username from Username header call throws exception`() {
    setSecurityContext(emptyMap())
    req.addHeader(USERNAME, USER_THROW_EXCEPTION)
    val cause = RuntimeException("Test exception")
    whenever(userService.getUserDetails(USER_THROW_EXCEPTION)).thenThrow(cause)
    val exception = assertThrows<DownstreamServiceException> { interceptor.preHandle(req, res, "null") }
    assertThat(exception.message).isEqualTo("Get user details request failed")
    assertThat(exception.cause).isEqualTo(cause)
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

  private fun setSecurityContext(claims: Map<String, Any>) =
    mock<AuthAwareAuthenticationToken> { on { tokenAttributes } doReturn claims }.also {
        token ->
      SecurityContextHolder.setContext(mock { on { authentication } doReturn token })
    }
}
