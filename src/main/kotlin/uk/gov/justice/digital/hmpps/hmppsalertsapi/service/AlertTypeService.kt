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
  fun getAlertTypes(includeInactive: Boolean, context: AlertRequestContext) = alertTypeRepository.findAll().toAlertTypeModels(context.username, includeInactive)

  fun createAlertType(createAlertTypeRequest: CreateAlertTypeRequest, context: AlertRequestContext): AlertType = createAlertTypeRequest.let {
    it.checkForExistingAlertType()
    val entity = it.toEntity(context)
    with(entity) {
      val toSave = create()
      alertTypeRepository.save(toSave).toAlertTypeModel(context.username, false)
    }
  }

  fun getAlertType(alertType: String, context: AlertRequestContext): AlertType = alertType.let {
    it.checkAlertTypeExists()
    alertTypeRepository.findByCode(alertType)!!.toAlertTypeModel(context.username, true)
  }

  @Transactional
  fun deactivateAlertType(alertType: String, context: AlertRequestContext): AlertType = alertType.let {
    it.checkAlertTypeExists()
    with(alertTypeRepository.findByCode(it)!!) {
      deactivatedAt = context.requestAt
      deactivatedBy = context.username
      deactivate()
      alertTypeRepository.save(this).toAlertTypeModel(context.username, true)
    }
  }

  @Transactional
  fun reactivateAlertType(alertType: String, context: AlertRequestContext) = alertType.let {
    it.checkAlertTypeExists()
    with(alertTypeRepository.findByCode(it)!!) {
      if (deactivatedAt != null) {
        deactivatedAt = null
        deactivatedBy = null
        reactivate(context.requestAt)
      }
      alertTypeRepository.save(this).toAlertTypeModel(context.username, false)
    }
  }

  @Transactional
  fun updateAlertType(
    alertType: String,
    updateAlertTypeRequest: UpdateAlertTypeRequest,
    context: AlertRequestContext,
  ): AlertType = alertType.let {
    it.checkAlertTypeExists()
    with(alertTypeRepository.findByCode(it)!!) {
      if (description != updateAlertTypeRequest.description) {
        description = updateAlertTypeRequest.description
        modifiedBy = context.username
        modifiedAt = context.requestAt
        update()
      }
      alertTypeRepository.save(this).toAlertTypeModel(context.username, false)
    }
  }

  fun CreateAlertTypeRequest.checkForExistingAlertType() = verifyDoesNotExist(alertTypeRepository.findByCode(code)) { AlreadyExistsException("Alert type", code) }

  private fun String.checkAlertTypeExists() = verifyExists(alertTypeRepository.findByCode(this)) { NotFoundException("Alert type", this) }
}
