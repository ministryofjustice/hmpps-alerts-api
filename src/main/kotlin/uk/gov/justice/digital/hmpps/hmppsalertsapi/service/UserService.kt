package uk.gov.justice.digital.hmpps.hmppsalertsapi.service

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppsalertsapi.client.usermanagement.UserManagementClient

@Service
class UserService(private val userManagementClient: UserManagementClient) {
  fun getUserDetails(username: String) = userManagementClient.getUserDetails(username)
}
