package uk.gov.justice.digital.hmpps.hmppsalertsapi.config

import io.opentelemetry.api.trace.Span
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.kotlin.spy
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.verifyNoMoreInteractions
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.ConfigDataApplicationContextInitializer
import org.springframework.context.annotation.Import
import org.springframework.http.HttpHeaders
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpServletResponse
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.TestPropertySource
import org.springframework.test.context.junit.jupiter.SpringExtension
import uk.gov.justice.digital.hmpps.hmppsalertsapi.integration.CLIENT_ID
import uk.gov.justice.digital.hmpps.hmppsalertsapi.integration.JwtAuthHelper

@TestPropertySource(
  properties = [
    "applicationinsights.connection.string=TEST",
  ],
)
@Import(JwtAuthHelper::class, ClientTrackingInterceptor::class, ClientTrackingConfiguration::class)
@ContextConfiguration(initializers = [ConfigDataApplicationContextInitializer::class])
@ActiveProfiles("test")
@ExtendWith(SpringExtension::class)
class ClientTrackingConfigurationTest {
  @Suppress("SpringJavaInjectionPointsAutowiringInspection")
  @Autowired
  private lateinit var interceptor: ClientTrackingInterceptor
  private lateinit var interceptorSpy: ClientTrackingInterceptor

  @Suppress("SpringJavaInjectionPointsAutowiringInspection")
  @Autowired
  private lateinit var jwtAuthHelper: JwtAuthHelper

  private val span = spy(Span.current())

  private val res = MockHttpServletResponse()
  private val req = MockHttpServletRequest()

  @BeforeEach
  fun setUp() {
    interceptorSpy = spy(interceptor)
    whenever(interceptorSpy.getCurrentSpan()).thenReturn(span)
  }

  @Test
  fun `set user attributes only`() {
    val user = "TEST_USER"
    val token = jwtAuthHelper.createJwt(subject = user, user = user, client = null)
    req.addHeader(HttpHeaders.AUTHORIZATION, "Bearer $token")

    interceptorSpy.preHandle(req, res, "null")

    verify(span).setAttribute("username", user)
    verify(span).setAttribute("enduser.id", user)
    verifyNoMoreInteractions(span)
  }

  @Test
  fun `set client id attribute only`() {
    val token = jwtAuthHelper.createJwt(subject = CLIENT_ID, user = null, client = CLIENT_ID)
    req.addHeader(HttpHeaders.AUTHORIZATION, "Bearer $token")

    interceptorSpy.preHandle(req, res, "null")

    verify(span).setAttribute("clientId", CLIENT_ID)
    verifyNoMoreInteractions(span)
  }

  @Test
  fun `set user and client attributes`() {
    val user = "TEST_USER"
    val token = jwtAuthHelper.createJwt(subject = user, user = user, client = CLIENT_ID)
    req.addHeader(HttpHeaders.AUTHORIZATION, "Bearer $token")

    interceptorSpy.preHandle(req, res, "null")

    verify(span).setAttribute("username", user)
    verify(span).setAttribute("enduser.id", user)
    verify(span).setAttribute("clientId", CLIENT_ID)
    verifyNoMoreInteractions(span)
  }

  @Test
  fun `no bearer token causes no attributes to be set`() {
    val token = jwtAuthHelper.createJwt(subject = CLIENT_ID, user = null, client = CLIENT_ID)
    req.addHeader(HttpHeaders.AUTHORIZATION, "Not-Bearer $token")

    interceptorSpy.preHandle(req, res, "null")

    verifyNoInteractions(span)
  }

  @Test
  fun `jwt parse exception causes no attributes to be set`() {
    val token = "invalid"
    req.addHeader(HttpHeaders.AUTHORIZATION, "Bearer $token")

    interceptorSpy.preHandle(req, res, "null")

    verifyNoInteractions(span)
  }
}
