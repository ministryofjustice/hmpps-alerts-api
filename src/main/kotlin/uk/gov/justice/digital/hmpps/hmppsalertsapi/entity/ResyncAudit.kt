package uk.gov.justice.digital.hmpps.hmppsalertsapi.entity

import com.fasterxml.jackson.databind.JsonNode
import io.hypersistence.utils.hibernate.type.array.ListArrayType
import io.hypersistence.utils.hibernate.type.json.JsonType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import org.hibernate.annotations.Type
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import uk.gov.justice.digital.hmpps.hmppsalertsapi.enumeration.Source
import uk.gov.justice.digital.hmpps.hmppsalertsapi.model.request.ResyncAlert
import java.time.LocalDateTime
import java.util.UUID

@Entity
class ResyncAudit(

  @Type(JsonType::class)
  @Column(columnDefinition = "jsonb")
  val request: JsonNode,

  val requestedAt: LocalDateTime,
  val requestedBy: String,
  val requestedByDisplayName: String,
  @Enumerated(EnumType.STRING)
  val source: Source,
  val completedAt: LocalDateTime,

  @Type(ListArrayType::class)
  @Column(columnDefinition = "uuid[]")
  val alertsDeleted: List<UUID>,

  @Type(ListArrayType::class)
  @Column(columnDefinition = "uuid[]")
  val alertsCreated: List<UUID>,

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  val id: Long = 0,
)

interface ResyncAuditRepository : JpaRepository<ResyncAudit, Long> {
  @Query(value = "select * from resync_audit where request ->> 'prisonNumber' = :prisonNumber", nativeQuery = true)
  fun findResyncAuditsByPrisonNumber(prisonNumber: String): List<ResyncAudit>
}

data class ResyncAuditRequest(val prisonNumber: String, val alerts: List<ResyncAlert>)
