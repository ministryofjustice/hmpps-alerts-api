package uk.gov.justice.digital.hmpps.hmppsalertsapi.service

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.hmppsalertsapi.config.AlertRequestContext
import uk.gov.justice.digital.hmpps.hmppsalertsapi.domain.toAlertTypeModel
import uk.gov.justice.digital.hmpps.hmppsalertsapi.domain.toAlertTypeModels
import uk.gov.justice.digital.hmpps.hmppsalertsapi.domain.toEntity
import uk.gov.justice.digital.hmpps.hmppsalertsapi.exceptions.AlreadyExistsException
import uk.gov.justice.digital.hmpps.hmppsalertsapi.exceptions.NotFoundException
import uk.gov.justice.digital.hmpps.hmppsalertsapi.exceptions.verifyDoesNotExist
import uk.gov.justice.digital.hmpps.hmppsalertsapi.exceptions.verifyExists
import uk.gov.justice.digital.hmpps.hmppsalertsapi.model.AlertType
import uk.gov.justice.digital.hmpps.hmppsalertsapi.model.request.CreateAlertTypeRequest
import uk.gov.justice.digital.hmpps.hmppsalertsapi.model.request.UpdateAlertTypeRequest
import uk.gov.justice.digital.hmpps.hmppsalertsapi.repository.AlertTypeRepository

@Service
class AlertTypeService(
  private val alertTypeRepository: AlertTypeRepository,
) {
  fun getAlertTypes(includeInactive: Boolean) =
    alertTypeRepository.findAll().toAlertTypeModels(includeInactive)

  fun createAlertType(createAlertTypeRequest: CreateAlertTypeRequest, context: AlertRequestContext): AlertType =
    createAlertTypeRequest.let {
      it.checkForExistingAlertType()
      val entity = it.toEntity(context)
      with(entity) {
        val toSave = create()
        alertTypeRepository.saveAndFlush(toSave).toAlertTypeModel(false)
      }
    }

  fun getAlertType(alertType: String): AlertType =
    alertType.let {
      it.checkAlertTypeExists()
      alertTypeRepository.findByCode(alertType)!!.toAlertTypeModel(true)
    }

  @Transactional
  fun deactivateAlertType(alertType: String, alertRequestContext: AlertRequestContext): AlertType =
    alertType.let {
      it.checkAlertTypeExists()
      with(alertTypeRepository.findByCode(it)!!) {
        deactivatedAt = alertRequestContext.requestAt
        deactivatedBy = alertRequestContext.username
        deactivate()
        alertTypeRepository.saveAndFlush(this).toAlertTypeModel(true)
      }
    }

  @Transactional
  fun reactivateAlertType(alertType: String, alertRequestContext: AlertRequestContext) =
    alertType.let {
      it.checkAlertTypeExists()
      with(alertTypeRepository.findByCode(it)!!) {
        if (deactivatedAt != null) {
          deactivatedAt = null
          deactivatedBy = null
          reactivate(alertRequestContext.requestAt)
        }
        alertTypeRepository.saveAndFlush(this).toAlertTypeModel(false)
      }
    }

  @Transactional
  fun updateAlertType(
    alertType: String,
    updateAlertTypeRequest: UpdateAlertTypeRequest,
    alertRequestContext: AlertRequestContext,
  ): AlertType =
    alertType.let {
      it.checkAlertTypeExists()
      with(alertTypeRepository.findByCode(it)!!) {
        if (description != updateAlertTypeRequest.description) {
          description = updateAlertTypeRequest.description
          modifiedBy = alertRequestContext.username
          modifiedAt = alertRequestContext.requestAt
          update()
        }
        alertTypeRepository.saveAndFlush(this).toAlertTypeModel(false)
      }
    }

  fun CreateAlertTypeRequest.checkForExistingAlertType() =
    verifyDoesNotExist(alertTypeRepository.findByCode(code)) { AlreadyExistsException("Alert type", code) }

  private fun String.checkAlertTypeExists() =
    verifyExists(alertTypeRepository.findByCode(this)) { NotFoundException("Alert type", this) }
}
