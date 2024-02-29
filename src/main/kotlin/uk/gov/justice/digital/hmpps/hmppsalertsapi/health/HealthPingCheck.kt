package uk.gov.justice.digital.hmpps.hmppsalertsapi.health

import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import uk.gov.justice.hmpps.kotlin.health.HealthPingCheck

@Component("hmppsAuth")
class HmppsAuthHealthPingCheck(@Qualifier("hmppsAuthHealthWebClient") webClient: WebClient) : HealthPingCheck(webClient)

@Component("manageUsers")
class ManageUsersHealthPingCheck(@Qualifier("manageUsersHealthWebClient") webClient: WebClient) : HealthPingCheck(webClient)

@Component("prisonerSearch")
class PrisonerSearchHealthPingCheck(@Qualifier("prisonerSearchHealthWebClient") webClient: WebClient) : HealthPingCheck(webClient)
