package uk.gov.justice.digital.hmpps.hmppsalertsapi.client.prisonersearch

import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.core.ParameterizedTypeReference
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.WebClientResponseException
import uk.gov.justice.digital.hmpps.hmppsalertsapi.client.prisonersearch.dto.PrisonerDto
import uk.gov.justice.digital.hmpps.hmppsalertsapi.client.prisonersearch.dto.PrisonerNumbersDto
import uk.gov.justice.digital.hmpps.hmppsalertsapi.config.DownstreamServiceException

inline fun <reified T> typeReference() = object : ParameterizedTypeReference<T>() {}

@Component
class PrisonerSearchClient(@Qualifier("prisonerSearchWebClient") private val webClient: WebClient) {
  fun getPrisoner(prisonerId: String): PrisonerDto? {
    return try {
      webClient
        .get()
        .uri("/prisoner/{prisonerId}", prisonerId)
        .retrieve()
        .bodyToMono(PrisonerDto::class.java)
        .block()
    } catch (e: WebClientResponseException.NotFound) {
      null
    } catch (e: Exception) {
      throw DownstreamServiceException("Get prisoner request failed", e)
    }
  }

  fun getPrisoners(prisonerNumbers: List<String>, batchSize: Int = 1000): Collection<PrisonerDto> {
    require(batchSize in 1..1000) {
      "Batch size must be between 1 and 1000"
    }
    if (prisonerNumbers.isEmpty()) return emptyList()
    return try {
      prisonerNumbers.chunked(batchSize).flatMap {
        webClient
          .post()
          .uri("/prisoner-search/prisoner-numbers")
          .bodyValue(PrisonerNumbersDto(it))
          .retrieve()
          .bodyToMono(typeReference<List<PrisonerDto>>())
          .block() ?: emptyList()
      }
    } catch (e: Exception) {
      throw DownstreamServiceException("Get prisoner request failed", e)
    }
  }
}
