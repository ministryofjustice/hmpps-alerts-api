package uk.gov.justice.digital.hmpps.hmppsalertsapi.client.manageusers

import com.github.tomakehurst.wiremock.client.WireMock.exactly
import com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor
import com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.WebClientResponseException
import uk.gov.justice.digital.hmpps.hmppsalertsapi.client.manageusers.dto.UserDetailsDto
import uk.gov.justice.digital.hmpps.hmppsalertsapi.config.DownstreamServiceException
import uk.gov.justice.digital.hmpps.hmppsalertsapi.integration.wiremock.ManageUsersServer
import uk.gov.justice.digital.hmpps.hmppsalertsapi.integration.wiremock.PRISON_CODE_LEEDS
import uk.gov.justice.digital.hmpps.hmppsalertsapi.integration.wiremock.TEST_USER
import uk.gov.justice.digital.hmpps.hmppsalertsapi.integration.wiremock.TEST_USER_NAME
import uk.gov.justice.digital.hmpps.hmppsalertsapi.integration.wiremock.USER_NOT_FOUND
import uk.gov.justice.digital.hmpps.hmppsalertsapi.integration.wiremock.USER_THROW_EXCEPTION

class ManageUsersClientTest {
  private lateinit var client: ManageUsersClient

  @BeforeEach
  fun resetMocks() {
    server.resetRequests()
    val webClient = WebClient.create("http://localhost:${server.port()}")
    client = ManageUsersClient(webClient)
  }

  @Test
  fun `getUserDetails - success`() {
    server.stubGetUserDetails()

    val result = client.getUserDetails(TEST_USER).block()

    assertThat(result!!).isEqualTo(
      UserDetailsDto(
        TEST_USER,
        true,
        TEST_USER_NAME,
        "nomis",
        PRISON_CODE_LEEDS,
        "123",
        result.uuid,
      ),
    )
    server.verify(exactly(1), getRequestedFor(urlEqualTo("/users/$TEST_USER")))
  }

  @Test
  fun `getUserDetails - user not found`() {
    val result = client.getUserDetails(USER_NOT_FOUND).block()

    assertThat(result).isNull()
    server.verify(exactly(1), getRequestedFor(urlEqualTo("/users/$USER_NOT_FOUND")))
  }

  @Test
  fun `getUserDetails - downstream service exception`() {
    server.stubGetUserDetailsException()

    val exception = assertThrows<DownstreamServiceException> { client.getUserDetails(USER_THROW_EXCEPTION).block() }
    assertThat(exception.message).isEqualTo("Get user details request failed")
    with(exception.cause) {
      assertThat(this).isInstanceOf(WebClientResponseException::class.java)
      assertThat(this!!.message).isEqualTo("500 Internal Server Error from GET http://localhost:8111/users/${USER_THROW_EXCEPTION}")
    }
    server.verify(exactly(4), getRequestedFor(urlEqualTo("/users/$USER_THROW_EXCEPTION")))
  }

  companion object {
    @JvmField
    internal val server = ManageUsersServer()

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
