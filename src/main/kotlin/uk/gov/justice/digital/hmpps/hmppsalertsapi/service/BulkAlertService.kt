package uk.gov.justice.digital.hmpps.hmppsalertsapi.service

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.hmppsalertsapi.client.prisonersearch.PrisonerSearchClient
import uk.gov.justice.digital.hmpps.hmppsalertsapi.config.AlertRequestContext
import uk.gov.justice.digital.hmpps.hmppsalertsapi.model.BulkAlert
import uk.gov.justice.digital.hmpps.hmppsalertsapi.model.request.BulkCreateAlerts
import uk.gov.justice.digital.hmpps.hmppsalertsapi.repository.AlertCodeRepository
import uk.gov.justice.digital.hmpps.hmppsalertsapi.repository.BulkAlertRepository
import java.time.LocalDateTime
import java.util.UUID

@Service
@Transactional
class BulkAlertService(
  private val bulkAlertRepository: BulkAlertRepository,
  private val alertCodeRepository: AlertCodeRepository,
  private val prisonerSearchClient: PrisonerSearchClient,
) {
  fun bulkCreateAlerts(request: BulkCreateAlerts, context: AlertRequestContext) =
    request.let {
      // Perform database checks first prior to checks that require API calls
      val alertCode = it.getAlertCode()

      // Uses API call
      it.validatePrisonNumbers()

      /*bulkAlertRepository.saveAndFlush(
        it.toAlertEntity(
          alertCode = alertCode,
          createdAt = context.requestAt,
          createdBy = context.username,
          createdByDisplayName = context.userDisplayName,
          source = context.source,
          activeCaseLoadId = context.activeCaseLoadId,
        ),
      ).toAlertModel()*/

      BulkAlert(
        bulkAlertUuid = UUID.randomUUID(),
        request = request,
        requestedAt = context.requestAt,
        requestedBy = context.username,
        requestedByDisplayName = context.userDisplayName,
        completedAt = LocalDateTime.now(),
        successful = true,
        messages = emptyList(),
        alertsCreated = emptyList(),
        alertsUpdated = emptyList(),
        alertsExpired = emptyList(),
      )
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
