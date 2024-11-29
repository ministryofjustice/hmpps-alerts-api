package uk.gov.justice.digital.hmpps.hmppsalertsapi.entity

import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import jakarta.persistence.Version
import org.springframework.data.jpa.repository.JpaRepository
import uk.gov.justice.digital.hmpps.hmppsalertsapi.IdGenerator.newUuid
import uk.gov.justice.digital.hmpps.hmppsalertsapi.config.AlertRequestContext.Companion.get
import java.time.LocalDateTime
import java.util.UUID

@Entity
@Table(name = "bulk_plan")
class BulkPlan(
  @ManyToOne
  @JoinColumn(name = "alert_code_id")
  var alertCode: AlertCode? = null,
  @Id
  val id: UUID = newUuid(),
) {
  @Version
  val version: Int? = null

  val createdAt: LocalDateTime = get().requestAt
  val createdBy: String = get().username
  val createdByDisplayName: String = get().userDisplayName
}

interface BulkPlanRepository : JpaRepository<BulkPlan, UUID>
