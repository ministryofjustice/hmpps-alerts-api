package uk.gov.justice.digital.hmpps.hmppsalertsapi.service

import org.assertj.core.api.Assertions.assertThat
import org.awaitility.kotlin.await
import org.awaitility.kotlin.matches
import org.awaitility.kotlin.untilCallTo
import org.awaitility.kotlin.withPollDelay
import org.junit.jupiter.api.Test
import org.springframework.data.repository.findByIdOrNull
import uk.gov.justice.digital.hmpps.hmppsalertsapi.entity.event.HmppsAdditionalInformation
import uk.gov.justice.digital.hmpps.hmppsalertsapi.entity.event.HmppsDomainEvent
import uk.gov.justice.digital.hmpps.hmppsalertsapi.entity.event.PersonReference.Companion.withPrisonNumber
import uk.gov.justice.digital.hmpps.hmppsalertsapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.hmppsalertsapi.service.event.DomainEventsListener.Companion.PRISONER_MERGED
import uk.gov.justice.digital.hmpps.hmppsalertsapi.utils.IdGenerator.prisonNumber
import java.time.Duration.ofSeconds
import java.time.ZonedDateTime

class PersonMergedIntTest : IntegrationTestBase() {

  @Test
  fun `message ignored if prison number not of interest`() {
    val prisonNumber = prisonNumber()
    assertThat(personSummaryRepository.findByIdOrNull(prisonNumber)).isNull()
    sendDomainEvent(personMergedEvent(prisonNumber, prisonNumber))

    await withPollDelay ofSeconds(1) untilCallTo { hmppsDomainEventsQueue.countAllMessagesOnQueue() } matches { it == 0 }
    assertThat(personSummaryRepository.findByIdOrNull(prisonNumber)).isNull()
  }

  @Test
  fun `merge event causes person summary to be deleted`() {
    val (plan, existingPeople) = transactionTemplate.execute {
      val existingPeople = listOf(givenPersonSummary(personSummary()), givenPersonSummary(personSummary()))
      val plan = givenBulkPlan(plan(givenAlertCode()).apply { people.addAll(existingPeople) })
      plan to existingPeople
    }!!

    val prisonNumber = existingPeople.first().prisonNumber
    val removedNomsNumber = existingPeople.last().prisonNumber

    val person = personSummaryRepository.findByIdOrNull(removedNomsNumber)
    assertThat(person).isNotNull

    sendDomainEvent(personMergedEvent(prisonNumber, removedNomsNumber))

    await withPollDelay ofSeconds(1) untilCallTo { hmppsDomainEventsQueue.countAllMessagesOnQueue() } matches { it == 0 }
    val saved = personSummaryRepository.findByIdOrNull(removedNomsNumber)
    assertThat(saved).isNull()

    transactionTemplate.execute {
      val updatedPlan = requireNotNull(bulkPlanRepository.findByIdOrNull(plan.id))
      assertThat(updatedPlan.people.map { it.prisonNumber }.single()).isEqualTo(prisonNumber)
    }
  }

  private fun personMergedEvent(
    prisonNumber: String,
    removedPrisonNumber: String,
    occurredAt: ZonedDateTime = ZonedDateTime.now(),
    eventType: String = PRISONER_MERGED,
    detailUrl: String? = null,
    description: String = "A prisoner was merged",
  ) = HmppsDomainEvent(
    eventType,
    1,
    detailUrl,
    occurredAt,
    description,
    HmppsAdditionalInformation(mutableMapOf("nomsNumber" to prisonNumber, "removedNomsNumber" to removedPrisonNumber)),
    withPrisonNumber(prisonNumber),
  )
}
