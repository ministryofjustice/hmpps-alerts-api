package uk.gov.justice.digital.hmpps.hmppsalertsapi.entity

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatCode
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.jdbc.Sql
import org.springframework.test.context.transaction.TestTransaction
import uk.gov.justice.digital.hmpps.hmppsalertsapi.repository.AlertRepository
import java.time.LocalDateTime
import java.util.UUID

@Sql("classpath:jpa/repository/reset.sql", "classpath:jpa/repository/alert-repository.sql")
class AlertRepositoryTest : RepositoryTest() {
  @Autowired
  lateinit var repository: AlertRepository

  @Test
  fun `finds active alerts for prison number`() {
    commitAndStartNew()
    val alerts = repository.findByPrisonNumberIn(listOf("A1234AA"), includeInactive = false)

    assertThat(alerts.map { it.id }).containsExactlyInAnyOrder(ALERT_1, ALERT_2, ALERT_6)
  }

  @Test
  fun `includes inactive alerts and filters by code`() {
    commitAndStartNew()
    val alerts = repository.findByPrisonNumberIn(listOf("A1234AA"), includeInactive = true, setOf("AS"))

    assertThat(alerts.map { it.id }).containsExactlyInAnyOrder(ALERT_1, ALERT_2, ALERT_3, ALERT_4)
  }

  @Test
  fun `finds active alerts by code`() {
    commitAndStartNew()
    val alerts = repository.findAllActiveByCode("AS")

    assertThat(alerts.map { it.id }).containsExactlyInAnyOrder(ALERT_1, ALERT_2, ALERT_5)
  }

  @Test
  fun `finds soft-deleted alert by ID`() {
    commitAndStartNew()
    val alert = repository.findByAlertUuidIncludingSoftDelete(ALERT_7)

    assertThat(alert?.id).isEqualTo(ALERT_7)
  }

  @Test
  fun `finds prison numbers with created or inactive events in time window`() {
    commitAndStartNew()
    val prisonNumbers = repository.prisonNumbersCreatedOrInactiveBetween(
      LocalDateTime.parse("2024-01-10T10:00:00"),
      LocalDateTime.parse("2024-01-10T11:00:00"),
    )

    assertThat(prisonNumbers).containsExactlyInAnyOrder("A1234AA", "B2345BB")
  }

  @Test
  fun `locks active alert creation`() {
    commitAndStartNew()
    assertThatCode { repository.lockActiveAlertCreation("A1234AA", "AS") }.doesNotThrowAnyException()
  }

  private fun commitAndStartNew() {
    TestTransaction.flagForCommit()
    TestTransaction.end()
    TestTransaction.start()
  }

  companion object {
    private val ALERT_1 = UUID.fromString("00000000-0000-0000-0000-000000000101")
    private val ALERT_2 = UUID.fromString("00000000-0000-0000-0000-000000000102")
    private val ALERT_3 = UUID.fromString("00000000-0000-0000-0000-000000000103")
    private val ALERT_4 = UUID.fromString("00000000-0000-0000-0000-000000000104")
    private val ALERT_5 = UUID.fromString("00000000-0000-0000-0000-000000000105")
    private val ALERT_6 = UUID.fromString("00000000-0000-0000-0000-000000000106")
    private val ALERT_7 = UUID.fromString("00000000-0000-0000-0000-000000000107")
  }
}
