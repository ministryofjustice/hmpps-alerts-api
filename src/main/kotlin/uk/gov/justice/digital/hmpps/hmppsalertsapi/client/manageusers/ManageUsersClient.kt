package uk.gov.justice.digital.hmpps.hmppsalertsapi.client.manageusers

import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.bodyToMono
import reactor.core.publisher.Mono
import uk.gov.justice.digital.hmpps.hmppsalertsapi.client.manageusers.dto.UserDetailsDto
import uk.gov.justice.digital.hmpps.hmppsalertsapi.client.retryNetworkExceptions

@Component
class ManageUsersClient(@Qualifier("manageUsersWebClient") private val webClient: WebClient) {
  fun getUserDetails(username: String): Mono<UserDetailsDto> = webClient.get()
    .uri("/users/{username}", username)
    .exchangeToMono { res ->
      when (res.statusCode()) {
        HttpStatus.NOT_FOUND -> Mono.empty()
        HttpStatus.OK -> res.bodyToMono<UserDetailsDto>()
        else -> res.createError()
      }
    }
    .retryNetworkExceptions("Get user details request failed")
}
