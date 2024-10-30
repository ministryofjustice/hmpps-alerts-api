package uk.gov.justice.digital.hmpps.hmppsalertsapi.integration.wiremock

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock.aResponse
import com.github.tomakehurst.wiremock.client.WireMock.equalToJson
import com.github.tomakehurst.wiremock.client.WireMock.get
import com.github.tomakehurst.wiremock.client.WireMock.post
import com.github.tomakehurst.wiremock.stubbing.StubMapping
import org.junit.jupiter.api.extension.AfterAllCallback
import org.junit.jupiter.api.extension.BeforeAllCallback
import org.junit.jupiter.api.extension.BeforeEachCallback
import org.junit.jupiter.api.extension.ExtensionContext
import uk.gov.justice.digital.hmpps.hmppsalertsapi.client.prisonersearch.dto.PrisonerDto
import uk.gov.justice.digital.hmpps.hmppsalertsapi.client.prisonersearch.dto.PrisonerNumbersDto
import java.time.LocalDate

internal const val PRISON_NUMBER = "A1234AA"
internal const val PRISON_NUMBER_NOT_FOUND = "NOT_FOUND"
internal const val PRISON_NUMBER_NULL_RESPONSE = "NULL"
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

  fun stubGetPrisoner(prisonNumber: String = PRISON_NUMBER, prisonCode: String = PRISON_CODE_LEEDS): StubMapping =
    stubFor(
      get("/prisoner/$prisonNumber").authorised()
        .willReturn(
          aResponse()
            .withHeader("Content-Type", "application/json")
            .withBody(
              mapper.writeValueAsString(
                PrisonerDto(
                  prisonerNumber = prisonNumber,
                  bookingId = 1234,
                  "First",
                  "Middle",
                  "Last",
                  LocalDate.of(1988, 4, 3),
                  prisonCode,
                ),
              ),
            )
            .withStatus(200),
        ),
    )

  fun stubGetPrisonerException(prisonNumber: String = PRISON_NUMBER_THROW_EXCEPTION): StubMapping =
    stubFor(get("/prisoner/$prisonNumber").authorised().willReturn(aResponse().withStatus(500)))

  fun stubGetPrisoners(prisonNumbers: Collection<String> = listOf(PRISON_NUMBER)): StubMapping =
    stubFor(
      post("/prisoner-search/prisoner-numbers").authorised()
        .withRequestBody(equalToJson(mapper.writeValueAsString(PrisonerNumbersDto(prisonNumbers)), true, false))
        .willReturn(
          aResponse()
            .withHeader("Content-Type", "application/json")
            .withBody(
              mapper.writeValueAsString(
                prisonNumbers.mapIndexed { index, prisonNumber ->
                  PrisonerDto(
                    prisonerNumber = prisonNumber,
                    bookingId = index + 1L,
                    "First$index",
                    "Middle",
                    "Last$index",
                    LocalDate.of(1988, 4, 3).plusDays(index.toLong()),
                    PRISON_CODE_LEEDS,
                  )
                },
              ),
            )
            .withStatus(200),
        ),
    )

  fun stubGetPrisonersNotFound(prisonNumbers: Collection<String> = listOf(PRISON_NUMBER_NOT_FOUND)): StubMapping =
    stubFor(
      post("/prisoner-search/prisoner-numbers").authorised()
        .withRequestBody(equalToJson(mapper.writeValueAsString(PrisonerNumbersDto(prisonNumbers)), true, false))
        .willReturn(
          aResponse()
            .withHeader("Content-Type", "application/json")
            .withBody(mapper.writeValueAsString(emptyList<PrisonerDto>()))
            .withStatus(200),
        ),
    )

  fun stubGetPrisonersNullResponse(prisonNumbers: Collection<String> = listOf(PRISON_NUMBER_NULL_RESPONSE)): StubMapping =
    stubFor(
      post("/prisoner-search/prisoner-numbers").authorised()
        .withRequestBody(equalToJson(mapper.writeValueAsString(PrisonerNumbersDto(prisonNumbers)), true, false))
        .willReturn(
          aResponse()
            .withHeader("Content-Type", "application/json")
            .withBody("")
            .withStatus(200),
        ),
    )

  fun stubGetPrisonersException(prisonNumbers: Collection<String> = listOf(PRISON_NUMBER_THROW_EXCEPTION)): StubMapping =
    stubFor(
      post("/prisoner-search/prisoner-numbers").authorised()
        .withRequestBody(equalToJson(mapper.writeValueAsString(PrisonerNumbersDto(prisonNumbers)), true, false))
        .willReturn(aResponse().withStatus(500)),
    )
}

class PrisonerSearchExtension : BeforeAllCallback, AfterAllCallback, BeforeEachCallback {
  companion object {
    @JvmField
    val prisonerSearch = PrisonerSearchServer()
  }

  override fun beforeAll(context: ExtensionContext) {
    prisonerSearch.start()
  }

  override fun beforeEach(context: ExtensionContext) {
    prisonerSearch.resetRequests()
    prisonerSearch.stubGetPrisoner()
    prisonerSearch.stubGetPrisonerException()
    prisonerSearch.stubGetPrisoners()
    prisonerSearch.stubGetPrisonersNotFound()
    prisonerSearch.stubGetPrisonersException()
  }

  override fun afterAll(context: ExtensionContext) {
    prisonerSearch.stop()
  }
}
