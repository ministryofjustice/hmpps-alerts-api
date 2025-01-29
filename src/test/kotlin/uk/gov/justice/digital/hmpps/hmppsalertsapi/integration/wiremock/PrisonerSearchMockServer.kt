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
import uk.gov.justice.digital.hmpps.hmppsalertsapi.client.prisonersearch.dto.PrisonerDetails
import uk.gov.justice.digital.hmpps.hmppsalertsapi.client.prisonersearch.dto.PrisonerNumbersDto

internal const val PRISON_NUMBER = "A1234AA"
internal const val PRISON_NUMBER_NOT_FOUND = "N1234FN"
internal const val PRISON_NUMBER_NULL_RESPONSE = "P1234NU"
internal const val PRISON_NUMBER_THROW_EXCEPTION = "T1234EX"

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

  fun stubGetPrisoner(prisonNumber: String = PRISON_NUMBER, prisonCode: String = PRISON_CODE_LEEDS): StubMapping = stubFor(
    get("/prisoner/$prisonNumber")
      .willReturn(
        aResponse()
          .withHeader("Content-Type", "application/json")
          .withBody(
            mapper.writeValueAsString(
              PrisonerDetails(
                prisonerNumber = prisonNumber,
                "First",
                "Middle",
                "Last",
                prisonCode,
                status = "ACTIVE IN",
                restrictedPatient = false,
                cellLocation = null,
                supportingPrisonId = null,
              ),
            ),
          )
          .withStatus(200),
      ),
  )

  fun stubGetPrisonerDetails(prisonerDetails: PrisonerDetails): StubMapping = stubFor(
    get("/prisoner/${prisonerDetails.prisonerNumber}")
      .willReturn(
        aResponse()
          .withHeader("Content-Type", "application/json")
          .withBody(
            mapper.writeValueAsString(
              PrisonerDetails(
                prisonerDetails.prisonerNumber,
                prisonerDetails.firstName,
                prisonerDetails.middleNames,
                prisonerDetails.lastName,
                prisonerDetails.prisonId,
                prisonerDetails.status,
                prisonerDetails.restrictedPatient,
                prisonerDetails.cellLocation,
                prisonerDetails.supportingPrisonId,
              ),
            ),
          )
          .withStatus(200),
      ),
  )

  fun stubGetPrisonerException(prisonNumber: String = PRISON_NUMBER_THROW_EXCEPTION): StubMapping = stubFor(get("/prisoner/$prisonNumber").willReturn(aResponse().withStatus(500)))

  fun stubGetPrisoners(prisonNumbers: Collection<String> = listOf(PRISON_NUMBER)): StubMapping = stubFor(
    post("/prisoner-search/prisoner-numbers")
      .withRequestBody(equalToJson(mapper.writeValueAsString(PrisonerNumbersDto(prisonNumbers)), true, true))
      .willReturn(
        aResponse()
          .withHeader("Content-Type", "application/json")
          .withBody(
            mapper.writeValueAsString(
              prisonNumbers.mapIndexed { index, prisonNumber ->
                PrisonerDetails(
                  prisonerNumber = prisonNumber,
                  "First$index",
                  "Middle",
                  "Last$index",
                  PRISON_CODE_LEEDS,
                  status = "ACTIVE IN",
                  restrictedPatient = false,
                  cellLocation = null,
                  supportingPrisonId = null,
                )
              },
            ),
          )
          .withStatus(200),
      ),
  )

  fun stubGetPrisonersNotFound(prisonNumbers: Collection<String> = listOf(PRISON_NUMBER_NOT_FOUND)): StubMapping = stubFor(
    post("/prisoner-search/prisoner-numbers")
      .withRequestBody(equalToJson(mapper.writeValueAsString(PrisonerNumbersDto(prisonNumbers)), true, false))
      .willReturn(
        aResponse()
          .withHeader("Content-Type", "application/json")
          .withBody(mapper.writeValueAsString(emptyList<PrisonerDetails>()))
          .withStatus(200),
      ),
  )

  fun stubGetPrisonersNullResponse(prisonNumbers: Collection<String> = listOf(PRISON_NUMBER_NULL_RESPONSE)): StubMapping = stubFor(
    post("/prisoner-search/prisoner-numbers")
      .withRequestBody(equalToJson(mapper.writeValueAsString(PrisonerNumbersDto(prisonNumbers)), true, false))
      .willReturn(
        aResponse()
          .withHeader("Content-Type", "application/json")
          .withBody("")
          .withStatus(200),
      ),
  )

  fun stubGetPrisonersException(prisonNumbers: Collection<String> = listOf(PRISON_NUMBER_THROW_EXCEPTION)): StubMapping = stubFor(
    post("/prisoner-search/prisoner-numbers")
      .withRequestBody(equalToJson(mapper.writeValueAsString(PrisonerNumbersDto(prisonNumbers)), true, false))
      .willReturn(aResponse().withStatus(500)),
  )
}

class PrisonerSearchExtension :
  BeforeAllCallback,
  AfterAllCallback,
  BeforeEachCallback {
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
