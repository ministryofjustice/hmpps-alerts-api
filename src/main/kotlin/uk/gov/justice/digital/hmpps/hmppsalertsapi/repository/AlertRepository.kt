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
interface AlertRepository : JpaRepository<Alert, Long> {
  @EntityGraph(value = "alert")
  fun findAll(filter: Specification<Alert>, pageable: Pageable): Page<Alert>

  fun findByAlertUuid(alertUuid: UUID): Alert?

  @EntityGraph(value = "alert")
  fun findByPrisonNumber(prisonNumber: String): Collection<Alert>

  @EntityGraph(value = "alert")
  fun findByPrisonNumberAndAlertCodeCode(prisonNumber: String, alertCode: String): Collection<Alert>

  @EntityGraph(value = "alert")
  fun findByPrisonNumberInAndAlertCodeCode(prisonNumbers: Collection<String>, alertCode: String): Collection<Alert>

  @EntityGraph(value = "alert")
  fun findByPrisonNumberNotInAndAlertCodeCode(prisonNumbers: Collection<String>, alertCode: String): Collection<Alert>

  @Query(value = "SELECT * FROM alert a WHERE a.alert_uuid = :alertUuid", nativeQuery = true)
  fun findByAlertUuidIncludingSoftDelete(alertUuid: UUID): Alert?

  @EntityGraph(value = "alert")
  fun findByPrisonNumberInOrderByActiveFromDesc(prisonNumbers: Collection<String>): Collection<Alert>
}
