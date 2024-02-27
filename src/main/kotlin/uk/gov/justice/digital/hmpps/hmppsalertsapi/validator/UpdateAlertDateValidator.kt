package uk.gov.justice.digital.hmpps.hmppsalertsapi.validator

import jakarta.validation.ConstraintValidator
import jakarta.validation.ConstraintValidatorContext
import uk.gov.justice.digital.hmpps.hmppsalertsapi.model.request.UpdateAlert

class UpdateAlertDateValidator : ConstraintValidator<DateComparison, UpdateAlert> {
  override fun isValid(value: UpdateAlert, context: ConstraintValidatorContext?): Boolean {
    return if (value.activeFrom != null && value.activeTo != null) {
      value.activeFrom.isBefore(value.activeTo)
    } else {
      true
    }
  }
}
