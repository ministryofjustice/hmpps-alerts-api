package uk.gov.justice.digital.hmpps.hmppsalertsapi.exceptions

data class PermissionDeniedException(val resource: String, val identifier: String) : RuntimeException("Permission denied for $resource $identifier")
