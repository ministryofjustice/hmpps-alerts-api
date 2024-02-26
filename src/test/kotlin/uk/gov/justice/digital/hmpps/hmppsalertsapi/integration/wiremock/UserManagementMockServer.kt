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
import uk.gov.justice.digital.hmpps.hmppsalertsapi.client.usermanagement.dto.UserDetailsDto
import java.util.UUID

internal const val TEST_USER = "TEST_USER"
internal const val TEST_USER_NAME = "Test User"
internal const val USER_NOT_FOUND = "USER_NOT_FOUND"
internal const val USER_THROW_EXCEPTION = "USER_THROW_EXCEPTION"

class UserManagementExtension : BeforeAllCallback, AfterAllCallback, BeforeEachCallback {
  companion object {
    @JvmField
    val userManagement = UserManagementServer()
  }

  override fun beforeAll(context: ExtensionContext) {
    userManagement.start()
    userManagement.stubGetUserDetails()
    userManagement.stubGetUserDetailsException()
  }

  override fun beforeEach(context: ExtensionContext) {
    userManagement.resetRequests()
    userManagement.stubGetUserDetails()
  }

  override fun afterAll(context: ExtensionContext) {
    userManagement.stop()
  }
}

class UserManagementServer : WireMockServer(8111) {
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
