package uk.gov.justice.digital.hmpps.hmppsalertsapi.client.prisonersearch

import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.bodyToFlux
import org.springframework.web.reactive.function.client.bodyToMono
import reactor.core.publisher.Mono
import uk.gov.justice.digital.hmpps.hmppsalertsapi.client.prisonersearch.dto.PrisonerDto
import uk.gov.justice.digital.hmpps.hmppsalertsapi.client.prisonersearch.dto.PrisonerNumbersDto
import uk.gov.justice.digital.hmpps.hmppsalertsapi.client.retryNetworkExceptions

@Component
class PrisonerSearchClient(@Qualifier("prisonerSearchWebClient") private val webClient: WebClient) {
  fun getPrisoner(prisonerId: String): Mono<PrisonerDto> {
    return webClient.get()
      .uri("/prisoner/{prisonerId}", prisonerId)
      .exchangeToMono { res ->
        when (res.statusCode()) {
          HttpStatus.NOT_FOUND -> Mono.empty()
          HttpStatus.OK -> res.bodyToMono<PrisonerDto>()
          else -> res.createError()
        }
      }
      .retryNetworkExceptions("Get prisoner request failed")
  }

  fun getPrisoners(prisonNumbers: Collection<String>, batchSize: Int = 1000): Collection<PrisonerDto> {
    require(batchSize in 1..1000) {
      "Batch size must be between 1 and 1000"
    }
    if (prisonNumbers.isEmpty()) return emptyList()
    return prisonNumbers.chunked(batchSize).flatMap {
      webClient
        .post()
        .uri("/prisoner-search/prisoner-numbers")
        .bodyValue(PrisonerNumbersDto(it))
        .retrieve()
        .bodyToFlux<PrisonerDto>()
        .collectList()
        .retryNetworkExceptions("Get prisoner request failed")
        .block() ?: emptyList()
    }
  }
}
