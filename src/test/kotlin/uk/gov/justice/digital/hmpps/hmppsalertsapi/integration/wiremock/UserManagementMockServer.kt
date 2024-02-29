package uk.gov.justice.digital.hmpps.hmppsalertsapi.integration.wiremock

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock.aResponse
import com.github.tomakehurst.wiremock.client.WireMock.get
import com.github.tomakehurst.wiremock.stubbing.StubMapping
import org.junit.jupiter.api.extension.AfterAllCallback
import org.junit.jupiter.api.extension.BeforeAllCallback
import org.junit.jupiter.api.extension.BeforeEachCallback
import org.junit.jupiter.api.extension.ExtensionContext
import uk.gov.justice.digital.hmpps.hmppsalertsapi.client.manageusers.dto.UserDetailsDto
import java.util.UUID

internal const val TEST_USER = "TEST_USER"
internal const val TEST_USER_NAME = "Test User"
internal const val USER_NOT_FOUND = "USER_NOT_FOUND"
internal const val USER_THROW_EXCEPTION = "USER_THROW_EXCEPTION"

class ManageUsersExtension : BeforeAllCallback, AfterAllCallback, BeforeEachCallback {
  companion object {
    @JvmField
    val manageUsersServer = ManageUsersServer()
  }

  override fun beforeAll(context: ExtensionContext) {
    manageUsersServer.start()
    manageUsersServer.stubGetUserDetails()
    manageUsersServer.stubGetUserDetailsException()
  }

  override fun beforeEach(context: ExtensionContext) {
    manageUsersServer.resetRequests()
    manageUsersServer.stubGetUserDetails()
  }

  override fun afterAll(context: ExtensionContext) {
    manageUsersServer.stop()
  }
}

class ManageUsersServer : WireMockServer(8111) {
  private val mapper: ObjectMapper = jacksonObjectMapper().registerModule(JavaTimeModule())

  fun stubGetUserDetails(username: String = TEST_USER, name: String = TEST_USER_NAME): StubMapping =
    stubFor(
      get("/users/$username")
        .willReturn(
          aResponse()
            .withHeader("Content-Type", "application/json")
            .withBody(
              mapper.writeValueAsString(
                UserDetailsDto(
                  username = username,
                  active = true,
                  name = name,
                  authSource = "nomis",
                  userId = "123",
                  uuid = UUID.randomUUID(),
                ),
              ),
            )
            .withStatus(200),
        ),
    )

  fun stubGetUserDetailsException(username: String = USER_THROW_EXCEPTION): StubMapping =
    stubFor(get("/users/$username").willReturn(aResponse().withStatus(500)))
}
