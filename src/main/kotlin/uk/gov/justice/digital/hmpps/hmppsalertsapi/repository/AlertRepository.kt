package uk.gov.justice.digital.hmpps.hmppsalertsapi.repository

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.hmppsalertsapi.entity.Alert
import java.util.UUID

@Repository
interface AlertRepository : JpaRepository<Alert, Long> {
  fun findByAlertUuid(alertUuid: UUID): Alert?

  fun findByPrisonNumberAndAlertCodeCode(prisonNumber: String, alertCode: String): Collection<Alert>

  @Query(
    value = "SELECT * from alert a where a.alert_uuid = :alertUuid",
    nativeQuery = true,
  )
  fun findByAlertUuidIncludingSoftDelete(alertUuid: UUID): Alert?

  fun findAllByPrisonNumber(prisonNumber: String, pageable: Pageable): Page<Alert>
}
