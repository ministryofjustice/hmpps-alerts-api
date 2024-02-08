package uk.gov.justice.digital.hmpps.hmppsalertsapi.config

import com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonMapperBuilder
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class JacksonConfiguration {
  @Bean
  fun objectMapper(): ObjectMapper = jacksonMapperBuilder()
    .configure(FAIL_ON_UNKNOWN_PROPERTIES, false)
    .addModule(JavaTimeModule())
    .build()
}
