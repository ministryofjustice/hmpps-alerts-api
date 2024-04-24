package uk.gov.justice.digital.hmpps.hmppsalertsapi.repository

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.domain.Specification
import org.springframework.data.jpa.repository.EntityGraph
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.hmppsalertsapi.entity.Alert
import uk.gov.justice.digital.hmpps.hmppsalertsapi.entity.AuditEvent
import uk.gov.justice.digital.hmpps.hmppsalertsapi.entity.Comment
import java.util.UUID

@Repository
interface AlertRepository : JpaRepository<Alert, Long> {
  @EntityGraph(value = "alert")
  fun findAll(filter: Specification<Alert>, pageable: Pageable): Page<Alert>

  fun findByAlertUuid(alertUuid: UUID): Alert?

  @EntityGraph(value = "alert")
  fun findByPrisonNumber(prisonNumber: String): Collection<Alert>

  @EntityGraph(value = "alert")
  fun findByPrisonNumberAndAlertCodeCode(prisonNumber: String, alertCode: String): Collection<Alert>

  @Query(
    value = "SELECT * FROM alert a WHERE a.alert_uuid = :alertUuid",
    nativeQuery = true,
  )
  fun findByAlertUuidIncludingSoftDelete(alertUuid: UUID): Alert?

  @EntityGraph(value = "alert")
  fun findByPrisonNumberInOrderByActiveFromDesc(prisonNumbers: Collection<String>): Collection<Alert>

  @Query(
    value = "SELECT c FROM Comment c WHERE c.alert.alertUuid = :alertUuid ORDER BY c.createdAt DESC",
  )
  fun findCommentsByAlertUuidOrderByCreatedAtDesc(alertUuid: UUID): Collection<Comment>

  @Query(
    value = "SELECT c FROM Comment c WHERE c.alert.alertId IN :alertIds ORDER BY c.createdAt DESC",
  )
  fun findCommentsByAlertIdInOrderByCreatedAtDesc(alertIds: Collection<Long>): Collection<Comment>

  @Query(
    value = "SELECT ae FROM AuditEvent ae WHERE ae.alert.alertUuid = :alertUuid ORDER BY ae.actionedAt DESC",
  )
  fun findAuditEventsByAlertUuidOrderByActionedAtDesc(alertUuid: UUID): Collection<AuditEvent>

  @Query(
    value = "SELECT ae FROM AuditEvent ae WHERE ae.alert.alertId IN :alertIds ORDER BY ae.actionedAt DESC",
  )
  fun findAuditEventsByAlertIdInOrderByActionedAtDesc(alertIds: Collection<Long>): Collection<AuditEvent>
}
