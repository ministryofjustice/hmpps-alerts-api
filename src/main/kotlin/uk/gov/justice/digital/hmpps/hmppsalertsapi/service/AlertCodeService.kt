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

  fun createAlertCode(createAlertCodeRequest: CreateAlertCodeRequest, context: AlertRequestContext): AlertCode = createAlertCodeRequest.let {
    it.checkForExistingAlertCode()
    it.checkAlertTypeExists()
    alertTypeRepository.findByCode(it.parent).let { alertType ->
      val entity = it.toEntity(context, alertType!!)
      with(entity) {
        val toSave = create()
        alertCodeRepository.save(toSave).toAlertCodeModel(context.username)
      }
    }
  }

  private fun CreateAlertCodeRequest.checkForExistingAlertCode() = verifyDoesNotExist(alertCodeRepository.findByCode(code)) { AlreadyExistsException("Alert code", code) }

  private fun CreateAlertCodeRequest.checkAlertTypeExists() = verifyExists(alertTypeRepository.findByCode(parent)) { NotFoundException("Alert type", parent) }

  private fun String.checkAlertCodeExists(): uk.gov.justice.digital.hmpps.hmppsalertsapi.entity.AlertCode = verifyExists(alertCodeRepository.findByCode(this)) { NotFoundException("Alert code", this) }

  @Transactional
  fun deactivateAlertCode(alertCode: String, context: AlertRequestContext): AlertCode = with(alertCode.checkAlertCodeExists()) {
    deactivatedAt = context.requestAt
    deactivatedBy = context.username
    deactivate()
    alertCodeRepository.save(this).toAlertCodeModel(context.username)
  }

  @Transactional
  fun reactivateAlertCode(alertCode: String, context: AlertRequestContext): AlertCode = alertCode.let {
    it.checkAlertCodeExists()
    with(alertCodeRepository.findByCode(it)!!) {
      if (deactivatedAt != null) {
        deactivatedAt = null
        deactivatedBy = null
        reactivate(context.requestAt)
      }
      alertCodeRepository.save(this).toAlertCodeModel(context.username)
    }
  }

  fun retrieveAlertCode(alertCode: String, context: AlertRequestContext): AlertCode = alertCode.let {
    it.checkAlertCodeExists()
    alertCodeRepository.findByCode(it)!!.toAlertCodeModel(context.username)
  }

  fun retrieveAlertCodes(includeInactive: Boolean, context: AlertRequestContext): Collection<AlertCode> = alertCodeRepository.findAll().toAlertCodeModels(context.username, includeInactive)

  fun updateAlertCode(
    alertCode: String,
    updateAlertCodeRequest: UpdateAlertCodeRequest,
    context: AlertRequestContext,
  ): AlertCode = alertCode.let {
    it.checkAlertCodeExists()
    with(alertCodeRepository.findByCode(it)!!) {
      if (description != updateAlertCodeRequest.description) {
        description = updateAlertCodeRequest.description
        modifiedBy = context.username
        modifiedAt = context.requestAt
        update()
      }
      alertCodeRepository.save(this).toAlertCodeModel(context.username)
    }
  }

  @Transactional
  fun setAlertCodeRestrictedStatus(alertCode: String, restrictedStatus: Boolean, context: AlertRequestContext): AlertCode = alertCode.let {
    it.checkAlertCodeExists()
    with(alertCodeRepository.findByCode(it)!!) {
      restricted = restrictedStatus
      alertCodeRepository.save(this).toAlertCodeModel(context.username)
    }
  }
}
