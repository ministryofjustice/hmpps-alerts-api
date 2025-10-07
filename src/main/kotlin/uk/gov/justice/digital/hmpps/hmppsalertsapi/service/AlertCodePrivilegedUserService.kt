package uk.gov.justice.digital.hmpps.hmppsalertsapi.service

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.hmppsalertsapi.entity.AlertCodePrivilegedUser
import uk.gov.justice.digital.hmpps.hmppsalertsapi.entity.AlertCodePrivilegedUserId
import uk.gov.justice.digital.hmpps.hmppsalertsapi.exceptions.NotFoundException
import uk.gov.justice.digital.hmpps.hmppsalertsapi.repository.AlertCodePrivilegedUserRepository
import uk.gov.justice.digital.hmpps.hmppsalertsapi.repository.AlertCodeRepository

@Service
class AlertCodePrivilegedUserService(
  private val alertCodeRepository: AlertCodeRepository,
  private val alertCodePrivilegedUserRepository: AlertCodePrivilegedUserRepository,
) {

  @Transactional
  fun addPrivilegedUser(alertCode: String, username: String) {
    val alertCodeId = alertCodeRepository.getAlertCodeIdForCode(alertCode)
      ?: throw NotFoundException("Alert code", alertCode)
    alertCodePrivilegedUserRepository.save(AlertCodePrivilegedUser(alertCodeId, username))
  }

  @Transactional
  fun removePrivilegedUser(alertCode: String, username: String) = alertCode.let {
    val alertCodeId = alertCodeRepository.getAlertCodeIdForCode(alertCode)
      ?: throw NotFoundException("Alert code", alertCode)
    alertCodePrivilegedUserRepository.deleteById(AlertCodePrivilegedUserId(alertCodeId, username))
  }
}
