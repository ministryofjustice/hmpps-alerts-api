package uk.gov.justice.digital.hmpps.hmppsalertsapi.client.prisonersearch

import com.github.tomakehurst.wiremock.client.WireMock.exactly
import com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor
import com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor
import com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.WebClientResponseException
import uk.gov.justice.digital.hmpps.hmppsalertsapi.client.prisonersearch.dto.PrisonerDetails
import uk.gov.justice.digital.hmpps.hmppsalertsapi.config.DownstreamServiceException
import uk.gov.justice.digital.hmpps.hmppsalertsapi.integration.wiremock.PRISON_NUMBER_NOT_FOUND
import uk.gov.justice.digital.hmpps.hmppsalertsapi.integration.wiremock.PRISON_NUMBER_NULL_RESPONSE
import uk.gov.justice.digital.hmpps.hmppsalertsapi.integration.wiremock.PRISON_NUMBER_THROW_EXCEPTION
import uk.gov.justice.digital.hmpps.hmppsalertsapi.integration.wiremock.PrisonerSearchServer

class PrisonerSearchClientTest {
  private lateinit var client: PrisonerSearchClient

  @BeforeEach
  fun resetMocks() {
    server.resetRequests()
    val webClient = WebClient.create("http://localhost:${server.port()}")
    client = PrisonerSearchClient(webClient)
  }

  @Test
  fun `getPrisoner - success`() {
    val prisonNumber = "C1234TS"
    val prisonCode = "SWI"
    server.stubGetPrisoner(prisonNumber, prisonCode)

    val result = client.getPrisoner(prisonNumber).block()

    assertThat(result!!).isEqualTo(
      PrisonerDetails(
        prisonerNumber = prisonNumber,
        "First",
        "Middle",
        "Last",
        prisonId = prisonCode,
        status = "ACTIVE IN",
        restrictedPatient = false,
        cellLocation = null,
        supportingPrisonId = null,
      ),
    )
    server.verify(exactly(1), getRequestedFor(urlEqualTo("/prisoner/$prisonNumber")))
  }

  @Test
  fun `getPrisoner - prisoner not found`() {
    val result = client.getPrisoner(PRISON_NUMBER_NOT_FOUND).block()

    assertThat(result).isNull()
    server.verify(exactly(1), getRequestedFor(urlEqualTo("/prisoner/$PRISON_NUMBER_NOT_FOUND")))
  }

  @Test
  fun `getPrisoner - downstream service exception`() {
    server.stubGetPrisonerException()

    val exception =
      assertThrows<DownstreamServiceException> { client.getPrisoner(PRISON_NUMBER_THROW_EXCEPTION).block() }
    assertThat(exception.message).isEqualTo("Get prisoner request failed")
    with(exception.cause) {
      assertThat(this).isInstanceOf(WebClientResponseException::class.java)
      assertThat(this!!.message).isEqualTo("500 Internal Server Error from GET http://localhost:8112/prisoner/${PRISON_NUMBER_THROW_EXCEPTION}")
    }
    server.verify(exactly(4), getRequestedFor(urlEqualTo("/prisoner/$PRISON_NUMBER_THROW_EXCEPTION")))
  }

  @Test
  fun `getPrisoners - batch size must be greater than zero`() {
    val exception = assertThrows<IllegalArgumentException> {
      client.getPrisoners(emptyList(), 0)
    }
    assertThat(exception.message).isEqualTo("Batch size must be between 1 and 1000")
  }

  @Test
  fun `getPrisoners - batch size must be less than 1001`() {
    val exception = assertThrows<IllegalArgumentException> {
      client.getPrisoners(emptyList(), 1001)
    }
    assertThat(exception.message).isEqualTo("Batch size must be between 1 and 1000")
  }

  @Test
  fun `getPrisoners - no prison numbers`() {
    assertThat(client.getPrisoners(emptyList())).isEmpty()
  }

  @Test
  fun `getPrisoners - not found`() {
    server.stubGetPrisonersNotFound()
    assertThat(client.getPrisoners(listOf(PRISON_NUMBER_NOT_FOUND))).isEmpty()
    server.verify(exactly(1), postRequestedFor(urlEqualTo("/prisoner-search/prisoner-numbers")))
  }

  @Test
  fun `getPrisoners - null response`() {
    server.stubGetPrisonersNullResponse()
    assertThat(client.getPrisoners(listOf(PRISON_NUMBER_NULL_RESPONSE))).isEmpty()
    server.verify(exactly(1), postRequestedFor(urlEqualTo("/prisoner-search/prisoner-numbers")))
  }

  @Test
  fun `getPrisoners - downstream service exception`() {
    server.stubGetPrisonersException()

    val exception =
      assertThrows<DownstreamServiceException> { client.getPrisoners(listOf(PRISON_NUMBER_THROW_EXCEPTION)) }
    assertThat(exception.message).isEqualTo("Get prisoner request failed")
    with(exception.cause) {
      assertThat(this).isInstanceOf(WebClientResponseException::class.java)
      assertThat(this!!.message).isEqualTo("500 Internal Server Error from POST http://localhost:8112/prisoner-search/prisoner-numbers")
    }
    server.verify(exactly(4), postRequestedFor(urlEqualTo("/prisoner-search/prisoner-numbers")))
  }

  companion object {
    @JvmField
    internal val server = PrisonerSearchServer()

    @BeforeAll
    @JvmStatic
    fun startMocks() {
      server.start()
    }

    @AfterAll
    @JvmStatic
    fun stopMocks() {
      server.stop()
    }
  }
}
