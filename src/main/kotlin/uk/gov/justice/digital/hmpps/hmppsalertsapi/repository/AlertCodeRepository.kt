package uk.gov.justice.digital.hmpps.hmppsalertsapi.repository

import org.springframework.data.jpa.repository.EntityGraph
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.hmppsalertsapi.entity.AlertCode
import uk.gov.justice.digital.hmpps.hmppsalertsapi.exceptions.NotFoundException

@Repository
interface AlertCodeRepository : JpaRepository<AlertCode, Long> {
  @EntityGraph(attributePaths = ["alertType", "privilegedUsers"])
  fun findByCode(code: String): AlertCode?

  @EntityGraph(attributePaths = ["alertType", "privilegedUsers"])
  fun findByCodeIn(codes: Collection<String>): Collection<AlertCode>

  @Query("select ac.alertCodeId from AlertCode ac where ac.code = :code")
  fun getAlertCodeIdForCode(code: String): Long?
}

fun AlertCodeRepository.getByCode(code: String) = findByCode(code) ?: throw NotFoundException("Alert code", code)
