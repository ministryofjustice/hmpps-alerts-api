package uk.gov.justice.digital.hmpps.hmppsalertsapi.validator

import jakarta.validation.Constraint
import jakarta.validation.ConstraintValidator
import jakarta.validation.ConstraintValidatorContext
import jakarta.validation.Payload
import java.time.LocalDate
import kotlin.reflect.KClass

interface ActiveDateRange {
  val activeFrom: LocalDate?
  val activeTo: LocalDate?
}

class ActiveDateRangeValidator : ConstraintValidator<ActiveDateRangeValid, ActiveDateRange> {
  override fun isValid(value: ActiveDateRange, context: ConstraintValidatorContext?): Boolean {
    return !(value.activeFrom?.isAfter(value.activeTo ?: value.activeFrom) ?: false)
  }
}

@Constraint(validatedBy = [ActiveDateRangeValidator::class])
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class ActiveDateRangeValid(
  val message: String = "Active from must be before active to",
  val groups: Array<KClass<*>> = [],
  val payload: Array<KClass<out Payload>> = [],
)
