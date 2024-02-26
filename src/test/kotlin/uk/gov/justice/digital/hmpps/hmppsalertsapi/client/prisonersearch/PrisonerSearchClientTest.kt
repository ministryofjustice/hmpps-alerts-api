package uk.gov.justice.digital.hmpps.hmppsalertsapi.client.prisonersearch

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.WebClientResponseException
import uk.gov.justice.digital.hmpps.hmppsalertsapi.client.prisonersearch.dto.PrisonerDto
import uk.gov.justice.digital.hmpps.hmppsalertsapi.config.DownstreamServiceException
import uk.gov.justice.digital.hmpps.hmppsalertsapi.integration.wiremock.PRISON_NUMBER
import uk.gov.justice.digital.hmpps.hmppsalertsapi.integration.wiremock.PRISON_NUMBER_NOT_FOUND
import uk.gov.justice.digital.hmpps.hmppsalertsapi.integration.wiremock.PRISON_NUMBER_THROW_EXCEPTION
import uk.gov.justice.digital.hmpps.hmppsalertsapi.integration.wiremock.PrisonerSearchServer
import java.time.LocalDate

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
    server.stubGetPrisonerDetails()

    val result = client.getPrisoner(PRISON_NUMBER)

    assertThat(result!!).isEqualTo(
      PrisonerDto(
        prisonerNumber = PRISON_NUMBER,
        bookingId = 1234,
        "Prisoner",
        "Middle",
        "lastName",
        LocalDate.of(1988, 4, 3),
      ),
    )
  }

  @Test
  fun `getPrisoner - prisoner not found`() {
    val result = client.getPrisoner(PRISON_NUMBER_NOT_FOUND)

    assertThat(result).isNull()
  }

  @Test
  fun `getPrisoner - downstream service exception`() {
    server.stubGetPrisonerDetailsException()

    val exception = assertThrows<DownstreamServiceException> { client.getPrisoner(PRISON_NUMBER_THROW_EXCEPTION) }
    assertThat(exception.message).isEqualTo("Get prisoner request failed")
    with(exception.cause) {
      assertThat(this).isInstanceOf(WebClientResponseException::class.java)
      assertThat(this!!.message).isEqualTo("500 Internal Server Error from GET http://localhost:8112/prisoner/${PRISON_NUMBER_THROW_EXCEPTION}")
    }
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
