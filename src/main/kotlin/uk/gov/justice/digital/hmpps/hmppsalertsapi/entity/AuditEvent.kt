package uk.gov.justice.digital.hmpps.hmppsalertsapi.entity

import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import uk.gov.justice.digital.hmpps.hmppsalertsapi.enumeration.AuditEventAction
import uk.gov.justice.digital.hmpps.hmppsalertsapi.enumeration.Source
import java.time.LocalDateTime

@Entity
@Table
class AuditEvent(
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Id
  val auditEventId: Long = 0,

  @ManyToOne
  @JoinColumn(name = "alert_id", nullable = false)
  val alert: Alert,

  @Enumerated(EnumType.STRING)
  val action: AuditEventAction,

  val description: String,
  val actionedAt: LocalDateTime,
  val actionedBy: String,
  val actionedByDisplayName: String,

  @Enumerated(EnumType.STRING)
  val source: Source,

  val activeCaseLoadId: String?,

  val descriptionUpdated: Boolean? = null,
  val authorisedByUpdated: Boolean? = null,
  val activeFromUpdated: Boolean? = null,
  val activeToUpdated: Boolean? = null,
  val commentAppended: Boolean? = null,
)
