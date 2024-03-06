package uk.gov.justice.digital.hmpps.hmppsalertsapi.utils

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonMapperBuilder

fun testObjectMapper(): ObjectMapper = jacksonMapperBuilder()
  .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
  .addModule(JavaTimeModule())
  .build()
