package uk.gov.justice.digital.hmpps.hmppsalertsapi.enumeration

enum class DomainEventType(
  val eventType: String,
  val description: String,
) {
  ALERT_CREATED("person.alert.created", "An alert has been created in the alerts service"),
  ALERT_UPDATED("person.alert.updated", "An alert has been updated in the alerts service"),
  ALERT_INACTIVE("person.alert.inactive", "An alert has been updated in the alerts service"),
  ALERT_DELETED("person.alert.deleted", "An alert has been deleted in the alerts service"),
  ALERT_CODE_CREATED("prisoner-alerts.alert-code-created", "An alert code has been created in the alerts reference data service"),
  ALERT_CODE_DEACTIVATED("prisoner-alerts.alert-code-deactivated", "An alert code has been deactivated in the alerts reference data service"),
  ALERT_CODE_REACTIVATED("prisoner-alerts.alert-code-reactivated", "An alert code has been reactivated in the alerts reference data service"),
  ALERT_CODE_UPDATED("prisoner-alerts.alert-code-updated", "An alert code has been updated in the alerts reference data service"),
  ALERT_TYPE_CREATED("prisoner-alerts.alert-type-created", "An alert type has been created in the alerts reference data service"),
  ALERT_TYPE_DEACTIVATED("prisoner-alerts.alert-type-deactivated", "An alert type has been deactivated in the alerts reference data service"),
  ALERT_TYPE_REACTIVATED("prisoner-alerts.alert-type-reactivated", "An alert type has been reactivated in the alerts reference data service"),
  ALERT_TYPE_UPDATED("prisoner-alerts.alert-type-updated", "An alert type has been updated in the alerts reference data service"),
  PERSON_ALERTS_CHANGED("person.alerts.changed", "The alerts for a given person have been updated in the alerts service"),
}
