package uk.gov.justice.digital.hmpps.hmppsalertsapi.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.hmppsalertsapi.entity.AlertCode

@Repository
interface AlertCodeRepository : JpaRepository<AlertCode, Long> {
  fun findByCode(code: String): AlertCode?

  fun findByCodeIn(codes: Collection<String>): Collection<AlertCode>
}
