package uk.gov.justice.digital.hmpps.hmppsalertsapi.service

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppsalertsapi.config.AlertRequestContext
import uk.gov.justice.digital.hmpps.hmppsalertsapi.domain.toAlertCodeModel
import uk.gov.justice.digital.hmpps.hmppsalertsapi.domain.toEntity
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
        alertCodeRepository.saveAndFlush(it.toEntity(context, alertType!!)).toAlertCodeModel()
      }
    }

  fun CreateAlertCodeRequest.checkForExistingAlertCode() =
    alertCodeRepository.findByCode(code) != null && throw ExistingActiveAlertTypeWithCodeException("Alert code exists with code '$code'")

  fun CreateAlertCodeRequest.checkAlertTypeExists() =
    alertTypeRepository.findByCode(parent) == null && throw AlertTypeNotFound("Alert type with code $parent could not be found")
}
