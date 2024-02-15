package uk.gov.justice.digital.hmpps.hmppsalertsapi.entity

import io.hypersistence.utils.hibernate.type.json.internal.JacksonUtil
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime

class NomisAlertTest {
  @Test
  fun `remove stores audit information`() {
    val removedAt = LocalDateTime.now()
    val alert = Alert(
      alertType = "A",
      alertCode = "B",
      authorisedBy = "",
      offenderId = "A1122DZ",
      validFrom = LocalDate.now(),
    )
    val entity = NomisAlert(
      nomisAlertId = 0,
      offenderBookId = 3,
      alertSeq = 1,
      alert = alert,
      nomisAlertData = JacksonUtil.toJsonNode("{}"),
    )

    entity.remove(removedAt)

    assertThat(entity.removedAt).isEqualTo(removedAt)
  }
}
