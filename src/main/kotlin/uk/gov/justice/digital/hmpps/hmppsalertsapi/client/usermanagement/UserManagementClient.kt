package uk.gov.justice.digital.hmpps.hmppsalertsapi.client.usermanagement

import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import uk.gov.justice.digital.hmpps.hmppsalertsapi.client.usermanagement.dto.UserDetailsDto

@Component
class UserManagementClient(@Qualifier("userManagementWebClient") private val webClient: WebClient) {
  fun getUserDetails(username: String): UserDetailsDto? {
    return webClient
      .get()
      .uri("/users/{username}", username)
      .retrieve()
      .bodyToMono(UserDetailsDto::class.java)
      .block()
  }
}
