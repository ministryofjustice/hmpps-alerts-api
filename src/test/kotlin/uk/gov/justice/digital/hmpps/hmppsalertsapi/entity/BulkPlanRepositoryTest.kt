package uk.gov.justice.digital.hmpps.hmppsalertsapi.entity

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.jdbc.Sql
import java.util.UUID

@Sql("classpath:jpa/repository/reset.sql", "classpath:jpa/repository/bulk-plan-repository.sql")
class BulkPlanRepositoryTest : RepositoryTest() {
  @Autowired
  lateinit var repository: BulkPlanRepository

  @Test
  fun `counts how plan affects existing alerts`() {
    val affects = repository.findPlanAffects(PLAN_1, "AS").associate { it.status to it.count }

    assertThat(affects).containsExactlyInAnyOrderEntriesOf(
      mapOf(
        Status.ACTIVE to 1,
        Status.CREATE to 1,
        Status.UPDATE to 1,
        Status.EXPIRE to 1,
      ),
    )
  }

  @Test
  fun `finds plans containing prison number`() {
    val plans = repository.findPlansWithPrisonNumber("A1111AA")

    assertThat(plans.map { it.id }).containsExactlyInAnyOrder(PLAN_1, PLAN_2)
  }

  companion object {
    private val PLAN_1 = UUID.fromString("00000000-0000-0000-0000-000000000201")
    private val PLAN_2 = UUID.fromString("00000000-0000-0000-0000-000000000202")
  }
}
