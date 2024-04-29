package uk.gov.justice.digital.hmpps.hmppsalertsapi.service

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.hmppsalertsapi.config.AlertRequestContext
import uk.gov.justice.digital.hmpps.hmppsalertsapi.domain.toAlertTypeModel
import uk.gov.justice.digital.hmpps.hmppsalertsapi.domain.toAlertTypeModels
import uk.gov.justice.digital.hmpps.hmppsalertsapi.domain.toEntity
import uk.gov.justice.digital.hmpps.hmppsalertsapi.exceptions.AlertTypeNotFound
import uk.gov.justice.digital.hmpps.hmppsalertsapi.exceptions.ExistingActiveAlertTypeWithCodeException
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

  fun deactivateAlertType(alertType: String, alertRequestContext: AlertRequestContext) =
    alertType.let {
      it.checkAlertTypeExists()
      alertTypeRepository.findByCode(it)!!.apply {
        with(this) {
          deactivatedAt = alertRequestContext.requestAt
          deactivatedBy = alertRequestContext.username
          deactivate()
          alertTypeRepository.saveAndFlush(this)
        }
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
    alertTypeRepository.findByCode(code) != null && throw ExistingActiveAlertTypeWithCodeException("Alert type exists with code '$code'")

  private fun String.checkAlertTypeExists() =
    alertTypeRepository.findByCode(this) == null && throw AlertTypeNotFound("Alert type with code $this could not be found")
}
