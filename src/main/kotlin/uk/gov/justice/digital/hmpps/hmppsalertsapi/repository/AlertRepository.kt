package uk.gov.justice.digital.hmpps.hmppsalertsapi.repository

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.domain.Specification
import org.springframework.data.jpa.repository.EntityGraph
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.hmppsalertsapi.entity.Alert
import java.util.UUID

@Repository
interface AlertRepository : JpaRepository<Alert, UUID> {
  @EntityGraph(value = "alert")
  fun findAll(filter: Specification<Alert>, pageable: Pageable): Page<Alert>

  @EntityGraph(value = "alert")
  fun findByPrisonNumber(prisonNumber: String): Collection<Alert>

  @EntityGraph(value = "alert")
  fun findByPrisonNumberAndAlertCodeCode(prisonNumber: String, alertCode: String): Collection<Alert>

  @EntityGraph(value = "alert")
  fun findByPrisonNumberInAndAlertCodeCode(prisonNumbers: Collection<String>, alertCode: String): Collection<Alert>

  @EntityGraph(value = "alert")
  fun findByPrisonNumberNotInAndAlertCodeCode(prisonNumbers: Collection<String>, alertCode: String): Collection<Alert>

  @Query(value = "select * from alert a where a.id = :alertUuid", nativeQuery = true)
  fun findByAlertUuidIncludingSoftDelete(alertUuid: UUID): Alert?

  @EntityGraph(attributePaths = ["alertCode.alertType", "auditEvents"])
  @Query(
    """
    select a from Alert a 
    where a.prisonNumber in :prisonNumbers
    and (:includeInactive = true or a.activeTo is null or a.activeTo > current_date)
  """,
  )
  fun findByPrisonNumberIn(prisonNumbers: Collection<String>, includeInactive: Boolean): Collection<Alert>
}
