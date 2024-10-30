package uk.gov.justice.digital.hmpps.hmppsalertsapi.integration.wiremock

import com.github.tomakehurst.wiremock.client.MappingBuilder
import com.github.tomakehurst.wiremock.client.WireMock.matching

fun MappingBuilder.authorised(): MappingBuilder = withHeader("Authorization", matching("Bearer .*"))
