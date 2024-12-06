package uk.gov.justice.digital.hmpps.hmppsalertsapi.entity

import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.JoinTable
import jakarta.persistence.ManyToMany
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import jakarta.persistence.Version
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.findByIdOrNull
import uk.gov.justice.digital.hmpps.hmppsalertsapi.IdGenerator.newUuid
import uk.gov.justice.digital.hmpps.hmppsalertsapi.config.AlertRequestContext
import uk.gov.justice.digital.hmpps.hmppsalertsapi.config.AlertRequestContext.Companion.get
import uk.gov.justice.digital.hmpps.hmppsalertsapi.enumeration.BulkAlertCleanupMode
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

  @Enumerated(EnumType.STRING)
  var cleanupMode: BulkAlertCleanupMode? = null,

  @Id
  val id: UUID = newUuid(),
) {
  @Version
  val version: Int? = null

  val createdAt: LocalDateTime = get().requestAt
  val createdBy: String = get().username
  val createdByDisplayName: String = get().userDisplayName

  @ManyToMany
  @JoinTable(
    name = "plan_person",
    joinColumns = [JoinColumn(name = "plan_id")],
    inverseJoinColumns = [JoinColumn(name = "prison_number")],
  )
  val people: MutableSet<PersonSummary> = mutableSetOf()

  var startedAt: LocalDateTime? = null
  var startedBy: String? = null
  var startedByDisplayName: String? = null

  var completedAt: LocalDateTime? = null
  var createdCount: Int? = null
  var updatedCount: Int? = null
  var unchangedCount: Int? = null
  var expiredCount: Int? = null

  fun ready(): Pair<AlertCode, BulkAlertCleanupMode> {
    val alertCode = checkNotNull(alertCode) {
      "Unable to calculate affect of plan until the alert code is selected"
    }
    val cleanupMode = checkNotNull(cleanupMode) {
      "Unable to calculate affect of plan until the cleanup mode is selected"
    }
    return alertCode to cleanupMode
  }

  fun start(context: AlertRequestContext) = apply {
    startedAt = context.requestAt
    startedBy = context.username
    startedByDisplayName = context.userDisplayName
  }

  fun completed(created: Int, updated: Int, unchanged: Int, expired: Int) = apply {
    completedAt = LocalDateTime.now()
    createdCount = created
    updatedCount = updated
    unchangedCount = unchanged
    expiredCount = expired
  }

  companion object {
    const val BULK_ALERT_USERNAME = "BULK_ALERTS"
    const val BULK_ALERT_DISPLAY_NAME = "Bulk Alerts Operation"
  }
}

interface BulkPlanRepository : JpaRepository<BulkPlan, UUID> {
  @Query(
    """
    with existing_alerts as (select a.id as id, a.prisonNumber as pn, a.activeFrom as activeFrom, a.activeTo as activeTo
                         from Alert a
                         where a.alertCode.code = :alertCode
                            and (a.activeTo is null or a.activeTo > current_date)
                            and a.deletedAt is null),
     prisoner_status as (select bp.id as id,
                                pp.prisonNumber as pn,
                                case
                                    when psea.id is null then 'CREATE'
                                    when psea.activeTo is null then 'ACTIVE'
                                    else 'UPDATE'
                                    end as stat
                         from BulkPlan bp
                         join bp.people pp
                                left join existing_alerts psea on psea.pn = pp.prisonNumber
                         where bp.id = :id)
    select
        case when ps.stat is null then 'EXPIRE' else ps.stat end as status, count(1) as count
    from existing_alerts ea
             full outer join prisoner_status ps on ea.pn = ps.pn
    group by status 
    """,
  )
  fun findPlanAffects(id: UUID, alertCode: String): List<PlanAffectCount>

  @Query(
    """
    select plan from BulkPlan plan
    join plan.people ppl
    where ppl.prisonNumber = :prisonNumber
    """,
  )
  fun findPlansWithPrisonNumber(prisonNumber: String): List<BulkPlan>
}

fun BulkPlanRepository.getPlan(id: UUID) = findByIdOrNull(id) ?: throw NotFoundException("Plan", id.toString())

interface PlanAffectCount {
  val status: Status
  val count: Int
}

enum class Status { ACTIVE, CREATE, UPDATE, EXPIRE }
