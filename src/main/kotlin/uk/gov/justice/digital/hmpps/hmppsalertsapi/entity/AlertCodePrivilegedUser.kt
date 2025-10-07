package uk.gov.justice.digital.hmpps.hmppsalertsapi.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.IdClass
import jakarta.persistence.Table
import java.io.Serializable

@Entity
@IdClass(AlertCodePrivilegedUserId::class)
@Table
class AlertCodePrivilegedUser(
  @Id
  @Column(name = "alert_code_id", nullable = false)
  val alertCodeId: Long,
  @Id
  @Column(name = "username", nullable = false)
  val username: String,
)

class AlertCodePrivilegedUserId(
  val alertCodeId: Long? = null,
  val username: String? = null,
) : Serializable {

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (javaClass != other?.javaClass) return false
    other as AlertCodePrivilegedUserId
    if (alertCodeId != other.alertCodeId) return false
    if (username != other.username) return false
    return true
  }

  override fun hashCode(): Int {
    var result = alertCodeId.hashCode()
    result = 31 * result + username.hashCode()
    return result
  }
}
