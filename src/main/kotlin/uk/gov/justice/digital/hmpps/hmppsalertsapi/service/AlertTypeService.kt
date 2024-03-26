package uk.gov.justice.digital.hmpps.hmppsalertsapi.service

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppsalertsapi.config.AlertRequestContext
import uk.gov.justice.digital.hmpps.hmppsalertsapi.domain.toAlertTypeModel
import uk.gov.justice.digital.hmpps.hmppsalertsapi.domain.toAlertTypeModels
import uk.gov.justice.digital.hmpps.hmppsalertsapi.domain.toEntity
import uk.gov.justice.digital.hmpps.hmppsalertsapi.exceptions.ExistingActiveAlertTypeWithCodeException
import uk.gov.justice.digital.hmpps.hmppsalertsapi.model.AlertType
import uk.gov.justice.digital.hmpps.hmppsalertsapi.model.request.CreateAlertTypeRequest
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
      alertTypeRepository.saveAndFlush(it.toEntity(context)).toAlertTypeModel(false)
    }

  fun CreateAlertTypeRequest.checkForExistingAlertType() =
    alertTypeRepository.findByCode(code) != null && throw ExistingActiveAlertTypeWithCodeException("Alert type exists with code '$code'")
}
