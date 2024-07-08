package uk.gov.justice.digital.hmpps.hmppsalertsapi.entity

import com.fasterxml.jackson.databind.JsonNode
import io.hypersistence.utils.hibernate.type.json.JsonType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.hibernate.annotations.Type
import java.time.LocalDateTime
import java.util.UUID

@Entity
@Table
class BulkAlert(
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  val bulkAlertId: Long = 0,

  val bulkAlertUuid: UUID,

  @Type(JsonType::class)
  @Column(columnDefinition = "jsonb")
  val request: JsonNode,

  val requestedAt: LocalDateTime,
  val requestedBy: String,
  val requestedByDisplayName: String,
  val completedAt: LocalDateTime,
  val successful: Boolean,

  @Type(JsonType::class)
  @Column(columnDefinition = "jsonb")
  val messages: JsonNode,

  @Type(JsonType::class)
  @Column(columnDefinition = "jsonb")
  val existingActiveAlerts: JsonNode,

  @Type(JsonType::class)
  @Column(columnDefinition = "jsonb")
  val alertsCreated: JsonNode,

  @Type(JsonType::class)
  @Column(columnDefinition = "jsonb")
  val alertsUpdated: JsonNode,

  @Type(JsonType::class)
  @Column(columnDefinition = "jsonb")
  val alertsExpired: JsonNode,
)
