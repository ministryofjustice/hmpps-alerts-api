package uk.gov.justice.digital.hmpps.hmppsalertsapi.entity

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.hmppsalertsapi.entity.event.AlertCreatedEvent
import uk.gov.justice.digital.hmpps.hmppsalertsapi.entity.event.AlertDeletedEvent
import uk.gov.justice.digital.hmpps.hmppsalertsapi.entity.event.AlertUpdatedEvent
import uk.gov.justice.digital.hmpps.hmppsalertsapi.enumeration.AuditEventAction.CREATED
import uk.gov.justice.digital.hmpps.hmppsalertsapi.enumeration.AuditEventAction.DELETED
import uk.gov.justice.digital.hmpps.hmppsalertsapi.enumeration.AuditEventAction.UPDATED
import uk.gov.justice.digital.hmpps.hmppsalertsapi.enumeration.Source
import uk.gov.justice.digital.hmpps.hmppsalertsapi.enumeration.Source.DPS
import uk.gov.justice.digital.hmpps.hmppsalertsapi.enumeration.Source.NOMIS
import uk.gov.justice.digital.hmpps.hmppsalertsapi.integration.wiremock.PRISON_CODE_LEEDS
import uk.gov.justice.digital.hmpps.hmppsalertsapi.integration.wiremock.PRISON_CODE_MOORLANDS
import uk.gov.justice.digital.hmpps.hmppsalertsapi.integration.wiremock.PRISON_NUMBER
import uk.gov.justice.digital.hmpps.hmppsalertsapi.utils.EntityGenerator.AC_VICTIM
import java.time.LocalDate
import java.time.LocalDateTime

class AlertTest {
  @Test
  fun `is active when active from is today`() {
    val alert = alertEntity().apply { activeFrom = LocalDate.now() }
    assertThat(alert.isActive()).isTrue
  }

  @Test
  fun `is active when active from is tomorrow`() {
    val alert = alertEntity().apply { activeFrom = LocalDate.now().plusDays(1) }
    assertThat(alert.isActive()).isTrue()
  }

  @Test
  fun `is active when active to is null`() {
    val alert = alertEntity().apply { activeTo = null }
    assertThat(alert.isActive()).isTrue
  }

  @Test
  fun `is active when active to is tomorrow`() {
    val alert = alertEntity().apply { activeTo = LocalDate.now().plusDays(1) }
    assertThat(alert.isActive()).isTrue
  }

  @Test
  fun `is not active when active to is today`() {
    val alert = alertEntity().apply { activeTo = LocalDate.now() }
    assertThat(alert.isActive()).isFalse
  }

  @Test
  fun `add audit event`() {
    val actionedAt = LocalDateTime.now().minusDays(2)
    val entity = alertEntity().apply {
      auditEvent(
        action = UPDATED,
        description = "Alert updated",
        actionedAt = actionedAt,
        actionedBy = "UPDATED_BY",
        actionedByDisplayName = "UPDATED_BY_DISPLAY_NAME",
        source = DPS,
        activeCaseLoadId = PRISON_CODE_MOORLANDS,
      )
    }

    assertThat(entity.auditEvents().single { it.action == UPDATED }).usingRecursiveComparison().isEqualTo(
      AuditEvent(
        alert = entity,
        action = UPDATED,
        description = "Alert updated",
        actionedAt = actionedAt,
        actionedBy = "UPDATED_BY",
        actionedByDisplayName = "UPDATED_BY_DISPLAY_NAME",
        source = DPS,
        activeCaseLoadId = PRISON_CODE_MOORLANDS,
      ),
    )
  }

  @Test
  fun `audit events are ordered newest to oldest`() {
    val entity = alertEntity().apply {
      auditEvent(
        action = UPDATED,
        description = "Alert updated",
        actionedAt = LocalDateTime.now().minusDays(2),
        actionedBy = "UPDATED_BY_2",
        actionedByDisplayName = "UPDATED_BY_2_DISPLAY_NAME",
        source = DPS,
        activeCaseLoadId = PRISON_CODE_LEEDS,
      )
      auditEvent(
        action = DELETED,
        description = "Alert deleted",
        actionedAt = LocalDateTime.now(),
        actionedBy = "DELETED_BY",
        actionedByDisplayName = "DELETED_BY_DISPLAY_NAME",
        source = NOMIS,
        activeCaseLoadId = PRISON_CODE_MOORLANDS,
      )
      auditEvent(
        action = UPDATED,
        description = "Alert updated",
        actionedAt = LocalDateTime.now().minusDays(1),
        actionedBy = "UPDATED_BY_3",
        actionedByDisplayName = "UPDATED_BY_3_DISPLAY_NAME",
        source = DPS,
        activeCaseLoadId = PRISON_CODE_MOORLANDS,
      )
      auditEvent(
        action = UPDATED,
        description = "Alert updated",
        actionedAt = LocalDateTime.now().minusDays(3),
        actionedBy = "UPDATED_BY_1",
        actionedByDisplayName = "UPDATED_BY_1_DISPLAY_NAME",
        source = NOMIS,
        activeCaseLoadId = PRISON_CODE_LEEDS,
      )
    }

    assertThat(entity.auditEvents()).isSortedAccordingTo(compareByDescending { it.actionedAt })
  }

  @Test
  fun `created audit event returns single created audit event`() {
    val createdAt: LocalDateTime = LocalDateTime.now().minusDays(3)
    val entity = alertEntity(createdAt).apply {
      auditEvent(
        action = UPDATED,
        description = "Alert updated",
        actionedAt = LocalDateTime.now().minusDays(2),
        actionedBy = "UPDATED_BY",
        actionedByDisplayName = "UPDATED_BY_DISPLAY_NAME",
        source = NOMIS,
        activeCaseLoadId = PRISON_CODE_LEEDS,
      )
      auditEvent(
        action = DELETED,
        description = "Alert deleted",
        actionedAt = LocalDateTime.now(),
        actionedBy = "DELETED_BY",
        actionedByDisplayName = "DELETED_BY_DISPLAY_NAME",
        source = DPS,
        activeCaseLoadId = PRISON_CODE_MOORLANDS,
      )
    }

    assertThat(entity.createdAuditEvent()).usingRecursiveComparison().isEqualTo(
      AuditEvent(
        alert = entity,
        action = CREATED,
        description = "Alert created",
        actionedAt = createdAt,
        actionedBy = "CREATED_BY",
        actionedByDisplayName = "CREATED_BY_DISPLAY_NAME",
        source = DPS,
        activeCaseLoadId = PRISON_CODE_MOORLANDS,
      ),
    )
  }

  @Test
  fun `last modified audit event is newest updated audit event`() {
    val lastModifiedAt = LocalDateTime.now().minusDays(1)
    val entity = alertEntity().apply {
      auditEvent(
        action = UPDATED,
        description = "Alert updated",
        actionedAt = LocalDateTime.now().minusDays(2),
        actionedBy = "UPDATED_BY_2",
        actionedByDisplayName = "UPDATED_BY_2_DISPLAY_NAME",
        source = DPS,
        activeCaseLoadId = PRISON_CODE_MOORLANDS,
      )
      auditEvent(
        action = DELETED,
        description = "Alert deleted",
        actionedAt = LocalDateTime.now(),
        actionedBy = "DELETED_BY",
        actionedByDisplayName = "DELETED_BY_DISPLAY_NAME",
        source = NOMIS,
        activeCaseLoadId = PRISON_CODE_MOORLANDS,
      )
      auditEvent(
        action = UPDATED,
        description = "Alert updated",
        actionedAt = lastModifiedAt,
        actionedBy = "UPDATED_BY_3",
        actionedByDisplayName = "UPDATED_BY_3_DISPLAY_NAME",
        source = DPS,
        activeCaseLoadId = PRISON_CODE_LEEDS,
      )
      auditEvent(
        action = UPDATED,
        description = "Alert updated",
        actionedAt = LocalDateTime.now().minusDays(3),
        actionedBy = "UPDATED_BY_1",
        actionedByDisplayName = "UPDATED_BY_1_DISPLAY_NAME",
        source = NOMIS,
        activeCaseLoadId = PRISON_CODE_LEEDS,
      )
    }

    assertThat(entity.lastModifiedAuditEvent()).usingRecursiveComparison().isEqualTo(
      AuditEvent(
        alert = entity,
        action = UPDATED,
        description = "Alert updated",
        actionedAt = lastModifiedAt,
        actionedBy = "UPDATED_BY_3",
        actionedByDisplayName = "UPDATED_BY_3_DISPLAY_NAME",
        source = DPS,
        activeCaseLoadId = PRISON_CODE_LEEDS,
      ),
    )
  }

  @Test
  fun `create audits event`() {
    val createdAt = LocalDateTime.now()
    val createdBy = "CREATED_BY"
    val createdByDisplayName = "CREATED_BY_DISPLAY_NAME"
    val source = DPS
    val activeCaseLoadId = PRISON_CODE_LEEDS

    val entity = Alert(
      alertCode = AC_VICTIM,
      prisonNumber = PRISON_NUMBER,
      description = "Alert description",
      authorisedBy = "A. Authorizer",
      activeFrom = LocalDate.now().minusDays(3),
      activeTo = LocalDate.now().plusDays(3),
      createdAt = createdAt,
      prisonCodeWhenCreated = null,
    ).create(
      createdAt = createdAt,
      createdBy = createdBy,
      createdByDisplayName = createdByDisplayName,
      source = source,
      activeCaseLoadId = activeCaseLoadId,
    )

    assertThat(entity.auditEvents().single()).usingRecursiveComparison().isEqualTo(
      AuditEvent(
        alert = entity,
        action = CREATED,
        description = "Alert created",
        actionedAt = createdAt,
        actionedBy = createdBy,
        actionedByDisplayName = createdByDisplayName,
        source = source,
        activeCaseLoadId = activeCaseLoadId,
      ),
    )
  }

  @Test
  fun `create raises domain event`() {
    val createdAt = LocalDateTime.now()
    val createdBy = "CREATED_BY"
    val createdByDisplayName = "CREATED_BY_DISPLAY_NAME"
    val source = DPS

    val entity = Alert(
      alertCode = AC_VICTIM,
      prisonNumber = PRISON_NUMBER,
      description = "Alert description",
      authorisedBy = "A. Authorizer",
      activeFrom = LocalDate.now().minusDays(3),
      activeTo = LocalDate.now().plusDays(3),
      createdAt = createdAt,
      prisonCodeWhenCreated = null,
    ).create(
      createdAt = createdAt,
      createdBy = createdBy,
      createdByDisplayName = createdByDisplayName,
      source = source,
      activeCaseLoadId = PRISON_CODE_MOORLANDS,
    )

    assertThat(entity.publishedDomainEvents().single()).isEqualTo(
      AlertCreatedEvent(
        alertUuid = entity.id,
        prisonNumber = entity.prisonNumber,
        alertCode = entity.alertCode.code,
        occurredAt = createdAt,
        source = source,
        createdBy = createdBy,
      ),
    )
  }

  @Test
  fun `update all properties audits event`() {
    val entity = alertEntity()
    val updatedDescription = "Updated description"
    val updatedAuthorisedBy = "Updated authorised by"
    val updatedActiveFrom = entity.activeFrom.plusDays(1)
    val updatedActiveTo = entity.activeTo!!.plusDays(1)
    val updatedAt = LocalDateTime.now()
    val updatedBy = "UPDATED_BY"
    val updatedByDisplayName = "UPDATED_BY_DISPLAY_NAME"
    val source = DPS
    val activeCaseLoadId = PRISON_CODE_MOORLANDS

    val sb = StringBuilder()
    sb.appendLine("Updated alert description from '${entity.description}' to '$updatedDescription'")
    sb.appendLine("Updated authorised by from '${entity.authorisedBy}' to '$updatedAuthorisedBy'")
    sb.appendLine("Updated active from from '${entity.activeFrom}' to '$updatedActiveFrom'")
    sb.appendLine("Updated active to from '${entity.activeTo}' to '$updatedActiveTo'")
    val expectedDescription = sb.toString().trimEnd()

    entity.update(
      description = updatedDescription,
      authorisedBy = updatedAuthorisedBy,
      activeFrom = updatedActiveFrom,
      activeTo = updatedActiveTo,
      updatedAt = updatedAt,
      updatedBy = updatedBy,
      updatedByDisplayName = updatedByDisplayName,
      source = source,
      activeCaseLoadId = activeCaseLoadId,
    )

    assertThat(entity.lastModifiedAt).isEqualTo(updatedAt)
    assertThat(entity.auditEvents().single { it.action == UPDATED }).usingRecursiveComparison().isEqualTo(
      AuditEvent(
        alert = entity,
        action = UPDATED,
        description = expectedDescription,
        actionedAt = updatedAt,
        actionedBy = updatedBy,
        actionedByDisplayName = updatedByDisplayName,
        source = source,
        activeCaseLoadId = activeCaseLoadId,
        descriptionUpdated = true,
        authorisedByUpdated = true,
        activeFromUpdated = true,
        activeToUpdated = true,
      ),
    )
  }

  @Test
  fun `update all properties raises domain event`() {
    val entity = alertEntity()
    val updatedAt = LocalDateTime.now()
    val updatedBy = "UPDATED_BY"
    val updatedByDisplayName = "UPDATED_BY_DISPLAY_NAME"
    val source = NOMIS
    val activeCaseLoadId = PRISON_CODE_LEEDS

    entity.update(
      description = "Updated description",
      authorisedBy = "Updated authorised by",
      activeFrom = entity.activeFrom.plusDays(1),
      activeTo = entity.activeTo!!.plusDays(1),
      updatedAt = updatedAt,
      updatedBy = updatedBy,
      updatedByDisplayName = updatedByDisplayName,
      source = source,
      activeCaseLoadId = activeCaseLoadId,
    )

    assertThat(entity.publishedDomainEvents().single()).isEqualTo(
      AlertUpdatedEvent(
        alertUuid = entity.id,
        prisonNumber = entity.prisonNumber,
        alertCode = entity.alertCode.code,
        occurredAt = updatedAt,
        source = source,
        updatedBy = updatedBy,
        descriptionUpdated = true,
        authorisedByUpdated = true,
        activeFromUpdated = true,
        activeToUpdated = true,
      ),
    )
  }

  @Test
  fun `update alert ignores no updates`() {
    val entity = alertEntity()

    entity.update(
      description = entity.description,
      authorisedBy = entity.authorisedBy,
      activeFrom = entity.activeFrom,
      activeTo = entity.activeTo,
      updatedBy = "UPDATED_BY",
      updatedByDisplayName = "UPDATED_BY_DISPLAY_NAME",
      source = DPS,
      activeCaseLoadId = PRISON_CODE_MOORLANDS,
    )

    assertThat(entity.lastModifiedAt).isNull()
    assertThat(entity.auditEvents().none { it.action == UPDATED }).isTrue
    assertThat(entity.publishedDomainEvents()).isEmpty()
  }

  @Test
  fun `update alert ignores null description`() {
    val entity = alertEntity()

    entity.update(
      description = null,
      authorisedBy = entity.authorisedBy,
      activeFrom = entity.activeFrom,
      activeTo = entity.activeTo,
      updatedBy = "UPDATED_BY",
      updatedByDisplayName = "UPDATED_BY_DISPLAY_NAME",
      source = NOMIS,
      activeCaseLoadId = PRISON_CODE_LEEDS,
    )

    assertThat(entity.description).isNotNull()
    assertThat(entity.lastModifiedAt).isNull()
    assertThat(entity.auditEvents().none { it.action == UPDATED }).isTrue
    assertThat(entity.publishedDomainEvents()).isEmpty()
  }

  @Test
  fun `update alert ignores null authorised by`() {
    val entity = alertEntity()

    entity.update(
      description = entity.description,
      authorisedBy = null,
      activeFrom = entity.activeFrom,
      activeTo = entity.activeTo,
      updatedBy = "UPDATED_BY",
      updatedByDisplayName = "UPDATED_BY_DISPLAY_NAME",
      source = DPS,
      activeCaseLoadId = PRISON_CODE_MOORLANDS,
    )

    assertThat(entity.authorisedBy).isNotNull()
    assertThat(entity.lastModifiedAt).isNull()
    assertThat(entity.auditEvents().none { it.action == UPDATED }).isTrue
    assertThat(entity.publishedDomainEvents()).isEmpty()
  }

  @Test
  fun `update alert ignores null active from`() {
    val entity = alertEntity()

    entity.update(
      description = entity.description,
      authorisedBy = entity.authorisedBy,
      activeFrom = null,
      activeTo = entity.activeTo,
      updatedBy = "UPDATED_BY",
      updatedByDisplayName = "UPDATED_BY_DISPLAY_NAME",
      source = NOMIS,
      activeCaseLoadId = PRISON_CODE_LEEDS,
    )

    assertThat(entity.activeFrom).isNotNull()
    assertThat(entity.lastModifiedAt).isNull()
    assertThat(entity.auditEvents().none { it.action == UPDATED }).isTrue
    assertThat(entity.publishedDomainEvents()).isEmpty()
  }

  @Test
  fun `update alert description only`() {
    val entity = alertEntity()
    val updatedDescription = "Updated description"
    val updatedAt = LocalDateTime.now()
    val source = NOMIS
    val expectedDescription = "Updated alert description from '${entity.description}' to '$updatedDescription'"

    entity.update(
      description = updatedDescription,
      authorisedBy = entity.authorisedBy,
      activeFrom = entity.activeFrom,
      activeTo = entity.activeTo,

      updatedAt = updatedAt,
      updatedBy = "UPDATED_BY",
      updatedByDisplayName = "UPDATED_BY_DISPLAY_NAME",
      source = source,
      activeCaseLoadId = PRISON_CODE_LEEDS,
    )

    assertThat(entity.lastModifiedAt).isEqualTo(updatedAt)
    with(entity.auditEvents().single { it.action == UPDATED }) {
      assertThat(description).isEqualTo(expectedDescription)
      assertThat(descriptionUpdated).isTrue
      assertThat(authorisedByUpdated).isFalse
      assertThat(activeFromUpdated).isFalse
      assertThat(activeToUpdated).isFalse
    }
    with(entity.publishedDomainEvents().single() as AlertUpdatedEvent) {
      assertThat(source).isEqualTo(source)
      assertThat(descriptionUpdated).isTrue
      assertThat(authorisedByUpdated).isFalse
      assertThat(activeFromUpdated).isFalse
      assertThat(activeToUpdated).isFalse
    }
  }

  @Test
  fun `update alert authorised by only`() {
    val entity = alertEntity()
    val updatedAuthorisedBy = "Updated authorised by"
    val updatedAt = LocalDateTime.now()
    val source = DPS
    val expectedDescription = "Updated authorised by from '${entity.authorisedBy}' to '$updatedAuthorisedBy'"

    entity.update(
      description = entity.description,
      authorisedBy = updatedAuthorisedBy,
      activeFrom = entity.activeFrom,
      activeTo = entity.activeTo,

      updatedAt = updatedAt,
      updatedBy = "UPDATED_BY",
      updatedByDisplayName = "UPDATED_BY_DISPLAY_NAME",
      source = source,
      activeCaseLoadId = PRISON_CODE_LEEDS,
    )

    assertThat(entity.lastModifiedAt).isEqualTo(updatedAt)
    with(entity.auditEvents().single { it.action == UPDATED }) {
      assertThat(description).isEqualTo(expectedDescription)
      assertThat(descriptionUpdated).isFalse
      assertThat(authorisedByUpdated).isTrue
      assertThat(activeFromUpdated).isFalse
      assertThat(activeToUpdated).isFalse
    }
    with(entity.publishedDomainEvents().single() as AlertUpdatedEvent) {
      assertThat(source).isEqualTo(source)
      assertThat(descriptionUpdated).isFalse
      assertThat(authorisedByUpdated).isTrue
      assertThat(activeFromUpdated).isFalse
      assertThat(activeToUpdated).isFalse
    }
  }

  @Test
  fun `update alert active from only`() {
    val entity = alertEntity()
    val updatedActiveFrom = entity.activeFrom.plusDays(1)
    val updatedAt = LocalDateTime.now()
    val source = NOMIS
    val expectedDescription = "Updated active from from '${entity.activeFrom}' to '$updatedActiveFrom'"

    entity.update(
      description = entity.description,
      authorisedBy = entity.authorisedBy,
      activeFrom = updatedActiveFrom,
      activeTo = entity.activeTo,

      updatedAt = updatedAt,
      updatedBy = "UPDATED_BY",
      updatedByDisplayName = "UPDATED_BY_DISPLAY_NAME",
      source = source,
      activeCaseLoadId = PRISON_CODE_MOORLANDS,
    )

    assertThat(entity.lastModifiedAt).isEqualTo(updatedAt)
    with(entity.auditEvents().single { it.action == UPDATED }) {
      assertThat(description).isEqualTo(expectedDescription)
      assertThat(descriptionUpdated).isFalse
      assertThat(authorisedByUpdated).isFalse
      assertThat(activeFromUpdated).isTrue
      assertThat(activeToUpdated).isFalse
    }
    with(entity.publishedDomainEvents().single() as AlertUpdatedEvent) {
      assertThat(source).isEqualTo(source)
      assertThat(descriptionUpdated).isFalse
      assertThat(authorisedByUpdated).isFalse
      assertThat(activeFromUpdated).isTrue
      assertThat(activeToUpdated).isFalse
    }
  }

  @Test
  fun `update alert active to only`() {
    val entity = alertEntity()
    val updatedActiveTo = entity.activeTo!!.plusDays(1)
    val updatedAt = LocalDateTime.now()
    val source = DPS
    val expectedDescription = "Updated active to from '${entity.activeTo}' to '$updatedActiveTo'"

    entity.update(
      description = entity.description,
      authorisedBy = entity.authorisedBy,
      activeFrom = entity.activeFrom,
      activeTo = updatedActiveTo,

      updatedAt = updatedAt,
      updatedBy = "UPDATED_BY",
      updatedByDisplayName = "UPDATED_BY_DISPLAY_NAME",
      source = source,
      activeCaseLoadId = PRISON_CODE_LEEDS,
    )

    assertThat(entity.lastModifiedAt).isEqualTo(updatedAt)
    with(entity.auditEvents().single { it.action == UPDATED }) {
      assertThat(description).isEqualTo(expectedDescription)
      assertThat(descriptionUpdated).isFalse
      assertThat(authorisedByUpdated).isFalse
      assertThat(activeFromUpdated).isFalse
      assertThat(activeToUpdated).isTrue
    }
    with(entity.publishedDomainEvents().single() as AlertUpdatedEvent) {
      assertThat(source).isEqualTo(source)
      assertThat(descriptionUpdated).isFalse
      assertThat(authorisedByUpdated).isFalse
      assertThat(activeFromUpdated).isFalse
      assertThat(activeToUpdated).isTrue
    }
  }

  @Test
  fun `delete audits event`() {
    val entity = alertEntity()
    val deletedAt = LocalDateTime.now()
    val source = NOMIS
    val activeCaseLoadId = PRISON_CODE_LEEDS

    entity.delete(deletedAt, "DELETED_BY", "DELETED_BY_DISPLAY_NAME", source, activeCaseLoadId)

    assertThat(entity.lastModifiedAt).isEqualTo(deletedAt)
    assertThat(entity.deletedAt).isEqualTo(deletedAt)
    assertThat(entity.auditEvents().single { it.action == DELETED }).usingRecursiveComparison().isEqualTo(
      AuditEvent(
        alert = entity,
        action = DELETED,
        description = "Alert deleted",
        actionedAt = deletedAt,
        actionedBy = "DELETED_BY",
        actionedByDisplayName = "DELETED_BY_DISPLAY_NAME",
        source = source,
        activeCaseLoadId = activeCaseLoadId,
      ),
    )
  }

  @Test
  fun `delete raises domain event`() {
    val entity = alertEntity()
    val deletedAt = LocalDateTime.now()
    val deletedBy = "DELETED_BY"
    val source = DPS

    entity.delete(deletedAt, deletedBy, "DELETED_BY_DISPLAY_NAME", source, PRISON_CODE_LEEDS)

    assertThat(entity.publishedDomainEvents().single()).isEqualTo(
      AlertDeletedEvent(
        alertUuid = entity.id,
        prisonNumber = entity.prisonNumber,
        alertCode = entity.alertCode.code,
        occurredAt = deletedAt,
        source = source,
        deletedBy = deletedBy,
      ),
    )
  }

  private fun alertEntity(
    createdAt: LocalDateTime = LocalDateTime.now().minusDays(3),
    createdBy: String = "CREATED_BY",
    createdByDisplayName: String = "CREATED_BY_DISPLAY_NAME",
    source: Source = DPS,
  ) = Alert(
    alertCode = AC_VICTIM,
    prisonNumber = PRISON_NUMBER,
    description = "Alert description",
    authorisedBy = "A. Authorizer",
    activeFrom = LocalDate.now().minusDays(3),
    activeTo = LocalDate.now().plusDays(3),
    createdAt = createdAt,
    prisonCodeWhenCreated = null,
  ).apply {
    auditEvent(
      action = CREATED,
      description = "Alert created",
      actionedAt = createdAt,
      actionedBy = createdBy,
      actionedByDisplayName = createdByDisplayName,
      source = source,
      activeCaseLoadId = PRISON_CODE_MOORLANDS,
    )
  }
}
