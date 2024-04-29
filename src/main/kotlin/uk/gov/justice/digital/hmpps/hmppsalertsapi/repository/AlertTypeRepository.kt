package uk.gov.justice.digital.hmpps.hmppsalertsapi.repository

import org.springframework.data.jpa.repository.EntityGraph
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.hmppsalertsapi.entity.AlertType

@Repository
interface AlertTypeRepository : JpaRepository<AlertType, Long> {
  @EntityGraph(attributePaths = ["alertCodes"])
  override fun findAll(): List<AlertType>

  @EntityGraph(attributePaths = ["alertCodes"])
  fun findByCode(code: String): AlertType?
}
