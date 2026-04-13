package uk.gov.justice.digital.hmpps.hmppsalertsapi.entity

import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import jakarta.persistence.Version
import org.springframework.data.jpa.repository.JpaRepository
import uk.gov.justice.digital.hmpps.hmppsalertsapi.client.prisonersearch.dto.PrisonerDetails

@Entity
@Table
class PersonSummary(
  @Id
  val prisonNumber: String,
  firstName: String,
  lastName: String,
  status: String,
  restrictedPatient: Boolean,
  prisonCode: String?,
  cellLocation: String?,
  supportingPrisonCode: String?,
) {
  final var firstName: String = firstName
    private set
  final var lastName: String = lastName
    private set
  final var status: String = status
    private set
  final var restrictedPatient: Boolean = restrictedPatient
    private set
  final var prisonCode: String? = prisonCode
    private set
  final var cellLocation: String? = cellLocation
    private set
  final var supportingPrisonCode: String? = supportingPrisonCode
    private set

  @Version
  val version: Int? = null

  fun update(
    firstName: String,
    lastName: String,
    status: String,
    restrictedPatient: Boolean,
    prisonCode: String?,
    cellLocation: String?,
    supportingPrisonCode: String?,
  ) {
    this.firstName = firstName
    this.lastName = lastName
    this.status = status
    this.restrictedPatient = restrictedPatient
    this.prisonCode = prisonCode
    this.cellLocation = cellLocation
    this.supportingPrisonCode = supportingPrisonCode
  }

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (javaClass != other?.javaClass) return false
    other as PersonSummary
    return prisonNumber == other.prisonNumber
  }

  override fun hashCode(): Int = prisonNumber.hashCode()
}

fun PrisonerDetails.toPersonSummary() = PersonSummary(
  prisonerNumber,
  firstName,
  lastName,
  status,
  restrictedPatient,
  prisonId,
  cellLocation,
  supportingPrisonId,
)

interface PersonSummaryRepository : JpaRepository<PersonSummary, String>
