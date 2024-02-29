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
import uk.gov.justice.digital.hmpps.hmppsalertsapi.client.prisonersearch.dto.PrisonerDto
import java.time.LocalDate

internal const val PRISON_NUMBER = "A1234AA"
internal const val PRISON_NUMBER_NOT_FOUND = "NOT_FOUND"
internal const val PRISON_NUMBER_THROW_EXCEPTION = "THROW"

class PrisonerSearchServer : WireMockServer(8112) {
  private val mapper: ObjectMapper = jacksonObjectMapper().registerModule(JavaTimeModule())

  fun stubHealthPing(status: Int) {
    stubFor(
      get("/health/ping").willReturn(
        aResponse()
          .withHeader("Content-Type", "application/json")
          .withBody(if (status == 200) """{"status":"UP"}""" else """{"status":"DOWN"}""")
          .withStatus(status),
      ),
    )
  }

  fun stubGetPrisonerDetails(prisonNumber: String = PRISON_NUMBER): StubMapping =
    stubFor(
      get("/prisoner/$prisonNumber")
        .willReturn(
          aResponse()
            .withHeader("Content-Type", "application/json")
            .withBody(
              mapper.writeValueAsString(
                PrisonerDto(
                  prisonerNumber = prisonNumber,
                  bookingId = 1234,
                  "Prisoner",
                  "Middle",
                  "lastName",
                  LocalDate.of(1988, 4, 3),
                ),
              ),
            )
            .withStatus(200),
        ),
    )

  fun stubGetPrisonerDetailsException(prisonNumber: String = PRISON_NUMBER_THROW_EXCEPTION): StubMapping =
    stubFor(get("/prisoner/$prisonNumber").willReturn(aResponse().withStatus(500)))
}

class PrisonerSearchExtension : BeforeAllCallback, AfterAllCallback, BeforeEachCallback {
  companion object {
    @JvmField
    val prisonerSearch = PrisonerSearchServer()
  }

  override fun beforeAll(context: ExtensionContext) {
    prisonerSearch.start()
    prisonerSearch.stubGetPrisonerDetails()
    prisonerSearch.stubGetPrisonerDetailsException()
  }

  override fun beforeEach(context: ExtensionContext) {
    prisonerSearch.resetRequests()
    prisonerSearch.stubGetPrisonerDetails()
  }

  override fun afterAll(context: ExtensionContext) {
    prisonerSearch.stop()
  }
}
