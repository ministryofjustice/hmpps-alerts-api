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
import uk.gov.justice.digital.hmpps.hmppsalertsapi.enumeration.BulkCreateAlertMode.ADD_MISSING
import uk.gov.justice.digital.hmpps.hmppsalertsapi.enumeration.BulkCreateAlertMode.EXPIRE_AND_REPLACE
import uk.gov.justice.digital.hmpps.hmppsalertsapi.model.request.BulkCreateAlerts
import uk.gov.justice.digital.hmpps.hmppsalertsapi.repository.AlertCodeRepository
import uk.gov.justice.digital.hmpps.hmppsalertsapi.repository.AlertRepository
import uk.gov.justice.digital.hmpps.hmppsalertsapi.repository.BulkAlertRepository
import java.time.LocalDate

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

      val alertCode = it.getAlertCode()
      it.validatePrisonNumbers(batchSize)
      val existingUnexpiredAlerts = it.getExistingUnexpiredAlerts(batchSize)
      val existingPermanentlyActiveAlerts = existingUnexpiredAlerts.filter { alert -> alert.isActive() && alert.activeTo == null }.map { alert -> alert.toBulkAlertAlertModel() }

      val alertsUpdated = if (it.mode == ADD_MISSING) existingUnexpiredAlerts.updateToBePermanentlyActive(context) else emptyList()

      val alertsExpired = if (it.mode == EXPIRE_AND_REPLACE) existingUnexpiredAlerts.expire(context) else emptyList()

      val prisonNumbersWithActiveAlerts = existingUnexpiredAlerts.filter { alert -> alert.isActive() }.map { alert -> alert.prisonNumber }.toSet()
      val prisonNumbersWithoutActiveAlerts = it.prisonNumbers.filterNot { prisonNumber -> prisonNumbersWithActiveAlerts.contains(prisonNumber) }

      val alertsCreated = it.createAlerts(context, alertCode, prisonNumbersWithoutActiveAlerts, batchSize)

      bulkAlertRepository.saveAndFlush(
        it.toEntity(
          objectMapper = objectMapper,
          requestedAt = context.requestAt,
          requestedBy = context.username,
          requestedByDisplayName = context.userDisplayName,
          existingActiveAlerts = if (it.mode == ADD_MISSING) existingPermanentlyActiveAlerts else emptyList(),
          alertsCreated = alertsCreated,
          alertsUpdated = alertsUpdated,
          alertsExpired = alertsExpired,
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

  private fun Collection<Alert>.updateToBePermanentlyActive(context: AlertRequestContext) =
    alertRepository.saveAllAndFlush(
      filter { it.activeTo != null }.map {
        val activeFrom = if (it.willBecomeActive()) LocalDate.now() else it.activeFrom
        it.update(
          description = null,
          authorisedBy = null,
          activeFrom = activeFrom,
          activeTo = null,
          appendComment = null,
          updatedAt = context.requestAt,
          updatedBy = context.username,
          updatedByDisplayName = context.userDisplayName,
          source = context.source,
          activeCaseLoadId = context.activeCaseLoadId,
        )
      },
    ).map { alert -> alert.toBulkAlertAlertModel(alert.lastModifiedAuditEvent()!!.description) }

  private fun Collection<Alert>.expire(context: AlertRequestContext) =
    alertRepository.saveAllAndFlush(
      map {
        val activeTo = if (it.willBecomeActive()) it.activeFrom else LocalDate.now()
        it.update(
          description = null,
          authorisedBy = null,
          activeFrom = null,
          activeTo = activeTo,
          appendComment = null,
          updatedAt = context.requestAt,
          updatedBy = context.username,
          updatedByDisplayName = context.userDisplayName,
          source = context.source,
          activeCaseLoadId = context.activeCaseLoadId,
        )
      },
    ).map { alert -> alert.toBulkAlertAlertModel() }

  private fun BulkCreateAlerts.createAlerts(context: AlertRequestContext, alertCode: AlertCode, prisonNumbersWithoutActiveAlerts: Collection<String>, batchSize: Int) =
    prisonNumbersWithoutActiveAlerts.chunked(batchSize).flatMap {
      alertRepository.saveAllAndFlush(
        it.map { prisonNumber ->
          toAlertEntity(
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
