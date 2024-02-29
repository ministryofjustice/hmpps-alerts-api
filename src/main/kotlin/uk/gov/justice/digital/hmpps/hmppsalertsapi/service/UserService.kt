package uk.gov.justice.digital.hmpps.hmppsalertsapi.service

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppsalertsapi.client.manageusers.ManageUsersClient

@Service
class UserService(private val manageUsersClient: ManageUsersClient) {
  fun getUserDetails(username: String) = manageUsersClient.getUserDetails(username)
}
