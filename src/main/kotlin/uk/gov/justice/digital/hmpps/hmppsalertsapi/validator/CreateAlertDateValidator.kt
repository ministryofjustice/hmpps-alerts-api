package uk.gov.justice.digital.hmpps.hmppsalertsapi.validator

import jakarta.validation.ConstraintValidator
import jakarta.validation.ConstraintValidatorContext
import uk.gov.justice.digital.hmpps.hmppsalertsapi.common.onOrBefore
import uk.gov.justice.digital.hmpps.hmppsalertsapi.model.request.CreateAlert

class CreateAlertDateValidator : ConstraintValidator<DateComparison, CreateAlert> {
  override fun isValid(value: CreateAlert, context: ConstraintValidatorContext?): Boolean {
    return if (value.activeFrom != null && value.activeTo != null) {
      value.activeFrom.onOrBefore(value.activeTo)
    } else {
      true
    }
  }
}
