package uk.gov.justice.digital.hmpps.hmppsalertsapi.service

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.hmppsalertsapi.config.AlertRequestContext
import uk.gov.justice.digital.hmpps.hmppsalertsapi.domain.toAlertCodeModel
import uk.gov.justice.digital.hmpps.hmppsalertsapi.domain.toAlertCodeModels
import uk.gov.justice.digital.hmpps.hmppsalertsapi.domain.toEntity
import uk.gov.justice.digital.hmpps.hmppsalertsapi.exceptions.AlreadyExistsException
import uk.gov.justice.digital.hmpps.hmppsalertsapi.exceptions.NotFoundException
import uk.gov.justice.digital.hmpps.hmppsalertsapi.exceptions.verifyDoesNotExist
import uk.gov.justice.digital.hmpps.hmppsalertsapi.exceptions.verifyExists
import uk.gov.justice.digital.hmpps.hmppsalertsapi.model.AlertCode
import uk.gov.justice.digital.hmpps.hmppsalertsapi.model.request.CreateAlertCodeRequest
import uk.gov.justice.digital.hmpps.hmppsalertsapi.model.request.UpdateAlertCodeRequest
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
          alertCodeRepository.save(toSave).toAlertCodeModel()
        }
      }
    }

  private fun CreateAlertCodeRequest.checkForExistingAlertCode() =
    verifyDoesNotExist(alertCodeRepository.findByCode(code)) { AlreadyExistsException("Alert code", code) }

  private fun CreateAlertCodeRequest.checkAlertTypeExists() =
    verifyExists(alertTypeRepository.findByCode(parent)) { NotFoundException("Alert type", parent) }

  private fun String.checkAlertCodeExists() =
    verifyExists(alertCodeRepository.findByCode(this)) { NotFoundException("Alert code", this) }

  @Transactional
  fun deactivateAlertCode(alertCode: String, alertRequestContext: AlertRequestContext): AlertCode =
    alertCode.let {
      it.checkAlertCodeExists()
      with(alertCodeRepository.findByCode(it)!!) {
        deactivatedAt = alertRequestContext.requestAt
        deactivatedBy = alertRequestContext.username
        deactivate()
        alertCodeRepository.save(this).toAlertCodeModel()
      }
    }

  @Transactional
  fun reactivateAlertCode(alertCode: String, alertRequestContext: AlertRequestContext): AlertCode =
    alertCode.let {
      it.checkAlertCodeExists()
      with(alertCodeRepository.findByCode(it)!!) {
        if (deactivatedAt != null) {
          deactivatedAt = null
          deactivatedBy = null
          reactivate(alertRequestContext.requestAt)
        }
        alertCodeRepository.save(this).toAlertCodeModel()
      }
    }

  fun retrieveAlertCode(alertCode: String): AlertCode =
    alertCode.let {
      it.checkAlertCodeExists()
      alertCodeRepository.findByCode(it)!!.toAlertCodeModel()
    }

  fun retrieveAlertCodes(includeInactive: Boolean): Collection<AlertCode> =
    alertCodeRepository.findAll().toAlertCodeModels(includeInactive)

  fun updateAlertCode(
    alertCode: String,
    updateAlertCodeRequest: UpdateAlertCodeRequest,
    alertRequestContext: AlertRequestContext,
  ): AlertCode =
    alertCode.let {
      it.checkAlertCodeExists()
      with(alertCodeRepository.findByCode(it)!!) {
        if (description != updateAlertCodeRequest.description) {
          description = updateAlertCodeRequest.description
          modifiedBy = alertRequestContext.username
          modifiedAt = alertRequestContext.requestAt
          update()
        }
        alertCodeRepository.save(this).toAlertCodeModel()
      }
    }
}
