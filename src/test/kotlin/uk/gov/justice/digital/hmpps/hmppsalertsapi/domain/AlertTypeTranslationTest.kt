package uk.gov.justice.digital.hmpps.hmppsalertsapi.domain

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.hmppsalertsapi.entity.AlertCode
import uk.gov.justice.digital.hmpps.hmppsalertsapi.entity.AlertType
import java.time.LocalDateTime
import uk.gov.justice.digital.hmpps.hmppsalertsapi.model.AlertCode as AlertCodeModel
import uk.gov.justice.digital.hmpps.hmppsalertsapi.model.AlertType as AlertTypeModel

class AlertTypeTranslationTest {
  @Test
  fun `should convert alert code entity to model`() {
    val alertType = alertType()
    val alertCode = alertCode(alertType)

    val model = alertCode.toAlertCodeModel()

    assertThat(model).isEqualTo(
      AlertCodeModel(
        alertTypeCode = alertType.code,
        code = alertCode.code,
        description = alertCode.description,
        listSequence = alertCode.listSequence,
        isActive = alertCode.isActive(),
        createdAt = alertCode.createdAt,
        createdBy = alertCode.createdBy,
        modifiedAt = alertCode.modifiedAt,
        modifiedBy = alertCode.modifiedBy,
        deactivatedAt = null,
        deactivatedBy = null,
      ),
    )
  }

  @Test
  fun `should include alert code deactivated properties when converting alert code entity to model`() {
    val alertType = alertType()
    val alertCode = alertCode(alertType).apply {
      deactivatedAt = LocalDateTime.now().minusDays(1)
      deactivatedBy = "DEACTIVATED_BY"
    }

    val model = alertCode.toAlertCodeModel()

    assertThat(model.isActive).isFalse()
    assertThat(model.deactivatedAt).isEqualTo(alertCode.deactivatedAt)
    assertThat(model.deactivatedBy).isEqualTo(alertCode.deactivatedBy)
  }

  @Test
  fun `should order alert codes by list sequence then code when converting alert code entities to models`() {
    val alertType = alertType()
    val alertCodeExpectedPosition3 = alertCode(alertType, 1, "AA").apply {
      listSequence = 2
    }
    val alertCodeExpectedPosition2 = alertCode(alertType, 2, "B").apply {
      listSequence = 1
    }
    val alertCodeExpectedPosition1 = alertCode(alertType, 3, "A").apply {
      listSequence = 1
    }

    val models = alertType.alertCodes(false).toAlertCodeModels()

    assertThat(models.map { it.code }).containsExactly(alertCodeExpectedPosition1.code, alertCodeExpectedPosition2.code, alertCodeExpectedPosition3.code)
    assertThat(models).isSortedAccordingTo(compareBy({ it.listSequence }, { it.code }))
  }

  @Test
  fun `should convert alert type entity to model`() {
    val alertType = alertType()

    val model = alertType.toAlertTypeModel(false)

    assertThat(model).isEqualTo(
      AlertTypeModel(
        code = alertType.code,
        description = alertType.description,
        listSequence = alertType.listSequence,
        isActive = alertType.isActive(),
        createdAt = alertType.createdAt,
        createdBy = alertType.createdBy,
        modifiedAt = alertType.modifiedAt,
        modifiedBy = alertType.modifiedBy,
        deactivatedAt = null,
        deactivatedBy = null,
        alertCodes = emptyList(),
      ),
    )
  }

  @Test
  fun `should include alert type deactivated properties when converting alert type entity to model`() {
    val alertType = alertType().apply {
      deactivatedAt = LocalDateTime.now().minusDays(1)
      deactivatedBy = "DEACTIVATED_BY"
    }

    val model = alertType.toAlertTypeModel(true)

    assertThat(model.isActive).isFalse()
    assertThat(model.deactivatedAt).isEqualTo(alertType.deactivatedAt)
    assertThat(model.deactivatedBy).isEqualTo(alertType.deactivatedBy)
  }

  @Test
  fun `should convert alert type alert codes to models`() {
    val alertType = alertType()
    val alertCode = alertCode(alertType)

    val model = alertType.toAlertTypeModel(false)

    assertThat(model.alertCodes).isEqualTo(
      listOf(
        AlertCodeModel(
          alertTypeCode = alertType.code,
          code = alertCode.code,
          description = alertCode.description,
          listSequence = alertCode.listSequence,
          isActive = alertCode.isActive(),
          createdAt = alertCode.createdAt,
          createdBy = alertCode.createdBy,
          modifiedAt = alertCode.modifiedAt,
          modifiedBy = alertCode.modifiedBy,
          deactivatedAt = null,
          deactivatedBy = null,
        ),
      ),
    )
  }

  @Test
  fun `should filter out inactive alert codes when converting alert type entity to model`() {
    val alertType = alertType()
    val activeAlertCode = alertCode(alertType, 1, "ABC")
    val inactiveAlertCode = alertCode(alertType, 2, "ADE").apply {
      deactivatedAt = LocalDateTime.now().minusSeconds(1)
      deactivatedBy = "DEACTIVATED_BY"
    }

    val model = alertType.toAlertTypeModel(false)

    assertThat(model.alertCodes.map { it.code }).isEqualTo(listOf(activeAlertCode.code))
    assertThat(model.alertCodes.map { it.code }).doesNotContain(inactiveAlertCode.code)
  }

  @Test
  fun `should filter out inactive alert types when converting alert type entities to models`() {
    val activeAlertType = alertType(1, "A")
    val inactiveAlertType = alertType(2, "B").apply {
      deactivatedAt = LocalDateTime.now().minusSeconds(1)
      deactivatedBy = "DEACTIVATED_BY"
    }

    val models = listOf(activeAlertType, inactiveAlertType).toAlertTypeModels(false)

    assertThat(models.map { it.code }).isEqualTo(listOf(activeAlertType.code))
  }

  @Test
  fun `should order alert types by list sequence then code when converting alert type entities to models`() {
    val alertTypeExpectedPosition3 = alertType(1, "AA").apply {
      listSequence = 2
    }
    val alertTypeExpectedPosition2 = alertType(2, "B").apply {
      listSequence = 1
    }
    val alertTypeExpectedPosition1 = alertType(3, "A").apply {
      listSequence = 1
    }

    val models = listOf(alertTypeExpectedPosition3, alertTypeExpectedPosition2, alertTypeExpectedPosition1).toAlertTypeModels(false)

    assertThat(models.map { it.code }).containsExactly(alertTypeExpectedPosition1.code, alertTypeExpectedPosition2.code, alertTypeExpectedPosition3.code)
    assertThat(models).isSortedAccordingTo(compareBy({ it.listSequence }, { it.code }))
  }

  private fun alertType(alertTypeId: Long = 1, code: String = "A") =
    AlertType(
      alertTypeId,
      code,
      "Alert type $code",
      1,
      LocalDateTime.now().minusDays(3),
      "CREATED_BY",
    ).apply {
      modifiedAt = LocalDateTime.now().minusDays(2)
      modifiedBy = "MODIFIED_BY"
    }

  private fun alertCode(alertType: AlertType, alertCodeId: Long = 1, code: String = "ABC") =
    AlertCode(
      alertCodeId,
      alertType,
      code,
      "Alert code $code",
      1,
      LocalDateTime.now().minusDays(3),
      "CREATED_BY",
    ).apply {
      modifiedAt = LocalDateTime.now().minusDays(2)
      modifiedBy = "MODIFIED_BY"
      alertType.addAlertCode(this)
    }
}
