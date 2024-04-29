package uk.gov.justice.digital.hmpps.hmppsalertsapi.service

import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.hmppsalertsapi.client.prisonersearch.PrisonerSearchClient
import uk.gov.justice.digital.hmpps.hmppsalertsapi.config.AlertRequestContext
import uk.gov.justice.digital.hmpps.hmppsalertsapi.domain.toAlertEntity
import uk.gov.justice.digital.hmpps.hmppsalertsapi.domain.toBulkAlertAlertModel
import uk.gov.justice.digital.hmpps.hmppsalertsapi.domain.toEntity
import uk.gov.justice.digital.hmpps.hmppsalertsapi.domain.toModel
import uk.gov.justice.digital.hmpps.hmppsalertsapi.entity.Alert
import uk.gov.justice.digital.hmpps.hmppsalertsapi.entity.AlertCode
import uk.gov.justice.digital.hmpps.hmppsalertsapi.model.request.BulkCreateAlerts
import uk.gov.justice.digital.hmpps.hmppsalertsapi.repository.AlertCodeRepository
import uk.gov.justice.digital.hmpps.hmppsalertsapi.repository.AlertRepository
import uk.gov.justice.digital.hmpps.hmppsalertsapi.repository.BulkAlertRepository

@Service
@Transactional
class BulkAlertService(
  private val alertRepository: AlertRepository,
  private val alertCodeRepository: AlertCodeRepository,
  private val bulkAlertRepository: BulkAlertRepository,
  private val prisonerSearchClient: PrisonerSearchClient,
  private val objectMapper: ObjectMapper,
) {
  fun bulkCreateAlerts(request: BulkCreateAlerts, context: AlertRequestContext, batchSize: Int = 1000) =
    request.let {
      require(batchSize in 1..1000) {
        "Batch size must be between 1 and 1000"
      }

      // Perform database checks first prior to checks that require API calls
      val alertCode = it.getAlertCode()

      // Uses API call
      it.validatePrisonNumbers(batchSize)

      val existingUnexpiredAlerts = it.getExistingUnexpiredAlerts(batchSize)
      val existingActiveAlerts = existingUnexpiredAlerts.filter { alert -> alert.isActive() }
      //val existingWillBecomeActiveAlerts = existingUnexpiredAlerts.filter { alert -> alert.willBecomeActive() }

      val alertsCreated = it.createAlerts(context, alertCode, existingActiveAlerts.groupBy { alert -> alert.prisonNumber }, batchSize)

      bulkAlertRepository.saveAndFlush(
        it.toEntity(
          objectMapper = objectMapper,
          requestedAt = context.requestAt,
          requestedBy = context.username,
          requestedByDisplayName = context.userDisplayName,
          existingActiveAlerts = existingActiveAlerts.map { alert -> alert.toBulkAlertAlertModel() },
          alertsCreated = alertsCreated,
          alertsUpdated = emptyList(),
          alertsExpired = emptyList(),
        ),
      ).toModel(objectMapper)
    }

  private fun BulkCreateAlerts.getAlertCode() =
    alertCodeRepository.findByCode(alertCode)?.also {
      require(it.isActive()) { "Alert code '$alertCode' is inactive" }
    } ?: throw IllegalArgumentException("Alert code '$alertCode' not found")

  private fun BulkCreateAlerts.validatePrisonNumbers(batchSize: Int) =
    prisonerSearchClient.getPrisoners(prisonNumbers, batchSize).associateBy { it.prisonerNumber }.also { prisoners ->
      prisonNumbers.filterNot { prisoners.containsKey(it) }.also {
        require(it.isEmpty()) { "Prison number(s) '${it.joinToString("', '")}' not found" }
      }
    }

  private fun BulkCreateAlerts.getExistingUnexpiredAlerts(batchSize: Int) =
    prisonNumbers.chunked(batchSize).flatMap {
      alertRepository.findByPrisonNumberInAndAlertCodeCode(it, alertCode)
        .filter { alert -> alert.isActive() || alert.willBecomeActive() }
    }

  private fun BulkCreateAlerts.createAlerts(context: AlertRequestContext, alertCode: AlertCode, existingActiveAlerts: Map<String, List<Alert>>, batchSize: Int) =
    prisonNumbers.filterNot { existingActiveAlerts.containsKey(it) }.chunked(batchSize).flatMap {
      alertRepository.saveAllAndFlush(
        it.map { prisonNumber ->
          this.toAlertEntity(
            alertCode = alertCode,
            prisonNumber = prisonNumber,
            createdAt = context.requestAt,
            createdBy = context.username,
            createdByDisplayName = context.userDisplayName,
            source = context.source,
            activeCaseLoadId = context.activeCaseLoadId,
          )
        },
      ).map { alert -> alert.toBulkAlertAlertModel() }
    }
}
