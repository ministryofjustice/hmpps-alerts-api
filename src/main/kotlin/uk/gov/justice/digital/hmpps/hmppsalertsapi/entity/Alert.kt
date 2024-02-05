package uk.gov.justice.digital.hmpps.hmppsalertsapi.entity

import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.util.UUID

@Entity
@Table
data class Alert(
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  val alertId: Long = 0,

  val alertUuid: UUID = UUID.randomUUID(),
)
