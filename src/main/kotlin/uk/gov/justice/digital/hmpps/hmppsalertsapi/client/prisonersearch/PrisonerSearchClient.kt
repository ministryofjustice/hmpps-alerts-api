package uk.gov.justice.digital.hmpps.hmppsalertsapi.client.prisonersearch

import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.WebClientResponseException
import uk.gov.justice.digital.hmpps.hmppsalertsapi.client.prisonersearch.dto.PrisonerDto
import uk.gov.justice.digital.hmpps.hmppsalertsapi.config.DownstreamServiceException

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
}
