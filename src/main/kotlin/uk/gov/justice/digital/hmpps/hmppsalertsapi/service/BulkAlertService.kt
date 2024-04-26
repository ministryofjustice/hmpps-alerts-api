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
  fun bulkCreateAlerts(request: BulkCreateAlerts, context: AlertRequestContext) =
    request.let {
      // Perform database checks first prior to checks that require API calls
      val alertCode = it.getAlertCode()

      // Uses API call
      it.validatePrisonNumbers()

      val alertsCreated = alertRepository.saveAllAndFlush(
        it.prisonNumbers.map { prisonNumber ->
          it.toAlertEntity(
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

      bulkAlertRepository.saveAndFlush(
        it.toEntity(
          objectMapper = objectMapper,
          requestedAt = context.requestAt,
          requestedBy = context.username,
          requestedByDisplayName = context.userDisplayName,
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

  private fun BulkCreateAlerts.validatePrisonNumbers() =
    prisonerSearchClient.getPrisoners(prisonNumbers).associateBy { it.prisonerNumber }.also { prisoners ->
      prisonNumbers.filterNot { prisoners.containsKey(it) }.also {
        require(it.isEmpty()) { "Prison number(s) '${it.joinToString("', '")}' not found" }
      }
    }
}
