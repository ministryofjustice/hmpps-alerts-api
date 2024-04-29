package uk.gov.justice.digital.hmpps.hmppsalertsapi.service

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppsalertsapi.config.AlertRequestContext
import uk.gov.justice.digital.hmpps.hmppsalertsapi.domain.toAlertCodeModel
import uk.gov.justice.digital.hmpps.hmppsalertsapi.domain.toAlertCodeModels
import uk.gov.justice.digital.hmpps.hmppsalertsapi.domain.toEntity
import uk.gov.justice.digital.hmpps.hmppsalertsapi.exceptions.AlertCodeNotFoundException
import uk.gov.justice.digital.hmpps.hmppsalertsapi.exceptions.AlertTypeNotFound
import uk.gov.justice.digital.hmpps.hmppsalertsapi.exceptions.ExistingActiveAlertTypeWithCodeException
import uk.gov.justice.digital.hmpps.hmppsalertsapi.model.AlertCode
import uk.gov.justice.digital.hmpps.hmppsalertsapi.model.request.CreateAlertCodeRequest
import uk.gov.justice.digital.hmpps.hmppsalertsapi.repository.AlertCodeRepository
import uk.gov.justice.digital.hmpps.hmppsalertsapi.repository.AlertTypeRepository

@Service
class AlertCodeService(
  private val alertCodeRepository: AlertCodeRepository,
  private val alertTypeRepository: AlertTypeRepository,
) {

  fun createAlertCode(createAlertCodeRequest: CreateAlertCodeRequest, context: AlertRequestContext): AlertCode =
    createAlertCodeRequest.let {
      it.checkForExistingAlertCode()
      it.checkAlertTypeExists()
      alertTypeRepository.findByCode(it.parent).let { alertType ->
        val entity = it.toEntity(context, alertType!!)
        with(entity) {
          val toSave = create()
          alertCodeRepository.saveAndFlush(toSave).toAlertCodeModel()
        }
      }
    }

  private fun CreateAlertCodeRequest.checkForExistingAlertCode() =
    alertCodeRepository.findByCode(code) != null && throw ExistingActiveAlertTypeWithCodeException("Alert code exists with code '$code'")

  private fun CreateAlertCodeRequest.checkAlertTypeExists() =
    alertTypeRepository.findByCode(parent) == null && throw AlertTypeNotFound("Alert type with code $parent could not be found")

  private fun String.checkAlertCodeExists() =
    alertCodeRepository.findByCode(this) == null && throw AlertCodeNotFoundException("Alert with code $this could not be found")

  fun deactivateAlertCode(alertCode: String, alertRequestContext: AlertRequestContext) {
    alertCode.let {
      it.checkAlertCodeExists()
      alertCodeRepository.findByCode(it)!!.apply {
        with(this) {
          deactivatedAt = alertRequestContext.requestAt
          deactivatedBy = alertRequestContext.username
          deactivate()
          alertCodeRepository.saveAndFlush(this)
        }
      }
    }
  }

  fun retrieveAlertCode(alertCode: String): AlertCode =
    alertCode.let {
      it.checkAlertCodeExists()
      alertCodeRepository.findByCode(it)!!.toAlertCodeModel()
    }

  fun retrieveAlertCodes(includeInactive: Boolean): Collection<AlertCode> =
    alertCodeRepository.findAll().toAlertCodeModels(includeInactive)
}
