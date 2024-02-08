package uk.gov.justice.digital.hmpps.hmppsalertsapi.entity

import io.hypersistence.utils.hibernate.type.json.internal.JacksonUtil
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import java.util.UUID

class NomisAlertTest {
  @Test
  fun `remove stores audit information`() {
    val removedAt = LocalDateTime.now()

    val entity = NomisAlert(
      nomisAlertId = 0,
      offenderBookId = 3,
      alertSeq = 1,
      alertUuid = UUID.randomUUID(),
      nomisAlertData = JacksonUtil.toJsonNode("{}"),
    )

    entity.remove(removedAt)

    assertThat(entity.removedAt).isEqualTo(removedAt)
  }
}
