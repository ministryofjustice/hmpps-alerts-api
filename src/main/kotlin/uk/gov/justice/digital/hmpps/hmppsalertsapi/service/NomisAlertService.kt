package uk.gov.justice.digital.hmpps.hmppsalertsapi.service

import com.fasterxml.jackson.databind.ObjectMapper
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.hmppsalertsapi.config.SyncContext
import uk.gov.justice.digital.hmpps.hmppsalertsapi.model.NomisAlert
import uk.gov.justice.digital.hmpps.hmppsalertsapi.model.NomisAlertMapping
import uk.gov.justice.digital.hmpps.hmppsalertsapi.repository.NomisAlertRepository

@Service
@Transactional
class NomisAlertService(
  private val nomisAlertRepository: NomisAlertRepository,
  private val objectMapper: ObjectMapper,
) {
  private companion object {
    val log: Logger = LoggerFactory.getLogger(this::class.java)
  }

  fun upsertAlert(
    nomisAlert: NomisAlert,
    syncContext: SyncContext,
  ): NomisAlertMapping {
    TODO("Not yet implemented")
  }
}
