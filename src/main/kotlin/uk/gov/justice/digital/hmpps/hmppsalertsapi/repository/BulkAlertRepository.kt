package uk.gov.justice.digital.hmpps.hmppsalertsapi.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.hmppsalertsapi.entity.BulkAlert
import java.util.UUID

@Repository
interface BulkAlertRepository : JpaRepository<BulkAlert, Long> {
  fun findByBulkAlertUuid(bulkAlertUuid: UUID): BulkAlert?
}
