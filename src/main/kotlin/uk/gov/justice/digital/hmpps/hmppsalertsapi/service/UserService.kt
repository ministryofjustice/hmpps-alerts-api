package uk.gov.justice.digital.hmpps.hmppsalertsapi.service

import org.springframework.stereotype.Service
import reactor.core.publisher.Mono
import uk.gov.justice.digital.hmpps.hmppsalertsapi.client.manageusers.ManageUsersClient
import uk.gov.justice.digital.hmpps.hmppsalertsapi.client.manageusers.dto.UserDetailsDto

@Service
class UserService(private val manageUsersClient: ManageUsersClient) {
  fun getUserDetails(username: String): Mono<UserDetailsDto> = manageUsersClient.getUserDetails(username)
}
