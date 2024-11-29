package uk.gov.justice.digital.hmpps.hmppsalertsapi.service

import org.assertj.core.api.Assertions.assertThat
import org.awaitility.kotlin.await
import org.awaitility.kotlin.matches
import org.awaitility.kotlin.untilCallTo
import org.awaitility.kotlin.withPollDelay
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.repository.findByIdOrNull
import uk.gov.justice.digital.hmpps.hmppsalertsapi.client.prisonersearch.dto.PrisonerDetails
import uk.gov.justice.digital.hmpps.hmppsalertsapi.entity.PersonSummary
import uk.gov.justice.digital.hmpps.hmppsalertsapi.entity.PersonSummaryRepository
import uk.gov.justice.digital.hmpps.hmppsalertsapi.entity.event.HmppsAdditionalInformation
import uk.gov.justice.digital.hmpps.hmppsalertsapi.entity.event.HmppsDomainEvent
import uk.gov.justice.digital.hmpps.hmppsalertsapi.entity.event.PersonReference.Companion.withPrisonNumber
import uk.gov.justice.digital.hmpps.hmppsalertsapi.entity.toPersonSummary
import uk.gov.justice.digital.hmpps.hmppsalertsapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.hmppsalertsapi.integration.wiremock.PrisonerSearchExtension.Companion.prisonerSearch
import uk.gov.justice.digital.hmpps.hmppsalertsapi.service.event.DomainEventsListener.Companion.PRISONER_UPDATED
import uk.gov.justice.digital.hmpps.hmppsalertsapi.utils.IdGenerator.prisonNumber
import uk.gov.justice.digital.hmpps.hmppsalertsapi.utils.prisoner
import java.time.Duration
import java.time.ZonedDateTime

class PersonSummaryUpdateIntTest : IntegrationTestBase() {
  @Autowired
  lateinit var personSummaryRepository: PersonSummaryRepository

  @Test
  fun `message ignored if category not of interest`() {
    val personSummary = personSummaryRepository.save(prisoner().toPersonSummary())
    val updatedPrisoner = prisoner(
      prisonerNumber = personSummary.prisonNumber,
      firstName = "Update-First",
      lastName = "Update-Last",
      status = "INACTIVE OUT",
      prisonId = null,
      cellLocation = null,
    )
    prisonerSearch.stubGetPrisonerDetails(updatedPrisoner)

    sendDomainEvent(
      personChangedEvent(
        personSummary.prisonNumber,
        setOf(
          "IDENTIFIERS",
          "ALERTS",
          "SENTENCE",
          "RESTRICTED_PATIENT",
          "INCENTIVE_LEVEL",
          "PHYSICAL_DETAILS",
          "CONTACT_DETAILS",
        ),
      ),
    )

    await withPollDelay Duration.ofSeconds(1) untilCallTo { hmppsDomainEventsQueue.countAllMessagesOnQueue() } matches { it == 0 }
    val saved = requireNotNull(personSummaryRepository.findByIdOrNull(personSummary.prisonNumber))
    saved.verifyAgainst(personSummary)
  }

  @Test
  fun `message ignored if prison number not of interest`() {
    val prisonNumber = prisonNumber()
    assertThat(personSummaryRepository.findByIdOrNull(prisonNumber)).isNull()
    val updatedPrisoner = prisoner(
      prisonerNumber = prisonNumber,
      firstName = "Update-First",
      lastName = "Update-Last",
      status = "INACTIVE OUT",
      prisonId = null,
      cellLocation = null,
    )

    sendDomainEvent(personChangedEvent(updatedPrisoner.prisonerNumber, setOf("PERSONAL_DETAILS")))

    await withPollDelay Duration.ofSeconds(1) untilCallTo { hmppsDomainEventsQueue.countAllMessagesOnQueue() } matches { it == 0 }
    assertThat(personSummaryRepository.findByIdOrNull(prisonNumber)).isNull()
  }

  @ParameterizedTest
  @ValueSource(strings = ["PERSONAL_DETAILS", "STATUS", "LOCATION"])
  fun `message with matching category causes person summary details to be updated`(changeCategory: String) {
    val personSummary = personSummaryRepository.save(prisoner().toPersonSummary())
    val updatedPrisoner = prisoner(
      prisonerNumber = personSummary.prisonNumber,
      firstName = "Update-First",
      lastName = "Update-Last",
      status = "INACTIVE OUT",
      prisonId = null,
      cellLocation = null,
    )
    prisonerSearch.stubGetPrisonerDetails(updatedPrisoner)

    sendDomainEvent(personChangedEvent(personSummary.prisonNumber, setOf(changeCategory)))

    await withPollDelay Duration.ofSeconds(1) untilCallTo { hmppsDomainEventsQueue.countAllMessagesOnQueue() } matches { it == 0 }
    val saved = requireNotNull(personSummaryRepository.findByIdOrNull(personSummary.prisonNumber))
    saved.verifyAgainst(updatedPrisoner)
  }

  private fun personChangedEvent(
    prisonNumber: String,
    changeCategories: Set<String>,
    occurredAt: ZonedDateTime = ZonedDateTime.now(),
    eventType: String = PRISONER_UPDATED,
    detailUrl: String? = null,
    description: String = "A prisoner was updated",
  ) = HmppsDomainEvent(
    eventType,
    1,
    detailUrl,
    occurredAt,
    description,
    HmppsAdditionalInformation(mutableMapOf("nomsNumber" to prisonNumber, "categoriesChanged" to changeCategories)),
    withPrisonNumber(prisonNumber),
  )

  private fun PersonSummary.verifyAgainst(prisoner: PrisonerDetails) {
    assertThat(prisonNumber).isEqualTo(prisoner.prisonerNumber)
    assertThat(firstName).isEqualTo(prisoner.firstName)
    assertThat(lastName).isEqualTo(prisoner.lastName)
    assertThat(status).isEqualTo(prisoner.status)
    assertThat(restrictedPatient).isEqualTo(prisoner.restrictedPatient)
    assertThat(prisonCode).isEqualTo(prisoner.prisonId)
    assertThat(cellLocation).isEqualTo(prisoner.cellLocation)
    assertThat(supportingPrisonCode).isEqualTo(prisoner.supportingPrisonId)
  }

  private fun PersonSummary.verifyAgainst(other: PersonSummary) {
    assertThat(prisonNumber).isEqualTo(other.prisonNumber)
    assertThat(firstName).isEqualTo(other.firstName)
    assertThat(lastName).isEqualTo(other.lastName)
    assertThat(status).isEqualTo(other.status)
    assertThat(restrictedPatient).isEqualTo(other.restrictedPatient)
    assertThat(prisonCode).isEqualTo(other.prisonCode)
    assertThat(cellLocation).isEqualTo(other.cellLocation)
    assertThat(supportingPrisonCode).isEqualTo(other.supportingPrisonCode)
  }
}
