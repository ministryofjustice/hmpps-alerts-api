package uk.gov.justice.digital.hmpps.hmppsalertsapi.entity

import jakarta.persistence.CascadeType
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.JoinTable
import jakarta.persistence.ManyToMany
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import jakarta.persistence.Version
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.repository.findByIdOrNull
import uk.gov.justice.digital.hmpps.hmppsalertsapi.IdGenerator.newUuid
import uk.gov.justice.digital.hmpps.hmppsalertsapi.config.AlertRequestContext.Companion.get
import uk.gov.justice.digital.hmpps.hmppsalertsapi.exceptions.NotFoundException
import java.time.LocalDateTime
import java.util.UUID

@Entity
@Table(name = "bulk_plan")
class BulkPlan(
  @ManyToOne
  @JoinColumn(name = "alert_code_id")
  var alertCode: AlertCode? = null,

  var description: String? = null,
  @Id
  val id: UUID = newUuid(),
) {
  @Version
  val version: Int? = null

  val createdAt: LocalDateTime = get().requestAt
  val createdBy: String = get().username
  val createdByDisplayName: String = get().userDisplayName

  @ManyToMany(cascade = [CascadeType.PERSIST, CascadeType.MERGE])
  @JoinTable(
    name = "plan_person",
    joinColumns = [JoinColumn(name = "plan_id")],
    inverseJoinColumns = [JoinColumn(name = "prison_number")],
  )
  val people: MutableList<PersonSummary> = mutableListOf()
}

interface BulkPlanRepository : JpaRepository<BulkPlan, UUID>

fun BulkPlanRepository.getPlan(id: UUID) = findByIdOrNull(id) ?: throw NotFoundException("Plan", id.toString())
