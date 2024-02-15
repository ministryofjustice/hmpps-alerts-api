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
data class NomisAlert(
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  val nomisAlertId: Long = 0,

  val offenderBookId: Long,

  val alertSeq: Int,

  val alertUuid: UUID,

  @Type(JsonType::class)
  @Column(columnDefinition = "jsonb")
  var nomisAlertData: JsonNode,

  var upsertedAt: LocalDateTime = LocalDateTime.now(),
) {
  var removedAt: LocalDateTime? = null

  fun remove(removedAt: LocalDateTime = LocalDateTime.now()) {
    this.removedAt = removedAt
  }
}
