package uk.gov.justice.digital.hmpps.hmppsalertsapi.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.hmppsalertsapi.entity.AlertCodePrivilegedUser
import uk.gov.justice.digital.hmpps.hmppsalertsapi.entity.AlertCodePrivilegedUserId

@Repository
interface AlertCodePrivilegedUserRepository : JpaRepository<AlertCodePrivilegedUser, AlertCodePrivilegedUserId> {
  fun findByAlertCodeIdAndUsername(alertCodeId: Long, username: String): AlertCodePrivilegedUser?
}

fun AlertCodePrivilegedUserRepository.userCanAdministerAlertCode(alertCodeId: Long, username: String): Boolean =
  findByAlertCodeIdAndUsername(alertCodeId, username) != null