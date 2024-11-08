package uk.gov.justice.digital.hmpps.hmppsalertsapi.service

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.kotlin.whenever
import reactor.core.publisher.Mono.just
import uk.gov.justice.digital.hmpps.hmppsalertsapi.client.manageusers.ManageUsersClient
import uk.gov.justice.digital.hmpps.hmppsalertsapi.integration.wiremock.TEST_USER
import uk.gov.justice.digital.hmpps.hmppsalertsapi.utils.userDetailsDto

class UserServiceTest {
  private val client = mock<ManageUsersClient>()
  private val service = UserService(client)

  @Test
  fun `get user by username`() {
    val userDetails = userDetailsDto()
    whenever(client.getUserDetails(TEST_USER)).thenReturn(just(userDetails))
    val result = service.getUserDetails(TEST_USER).block()
    assertThat(result).isEqualTo(userDetails)
  }
}
