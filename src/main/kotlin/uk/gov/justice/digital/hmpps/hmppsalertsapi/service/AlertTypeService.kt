package uk.gov.justice.digital.hmpps.hmppsalertsapi.service

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppsalertsapi.domain.toAlertTypeModels
import uk.gov.justice.digital.hmpps.hmppsalertsapi.repository.AlertTypeRepository

@Service
class AlertTypeService(
  private val alertTypeRepository: AlertTypeRepository,
) {
  fun getAlertTypes(includeInactive: Boolean) =
    alertTypeRepository.findAll().toAlertTypeModels(includeInactive)
}
