package uk.gov.justice.digital.hmpps.hmppsalertsapi.repository

import org.springframework.data.jpa.repository.EntityGraph
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.hmppsalertsapi.entity.AuditEvent
import java.time.LocalDateTime
import java.util.UUID

@Repository
interface AuditEventRepository : JpaRepository<AuditEvent, Long> {
  fun findAuditEventsByAlertIdInOrderByActionedAtDesc(alertIds: Collection<UUID>): Collection<AuditEvent>

  @EntityGraph(attributePaths = ["alert.alertCode.alertType"])
  fun findByActionedByAndActionedAt(actionedBy: String, actionedAt: LocalDateTime): List<AuditEvent>
}
