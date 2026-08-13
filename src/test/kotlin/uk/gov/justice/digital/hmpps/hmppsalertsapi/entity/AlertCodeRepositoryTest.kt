package uk.gov.justice.digital.hmpps.hmppsalertsapi.entity

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.jdbc.Sql
import uk.gov.justice.digital.hmpps.hmppsalertsapi.repository.AlertCodeRepository

@Sql("classpath:jpa/repository/reset.sql", "classpath:jpa/repository/alert-code-repository.sql")
class AlertCodeRepositoryTest : RepositoryTest() {
  @Autowired
  lateinit var repository: AlertCodeRepository

  @Test
  fun `returns alert code ID for code`() {
    assertThat(repository.getAlertCodeIdForCode("TEST_CODE")).isEqualTo(900001L)
  }

  @Test
  fun `returns null when code does not exist`() {
    assertThat(repository.getAlertCodeIdForCode("NOT_FOUND")).isNull()
  }
}
