package uk.gov.justice.digital.hmpps.hmppsalertsapi.validator

import jakarta.validation.ConstraintValidator
import jakarta.validation.ConstraintValidatorContext
import uk.gov.justice.digital.hmpps.hmppsalertsapi.model.request.MigrateAlert

class MigrateAlertUpdatedByValidator : ConstraintValidator<UpdatedByRequired, MigrateAlert> {
  override fun isValid(value: MigrateAlert, context: ConstraintValidatorContext?): Boolean {
    return if (value.updatedAt != null) {
      value.updatedBy != null
    } else {
      true
    }
  }
}
