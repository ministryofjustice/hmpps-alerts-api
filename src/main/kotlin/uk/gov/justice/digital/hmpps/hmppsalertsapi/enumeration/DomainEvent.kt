package uk.gov.justice.digital.hmpps.hmppsalertsapi.enumeration

enum class DomainEvent(
  val eventType: String,
  val description: String,
) {
  ALERT_CREATED("prisoner-alerts.alert-created", "An alert has been created in the alerts service"),
  ALERT_UPDATED("prisoner-alerts.alert-updated", "An alert has been updated in the alerts service"),
  ALERT_DELETED("prisoner-alerts.alert-deleted", "An alert has been deleted in the alerts service"),
}
