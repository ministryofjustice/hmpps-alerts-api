package uk.gov.justice.digital.hmpps.hmppsalertsapi.service

import com.fasterxml.jackson.databind.ObjectMapper
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.hmppsalertsapi.config.SyncContext
import uk.gov.justice.digital.hmpps.hmppsalertsapi.domain.toEntity
import uk.gov.justice.digital.hmpps.hmppsalertsapi.domain.toMappingModel
import uk.gov.justice.digital.hmpps.hmppsalertsapi.entity.NomisAlert
import uk.gov.justice.digital.hmpps.hmppsalertsapi.enumeration.UpsertStatus
import uk.gov.justice.digital.hmpps.hmppsalertsapi.repository.NomisAlertRepository
import java.time.LocalDateTime
import uk.gov.justice.digital.hmpps.hmppsalertsapi.model.NomisAlert as NomisAlertModel

@Service
@Transactional
class NomisAlertService(
  private val nomisAlertRepository: NomisAlertRepository,
  private val objectMapper: ObjectMapper,
) {
  fun upsertNomisAlert(
    nomisAlertModel: NomisAlertModel,
    syncContext: SyncContext,
  ) =
    nomisAlertRepository.findByOffenderBookIdAndAlertSeq(nomisAlertModel.offenderBookId, nomisAlertModel.alertSeq).let {
      when (it) {
        null -> createNomisAlert(nomisAlertModel)
        else -> updateNomisAlert(it, nomisAlertModel)
      }
    }

  private fun createNomisAlert(nomisAlertModel: NomisAlertModel) =
    nomisAlertModel.toEntity(objectMapper).let {
      nomisAlertRepository.saveAndFlush(it)
      it.toMappingModel(UpsertStatus.CREATED)
    }

  private fun updateNomisAlert(existingNomisAlert: NomisAlert, nomisAlertModel: NomisAlertModel) =
    existingNomisAlert.apply {
      nomisAlertData = objectMapper.valueToTree(nomisAlertModel)
      upsertedAt = LocalDateTime.now()
    }.let {
      nomisAlertRepository.saveAndFlush(it)
      it.toMappingModel(UpsertStatus.UPDATED)
    }

  private companion object {
    private val log: Logger = LoggerFactory.getLogger(this::class.java)
  }
}
