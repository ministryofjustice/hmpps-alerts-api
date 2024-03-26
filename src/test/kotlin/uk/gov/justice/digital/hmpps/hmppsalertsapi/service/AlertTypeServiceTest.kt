package uk.gov.justice.digital.hmpps.hmppsalertsapi.service

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.hmppsalertsapi.config.AlertRequestContext
import uk.gov.justice.digital.hmpps.hmppsalertsapi.domain.toAlertTypeModel
import uk.gov.justice.digital.hmpps.hmppsalertsapi.domain.toAlertTypeModels
import uk.gov.justice.digital.hmpps.hmppsalertsapi.entity.AlertCode
import uk.gov.justice.digital.hmpps.hmppsalertsapi.entity.AlertType
import uk.gov.justice.digital.hmpps.hmppsalertsapi.model.request.CreateAlertTypeRequest
import uk.gov.justice.digital.hmpps.hmppsalertsapi.repository.AlertTypeRepository
import java.time.LocalDateTime

class AlertTypeServiceTest {

  private val entityCaptor = argumentCaptor<AlertType>()
  private val alertTypeRepository: AlertTypeRepository = mock()

  private val service = AlertTypeService(alertTypeRepository)

  @Test
  fun `get all active alert types`() {
    val activeAlertType = alertType(1, "A").apply {
      alertCode(this, 1, "ABC")
      alertCode(this, 2, "ADE").apply {
        deactivatedAt = LocalDateTime.now().minusSeconds(1)
        deactivatedBy = "DEACTIVATED_BY"
      }
    }
    val inactiveAlertType = alertType(2, "B").apply {
      deactivatedAt = LocalDateTime.now().minusSeconds(1)
      deactivatedBy = "DEACTIVATED_BY"
    }
    val includeInactive = false

    whenever(alertTypeRepository.findAll()).thenReturn(listOf(activeAlertType, inactiveAlertType))

    val alertTypes = service.getAlertTypes(includeInactive)

    assertThat(alertTypes).isEqualTo(listOf(activeAlertType.toAlertTypeModel(includeInactive)))
  }

  @Test
  fun `get all alert types including inactive`() {
    val activeAlertType = alertType(1, "A").apply {
      alertCode(this, 1, "ABC")
      alertCode(this, 2, "ADE").apply {
        deactivatedAt = LocalDateTime.now().minusSeconds(1)
        deactivatedBy = "DEACTIVATED_BY"
      }
    }
    val inactiveAlertType = alertType(2, "B").apply {
      deactivatedAt = LocalDateTime.now().minusSeconds(1)
      deactivatedBy = "DEACTIVATED_BY"
    }
    val includeInactive = true

    whenever(alertTypeRepository.findAll()).thenReturn(listOf(activeAlertType, inactiveAlertType))

    val alertTypes = service.getAlertTypes(includeInactive)

    assertThat(alertTypes).isEqualTo(listOf(activeAlertType, inactiveAlertType).toAlertTypeModels(includeInactive))
  }

  @Test
  fun `save an alert type`() {
    whenever(alertTypeRepository.saveAndFlush(any())).thenReturn(alertType())
    service.createAlertType(
      CreateAlertTypeRequest(code = "A", description = "Alert type A"),
      AlertRequestContext(username = "USER", userDisplayName = "USER"),
    )
    verify(alertTypeRepository).saveAndFlush(entityCaptor.capture())
    val value = entityCaptor.firstValue
    assertThat(value).isNotNull
    assertThat(value.createdBy).isEqualTo("USER")
    assertThat(value.code).isEqualTo("A")
    assertThat(value.description).isEqualTo("Alert type A")
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
