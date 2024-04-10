package uk.gov.justice.digital.hmpps.hmppsalertsapi.validator

import jakarta.validation.ConstraintValidator
import jakarta.validation.ConstraintValidatorContext
import uk.gov.justice.digital.hmpps.hmppsalertsapi.common.onOrBefore
import uk.gov.justice.digital.hmpps.hmppsalertsapi.model.request.MigrateAlert

class MigrateAlertDateValidator : ConstraintValidator<DateComparison, MigrateAlert> {
  override fun isValid(value: MigrateAlert, context: ConstraintValidatorContext?): Boolean {
    return if (value.activeTo != null) {
      value.activeFrom.onOrBefore(value.activeTo)
    } else {
      true
    }
  }
}
