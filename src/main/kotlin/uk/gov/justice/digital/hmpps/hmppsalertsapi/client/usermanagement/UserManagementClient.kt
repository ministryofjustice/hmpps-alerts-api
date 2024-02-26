package uk.gov.justice.digital.hmpps.hmppsalertsapi.client.usermanagement

import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.WebClientResponseException
import uk.gov.justice.digital.hmpps.hmppsalertsapi.client.usermanagement.dto.UserDetailsDto
import uk.gov.justice.digital.hmpps.hmppsalertsapi.config.DownstreamServiceException

@Component
class UserManagementClient(@Qualifier("userManagementWebClient") private val webClient: WebClient) {
  fun getUserDetails(username: String): UserDetailsDto? {
    return try {
      webClient
        .get()
        .uri("/users/{username}", username)
        .retrieve()
        .bodyToMono(UserDetailsDto::class.java)
        .block()
    } catch (e: WebClientResponseException.NotFound) {
      null
    } catch (e: Exception) {
      throw DownstreamServiceException("Get user details request failed", e)
    }
  }
}
