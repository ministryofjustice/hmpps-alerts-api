package uk.gov.justice.digital.hmpps.hmppsalertsapi.entity

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
  val alertCodeId: Long,
  @Id
  val username: String,
)

class AlertCodePrivilegedUserId : Serializable {
  private val alertCodeId: Long? = null
  private val username: String? = null

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