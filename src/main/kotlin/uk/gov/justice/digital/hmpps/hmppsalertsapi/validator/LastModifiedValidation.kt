package uk.gov.justice.digital.hmpps.hmppsalertsapi.validator

import jakarta.validation.Constraint
import jakarta.validation.ConstraintValidator
import jakarta.validation.ConstraintValidatorContext
import jakarta.validation.Payload
import java.time.LocalDateTime
import kotlin.reflect.KClass

interface Modifiable {
  val lastModifiedAt: LocalDateTime?
  val lastModifiedBy: String?
  val lastModifiedByDisplayName: String?
}

class LastModifiedByValidator : ConstraintValidator<LastModifiedByRequired, Modifiable> {
  override fun isValid(value: Modifiable, context: ConstraintValidatorContext?): Boolean {
    return value.lastModifiedAt == null || value.lastModifiedBy?.isNotBlank() ?: false
  }
}

class LastModifiedByDisplayNameValidator : ConstraintValidator<LastModifiedByDisplayNameRequired, Modifiable> {
  override fun isValid(value: Modifiable, context: ConstraintValidatorContext?): Boolean {
    return value.lastModifiedAt == null || value.lastModifiedByDisplayName?.isNotBlank() ?: false
  }
}

@Constraint(validatedBy = [LastModifiedByValidator::class])
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class LastModifiedByRequired(
  val message: String,
  val groups: Array<KClass<*>> = [],
  val payload: Array<KClass<out Payload>> = [],
)

@Constraint(validatedBy = [LastModifiedByDisplayNameValidator::class])
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class LastModifiedByDisplayNameRequired(
  val message: String,
  val groups: Array<KClass<*>> = [],
  val payload: Array<KClass<out Payload>> = [],
)
