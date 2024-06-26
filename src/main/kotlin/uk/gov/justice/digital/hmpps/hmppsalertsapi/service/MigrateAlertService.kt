package uk.gov.justice.digital.hmpps.hmppsalertsapi.service

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.hmppsalertsapi.domain.toAlertEntity
import uk.gov.justice.digital.hmpps.hmppsalertsapi.entity.Alert
import uk.gov.justice.digital.hmpps.hmppsalertsapi.entity.AlertCode
import uk.gov.justice.digital.hmpps.hmppsalertsapi.model.MigratedAlert
import uk.gov.justice.digital.hmpps.hmppsalertsapi.model.request.MigrateAlert
import uk.gov.justice.digital.hmpps.hmppsalertsapi.repository.AlertCodeRepository
import uk.gov.justice.digital.hmpps.hmppsalertsapi.repository.AlertRepository
import java.time.LocalDateTime

@Service
@Transactional
class MigrateAlertService(
  private val alertCodeRepository: AlertCodeRepository,
  private val alertRepository: AlertRepository,
) {
  fun migratePrisonerAlerts(prisonNumber: String, request: List<MigrateAlert>): List<MigratedAlert> {
    val migratedAt = LocalDateTime.now()
    val alertCodes = request.alertCodes()
    request.checkForNotFoundAlertCodes(alertCodes)
    request.logActiveToBeforeActiveFrom(prisonNumber)

    alertRepository.deleteAll(alertRepository.findByPrisonNumber(prisonNumber))
    alertRepository.flush()

    return request.map {
      alertRepository.save(it.toAlertEntity(prisonNumber, alertCodes[it.alertCode]!!, migratedAt))
    }.also {
      it.logDuplicateActiveAlerts(prisonNumber)
      it.logHistoricAlerts(prisonNumber)
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

  private fun List<MigrateAlert>.logActiveToBeforeActiveFrom(prisonNumber: String) {
    this.filter { it.activeTo?.isBefore(it.activeFrom) == true }.forEach {
      log.warn("Alert with sequence '${it.alertSeq}' for person with prison number '$prisonNumber' from booking with id '${it.offenderBookId}' and sequence '${it.bookingSeq}' has an active to date '${it.activeTo}' that is before the active from date '${it.activeFrom}'")
    }
  }

  private fun List<Alert>.logDuplicateActiveAlerts(prisonNumber: String) {
    this.filter { it.isActive() }.groupBy { it.alertCode.code }.filter { it.value.size > 1 }.run {
      if (any()) {
        log.warn("Person with prison number '$prisonNumber' has ${this.size} duplicate active alert(s) for code(s) ${this.map { "'${it.key}' (${it.value.size} active)" }.joinToString(", ")}")
      }
    }
  }

  private fun List<Alert>.logHistoricAlerts(prisonNumber: String) {
    this.filter { (it.migratedAlert?.bookingSeq ?: 1) > 1 }.run {
      if (any()) {
        log.warn(
          "Person with prison number '$prisonNumber' has ${this.size} historic alert(s) for code(s) ${
            this.joinToString(
              ", ",
            ) { "'${it.alertCode.code}' (${if (it.isActive()) "active" else "inactive"})" }
          }",
        )
      }
    }
  }

  private companion object {
    private val log: Logger = LoggerFactory.getLogger(this::class.java)
  }
}
