package uk.gov.justice.digital.hmpps.hmppsalertsapi.client.prisonersearch

import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.bodyToFlux
import org.springframework.web.reactive.function.client.bodyToMono
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import uk.gov.justice.digital.hmpps.hmppsalertsapi.client.prisonersearch.dto.PrisonerDetails
import uk.gov.justice.digital.hmpps.hmppsalertsapi.client.prisonersearch.dto.PrisonerNumbersDto
import uk.gov.justice.digital.hmpps.hmppsalertsapi.client.retryNetworkExceptions

@Component
class PrisonerSearchClient(@Qualifier("prisonerSearchWebClient") private val webClient: WebClient) {
  fun getPrisoner(prisonerId: String): Mono<PrisonerDetails> {
    return webClient.get()
      .uri("/prisoner/{prisonerId}", prisonerId)
      .exchangeToMono { res ->
        when (res.statusCode()) {
          HttpStatus.NOT_FOUND -> Mono.empty()
          HttpStatus.OK -> res.bodyToMono<PrisonerDetails>()
          else -> res.createError()
        }
      }
      .retryNetworkExceptions("Get prisoner request failed")
  }

  fun getPrisoners(prisonNumbers: Collection<String>, batchSize: Int = 1000): List<PrisonerDetails> {
    require(batchSize in 1..1000) {
      "Batch size must be between 1 and 1000"
    }
    if (prisonNumbers.isEmpty()) return emptyList()
    return Flux.fromIterable(prisonNumbers).buffer(batchSize).flatMap(
        {
            webClient
                .post()
                .uri("/prisoner-search/prisoner-numbers")
                .bodyValue(PrisonerNumbersDto(it))
                .retrieve()
                .bodyToFlux<PrisonerDetails>()
                .retryNetworkExceptions("Get prisoner request failed")
        },
        5,
    ).collectList()
      .block()!!
  }
}
