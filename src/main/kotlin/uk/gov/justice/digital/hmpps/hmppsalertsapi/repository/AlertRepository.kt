package uk.gov.justice.digital.hmpps.hmppsalertsapi.repository

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.domain.Specification
import org.springframework.data.jpa.repository.EntityGraph
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.JpaSpecificationExecutor
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.hmppsalertsapi.entity.Alert
import java.time.LocalDate
import java.util.UUID

@Repository
interface AlertRepository : JpaRepository<Alert, UUID>, JpaSpecificationExecutor<Alert> {
  @EntityGraph(value = "alert")
  override fun findAll(filter: Specification<Alert>, pageable: Pageable): Page<Alert>

  @EntityGraph(value = "alert")
  fun findByPrisonNumber(prisonNumber: String): Collection<Alert>

  @EntityGraph(value = "alert")
  fun findByPrisonNumberAndAlertCodeCode(prisonNumber: String, alertCode: String): Collection<Alert>

  @Query(value = "select * from alert a where a.id = :alertUuid", nativeQuery = true)
  fun findByAlertUuidIncludingSoftDelete(alertUuid: UUID): Alert?

  @Query(
    """
    select a from Alert a
    join fetch a.alertCode ac
    join fetch ac.alertType at
    join fetch a.auditEvents ae
    where a.prisonNumber in :prisonNumbers
    and (:includeInactive = true or a.activeTo is null or a.activeTo > current_date)
  """,
  )
  fun findByPrisonNumberIn(prisonNumbers: Collection<String>, includeInactive: Boolean): Collection<Alert>

  @EntityGraph(value = "alert")
  fun findAllByActiveTo(activeTo: LocalDate): List<Alert>

  @Query(
    """
    select a from Alert a
    join fetch a.alertCode ac
    join fetch ac.alertType at
    join fetch a.auditEvents ae
    where ac.code = :code
    and (a.activeTo is null or a.activeTo > current_date)
  """,
  )
  fun findAllActiveByCode(code: String): List<Alert>
}
