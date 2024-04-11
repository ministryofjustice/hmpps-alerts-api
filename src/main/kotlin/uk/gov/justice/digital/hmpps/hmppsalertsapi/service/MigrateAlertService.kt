package uk.gov.justice.digital.hmpps.hmppsalertsapi.service

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.hmppsalertsapi.config.ExistingActiveAlertWithCodeException
import uk.gov.justice.digital.hmpps.hmppsalertsapi.domain.toAlertEntity
import uk.gov.justice.digital.hmpps.hmppsalertsapi.domain.toAlertModel
import uk.gov.justice.digital.hmpps.hmppsalertsapi.entity.AlertCode
import uk.gov.justice.digital.hmpps.hmppsalertsapi.model.Alert
import uk.gov.justice.digital.hmpps.hmppsalertsapi.model.MigratedAlert
import uk.gov.justice.digital.hmpps.hmppsalertsapi.model.request.MigrateAlert
import uk.gov.justice.digital.hmpps.hmppsalertsapi.model.request.MigrateAlertRequest
import uk.gov.justice.digital.hmpps.hmppsalertsapi.repository.AlertCodeRepository
import uk.gov.justice.digital.hmpps.hmppsalertsapi.repository.AlertRepository
import java.time.LocalDateTime
import uk.gov.justice.digital.hmpps.hmppsalertsapi.entity.Alert as AlertEntity

@Service
@Transactional
class MigrateAlertService(
  private val alertCodeRepository: AlertCodeRepository,
  private val alertRepository: AlertRepository,
) {
  fun migrateAlert(migrateAlertRequest: MigrateAlertRequest): Alert {
    val alertCode = alertCodeRepository.findByCode(migrateAlertRequest.alertCode) ?: throw IllegalArgumentException("Alert code '${migrateAlertRequest.alertCode}' not found")
    val alert = migrateAlertRequest.toAlertEntity(alertCode)
    if (alert.isActive() || alert.willBecomeActive()) {
      alert.checkForExistingActiveAlert()
    }
    return alertRepository.saveAndFlush(alert).toAlertModel()
  }

  private fun AlertEntity.checkForExistingActiveAlert() =
    alertRepository.findByPrisonNumberAndAlertCodeCode(prisonNumber, alertCode.code)
      .any { it.isActive() || it.willBecomeActive() } && throw ExistingActiveAlertWithCodeException(prisonNumber, alertCode.code)

  fun migratePrisonerAlerts(prisonNumber: String, request: List<MigrateAlert>): List<MigratedAlert> {
    val migratedAt = LocalDateTime.now()
    val alertCodes = request.alertCodes()
    request.checkForNotFoundAlertCodes(alertCodes)

    return request.map {
      alertRepository.save(it.toAlertEntity(prisonNumber, alertCodes[it.alertCode]!!, migratedAt))
    }.also {
      alertRepository.flush()
    }.map {
      MigratedAlert(
        offenderBookId = it.migratedAlert!!.offenderBookId,
        bookingSeq = it.migratedAlert!!.bookingSeq,
        alertSeq = it.migratedAlert!!.alertSeq,
        alertUuid = it.alertUuid,
      )
    }
  }

  private fun List<MigrateAlert>.alertCodes() =
    map { it.alertCode }.distinct().let { requestAlertCodes ->
      alertCodeRepository.findByCodeIn(requestAlertCodes).associateBy { it.code }
    }

  private fun List<MigrateAlert>.checkForNotFoundAlertCodes(alertCodes: Map<String, AlertCode>) =
    map { it.alertCode }.distinct().filterNot { alertCodes.containsKey(it) }.sorted().run {
      if (this.isNotEmpty()) {
        throw IllegalArgumentException("Alert code(s) '${this.joinToString("', '") }' not found")
      }
    }
}
