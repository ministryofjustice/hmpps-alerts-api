package uk.gov.justice.digital.hmpps.hmppsalertsapi.config

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.ConfigDataApplicationContextInitializer
import org.springframework.context.annotation.Import
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpServletResponse
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.junit.jupiter.SpringExtension
import uk.gov.justice.digital.hmpps.hmppsalertsapi.integration.JwtAuthHelper
import uk.gov.justice.digital.hmpps.hmppsalertsapi.resource.SYNC_SUPPRESS_EVENTS

@Import(JwtAuthHelper::class, SyncContextInterceptor::class, SyncContextConfiguration::class)
@ContextConfiguration(initializers = [ConfigDataApplicationContextInitializer::class])
@ActiveProfiles("test")
@ExtendWith(SpringExtension::class)
class SyncContextConfigurationTest {
  @Suppress("SpringJavaInjectionPointsAutowiringInspection")
  @Autowired
  private lateinit var interceptor: SyncContextInterceptor

  private val req = MockHttpServletRequest()
  private val res = MockHttpServletResponse()

  @Test
  fun `populate sync context`() {
    req.addHeader(SYNC_SUPPRESS_EVENTS, "true")

    interceptor.preHandle(req, res, "null")

    val syncContext = req.getAttribute(SyncContext::class.simpleName!!) as SyncContext

    assertThat(syncContext.suppressEvents).isTrue()
  }

  @Test
  fun `suppress events is optional`() {
    interceptor.preHandle(req, res, "null")

    val syncContext = req.getAttribute(SyncContext::class.simpleName!!) as SyncContext

    assertThat(syncContext.suppressEvents).isFalse()
  }

  @Test
  fun `empty suppress events converted to false`() {
    req.addHeader(SYNC_SUPPRESS_EVENTS, "")

    interceptor.preHandle(req, res, "null")

    val syncContext = req.getAttribute(SyncContext::class.simpleName!!) as SyncContext

    assertThat(syncContext.suppressEvents).isFalse()
  }

  @Test
  fun `whitespace suppress events converted to false`() {
    req.addHeader(SYNC_SUPPRESS_EVENTS, "    ")

    interceptor.preHandle(req, res, "null")

    val syncContext = req.getAttribute(SyncContext::class.simpleName!!) as SyncContext

    assertThat(syncContext.suppressEvents).isFalse()
  }

  @Test
  fun `suppress events is trimmed`() {
    req.addHeader(SYNC_SUPPRESS_EVENTS, " true  ")

    interceptor.preHandle(req, res, "null")

    val syncContext = req.getAttribute(SyncContext::class.simpleName!!) as SyncContext

    assertThat(syncContext.suppressEvents).isTrue()
  }

  @Test
  fun `suppress events is case insensitive`() {
    req.addHeader(SYNC_SUPPRESS_EVENTS, "TrUe")

    interceptor.preHandle(req, res, "null")

    val syncContext = req.getAttribute(SyncContext::class.simpleName!!) as SyncContext

    assertThat(syncContext.suppressEvents).isTrue()
  }
}
