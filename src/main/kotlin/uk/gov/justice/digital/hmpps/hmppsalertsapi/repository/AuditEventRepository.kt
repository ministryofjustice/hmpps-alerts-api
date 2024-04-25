package uk.gov.justice.digital.hmpps.hmppsalertsapi.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.hmppsalertsapi.entity.AuditEvent

@Repository
interface AuditEventRepository : JpaRepository<AuditEvent, Long> {
  fun findAuditEventsByAlertAlertIdInOrderByActionedAtDesc(alertIds: Collection<Long>): Collection<AuditEvent>
}
