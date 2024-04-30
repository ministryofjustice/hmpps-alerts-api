package uk.gov.justice.digital.hmpps.hmppsalertsapi.service

import com.fasterxml.jackson.databind.ObjectMapper
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import uk.gov.justice.digital.hmpps.hmppsalertsapi.client.prisonersearch.PrisonerSearchClient
import uk.gov.justice.digital.hmpps.hmppsalertsapi.config.AlertRequestContext
import uk.gov.justice.digital.hmpps.hmppsalertsapi.integration.wiremock.PRISON_CODE_LEEDS
import uk.gov.justice.digital.hmpps.hmppsalertsapi.integration.wiremock.TEST_USER
import uk.gov.justice.digital.hmpps.hmppsalertsapi.integration.wiremock.TEST_USER_NAME
import uk.gov.justice.digital.hmpps.hmppsalertsapi.repository.AlertCodeRepository
import uk.gov.justice.digital.hmpps.hmppsalertsapi.repository.AlertRepository
import uk.gov.justice.digital.hmpps.hmppsalertsapi.repository.BulkAlertRepository
import uk.gov.justice.digital.hmpps.hmppsalertsapi.utils.bulkCreateAlertRequest
import uk.gov.justice.digital.hmpps.hmppsalertsapi.utils.testObjectMapper

@ExtendWith(MockitoExtension::class)
class BulkAlertServiceTest {
  @Mock
  lateinit var alertCodeRepository: AlertCodeRepository

  @Mock
  lateinit var alertRepository: AlertRepository

  @Mock
  lateinit var bulkAlertRepository: BulkAlertRepository

  @Mock
  lateinit var prisonerSearchClient: PrisonerSearchClient

  @Mock
  val objectMapper: ObjectMapper = testObjectMapper()

  @InjectMocks
  lateinit var underTest: BulkAlertService

  private val context = AlertRequestContext(
    username = TEST_USER,
    userDisplayName = TEST_USER_NAME,
    activeCaseLoadId = PRISON_CODE_LEEDS,
  )

  @Test
  fun `batch size must be greater than zero`() {
    val exception = assertThrows<IllegalArgumentException> {
      underTest.bulkCreateAlerts(bulkCreateAlertRequest(), context, 0)
    }
    assertThat(exception.message).isEqualTo("Batch size must be between 1 and 1000")
  }

  @Test
  fun `batch size must be less than 1001`() {
    val exception = assertThrows<IllegalArgumentException> {
      underTest.bulkCreateAlerts(bulkCreateAlertRequest(), context, 1001)
    }
    assertThat(exception.message).isEqualTo("Batch size must be between 1 and 1000")
  }
}
