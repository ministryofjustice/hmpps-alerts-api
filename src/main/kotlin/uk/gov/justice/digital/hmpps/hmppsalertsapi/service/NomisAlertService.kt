package uk.gov.justice.digital.hmpps.hmppsalertsapi.service

import com.fasterxml.jackson.databind.ObjectMapper
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.hmppsalertsapi.config.SyncContext
import uk.gov.justice.digital.hmpps.hmppsalertsapi.domain.toEntity
import uk.gov.justice.digital.hmpps.hmppsalertsapi.domain.toMappingModel
import uk.gov.justice.digital.hmpps.hmppsalertsapi.model.NomisAlert
import uk.gov.justice.digital.hmpps.hmppsalertsapi.model.NomisAlertMapping
import uk.gov.justice.digital.hmpps.hmppsalertsapi.repository.NomisAlertRepository
import java.util.UUID

@Service
@Transactional
class NomisAlertService(
  private val nomisAlertRepository: NomisAlertRepository,
  private val objectMapper: ObjectMapper,
) {
  fun upsertNomisAlert(
    nomisAlert: NomisAlert,
    syncContext: SyncContext,
  ): NomisAlertMapping {
    val alertUuid = UUID.randomUUID()

    val entity = nomisAlert.toEntity(objectMapper, alertUuid)

    return entity.toMappingModel()
  }

  private companion object {
    private val log: Logger = LoggerFactory.getLogger(this::class.java)
  }
}
