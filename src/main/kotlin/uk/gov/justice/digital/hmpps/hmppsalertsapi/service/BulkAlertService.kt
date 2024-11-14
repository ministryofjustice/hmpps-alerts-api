package uk.gov.justice.digital.hmpps.hmppsalertsapi.service

import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.hmppsalertsapi.client.prisonersearch.PrisonerSearchClient
import uk.gov.justice.digital.hmpps.hmppsalertsapi.common.aop.PublishPersonAlertsChanged
import uk.gov.justice.digital.hmpps.hmppsalertsapi.config.AlertRequestContext
import uk.gov.justice.digital.hmpps.hmppsalertsapi.domain.toAlertEntity
import uk.gov.justice.digital.hmpps.hmppsalertsapi.domain.toBulkAlertAlertModel
import uk.gov.justice.digital.hmpps.hmppsalertsapi.domain.toEntity
import uk.gov.justice.digital.hmpps.hmppsalertsapi.domain.toModel
import uk.gov.justice.digital.hmpps.hmppsalertsapi.entity.Alert
import uk.gov.justice.digital.hmpps.hmppsalertsapi.entity.AlertCode
import uk.gov.justice.digital.hmpps.hmppsalertsapi.enumeration.BulkCreateAlertCleanupMode.EXPIRE_FOR_PRISON_NUMBERS_NOT_SPECIFIED
import uk.gov.justice.digital.hmpps.hmppsalertsapi.exceptions.InvalidInputException
import uk.gov.justice.digital.hmpps.hmppsalertsapi.exceptions.verifyExists
import uk.gov.justice.digital.hmpps.hmppsalertsapi.model.BulkAlertAlert
import uk.gov.justice.digital.hmpps.hmppsalertsapi.model.BulkAlertPlan
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
  @PublishPersonAlertsChanged
  fun bulkCreateAlerts(bulk: BulkCreateAlerts, context: AlertRequestContext, batchSize: Int = 1000) =
    bulk.let {
      require(batchSize in 1..1000) {
        "Batch size must be between 1 and 1000"
      }

      val alertCode = it.getAlertCode()
      it.validatePrisonNumbers(batchSize)

      val existingUnexpiredAlerts = it.getExistingUnexpiredAlerts(batchSize)

      // The order of these procedures is important. They action the request based on the logic for mode and cleanup mode
      val existingActiveAlerts = it.getAnyExistingActiveAlertsThatWillNotBeRecreated(existingUnexpiredAlerts)
      val alertsUpdated =
        it.updateRelevantExistingUnexpiredAlertsToBePermanentlyActive(context, existingUnexpiredAlerts, batchSize)
      val alertsExpired = it.expireRelevantExistingUnexpiredAlertsAndAlertsForPrisonNumbersNotInRequest(
        context,
        batchSize,
      )
      val alertsCreated = it.createAlertsWhereNoActiveAlertFromPreviousActionsExists(
        context,
        alertCode,
        existingUnexpiredAlerts,
        batchSize,
      )

      bulkAlertRepository.save(
        it.toEntity(
          objectMapper = objectMapper,
          requestedAt = context.requestAt,
          requestedBy = context.username,
          requestedByDisplayName = context.userDisplayName,
          existingActiveAlerts = existingActiveAlerts,
          alertsCreated = alertsCreated,
          alertsUpdated = alertsUpdated,
          alertsExpired = alertsExpired,
        ),
      ).toModel(objectMapper)
    }

  @Transactional(readOnly = true)
  fun planBulkCreateAlerts(bulk: BulkCreateAlerts, context: AlertRequestContext, batchSize: Int = 1000) =
    bulk.let { request ->
      require(batchSize in 1..1000) {
        "Batch size must be between 1 and 1000"
      }
      request.getAlertCode()
      request.validatePrisonNumbers(batchSize)

      val existingUnexpiredAlerts = request.getExistingUnexpiredAlerts(batchSize)
      val existingActiveAlerts = existingUnexpiredAlerts.filter { it.activeTo == null }
      val alertsUpdated = existingUnexpiredAlerts.filter { it.activeTo != null }
      val alertsExpired = request.getAlertsToBeExpired()
      val prisonNumbersWithActiveAlerts = existingUnexpiredAlerts.map { it.prisonNumber }.toSet()
      val prisonNumbersWithoutActiveAlerts = request.prisonNumbers.filterNot { prisonNumbersWithActiveAlerts.contains(it) }

      BulkAlertPlan(
        request = request,
        existingActiveAlertsPrisonNumbers = existingActiveAlerts.map { it.prisonNumber },
        alertsToBeCreatedForPrisonNumbers = prisonNumbersWithoutActiveAlerts,
        alertsToBeUpdatedForPrisonNumbers = alertsUpdated.map { it.prisonNumber },
        alertsToBeExpiredForPrisonNumbers = alertsExpired.map { it.prisonNumber },
      )
    }

  private fun BulkCreateAlerts.getAlertCode() =
    verifyExists(alertCodeRepository.findByCode(alertCode)) {
      InvalidInputException("Alert code", alertCode)
    }.also {
      require(it.isActive()) { "Alert code is inactive" }
    }

  private fun BulkCreateAlerts.validatePrisonNumbers(batchSize: Int) =
    prisonerSearchClient.getPrisoners(prisonNumbers, batchSize).associateBy { it.prisonerNumber }.also { prisoners ->
      prisonNumbers.filterNot { prisoners.containsKey(it) }.also {
        require(it.isEmpty()) { "Prison number(s) not found" }
      }
    }

  private fun BulkCreateAlerts.getExistingUnexpiredAlerts(batchSize: Int) =
    prisonNumbers.chunked(batchSize).flatMap {
      alertRepository.findByPrisonNumberInAndAlertCodeCode(it, alertCode)
        .filter { alert -> alert.isActive() }
    }

  private fun BulkCreateAlerts.getAnyExistingActiveAlertsThatWillNotBeRecreated(existingUnexpiredAlerts: Collection<Alert>) =
    existingUnexpiredAlerts.filter { it.isActive() && it.activeTo == null }
      .map { it.toBulkAlertAlertModel() }

  private fun BulkCreateAlerts.updateRelevantExistingUnexpiredAlertsToBePermanentlyActive(
    context: AlertRequestContext,
    existingUnexpiredAlerts: Collection<Alert>,
    batchSize: Int,
  ) = existingUnexpiredAlerts.updateToBePermanentlyActive(this, context, batchSize)

  private fun BulkCreateAlerts.getAlertsToBeExpired() = if (cleanupMode == EXPIRE_FOR_PRISON_NUMBERS_NOT_SPECIFIED) {
    alertRepository.findByPrisonNumberNotInAndAlertCodeCode(prisonNumbers, alertCode).filter { it.isActive() }
  } else {
    emptyList()
  }

  private fun BulkCreateAlerts.expireRelevantExistingUnexpiredAlertsAndAlertsForPrisonNumbersNotInRequest(
    context: AlertRequestContext,
    batchSize: Int,
  ) = getAlertsToBeExpired().expire(context, batchSize)

  private fun Collection<Alert>.updateToBePermanentlyActive(
    bulk: BulkCreateAlerts,
    context: AlertRequestContext,
    batchSize: Int,
  ) =
    chunked(batchSize).flatMap {
      alertRepository.saveAll(
        filter { it.activeTo != null }.map {
          it.update(
            description = bulk.description,
            authorisedBy = null,
            activeFrom = it.activeFrom,
            activeTo = null,
            updatedAt = context.requestAt,
            updatedBy = context.username,
            updatedByDisplayName = context.userDisplayName,
            source = context.source,
            activeCaseLoadId = context.activeCaseLoadId,
          )
        },
      ).map { alert -> alert.toBulkAlertAlertModel(alert.lastModifiedAuditEvent()!!.description) }
    }

  private fun Collection<Alert>.expire(context: AlertRequestContext, batchSize: Int) =
    chunked(batchSize).flatMap {
      alertRepository.saveAll(
        map {
          it.update(
            description = null,
            authorisedBy = null,
            activeFrom = null,
            activeTo = LocalDate.now(),
            updatedAt = context.requestAt,
            updatedBy = context.username,
            updatedByDisplayName = context.userDisplayName,
            source = context.source,
            activeCaseLoadId = context.activeCaseLoadId,
          )
        },
      ).map { alert -> alert.toBulkAlertAlertModel() }
    }

  private fun BulkCreateAlerts.createAlertsWhereNoActiveAlertFromPreviousActionsExists(
    context: AlertRequestContext,
    alertCode: AlertCode,
    existingUnexpiredAlerts: Collection<Alert>,
    batchSize: Int,
  ): Collection<BulkAlertAlert> {
    val prisonNumbersWithActiveAlerts = existingUnexpiredAlerts.filter { it.isActive() }.map { it.prisonNumber }.toSet()
    val prisonNumbersWithoutActiveAlerts = prisonNumbers.filterNot { prisonNumbersWithActiveAlerts.contains(it) }

    return prisonNumbersWithoutActiveAlerts.chunked(batchSize).flatMap {
      alertRepository.saveAll(
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
}
