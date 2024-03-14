package uk.gov.justice.digital.hmpps.hmppsalertsapi.repository

import jakarta.persistence.criteria.CriteriaBuilder
import jakarta.persistence.criteria.CriteriaQuery
import jakarta.persistence.criteria.Predicate
import jakarta.persistence.criteria.Root
import org.springframework.data.jpa.domain.Specification
import uk.gov.justice.digital.hmpps.hmppsalertsapi.entity.Alert
import uk.gov.justice.digital.hmpps.hmppsalertsapi.entity.AlertCode
import uk.gov.justice.digital.hmpps.hmppsalertsapi.entity.AlertType
import java.time.LocalDate

class AlertsFilter(
  val prisonNumber: String,
  val isActive: Boolean? = null,
  val alertType: String? = null,
) : Specification<Alert> {
  override fun toPredicate(root: Root<Alert>, query: CriteriaQuery<*>, criteriaBuilder: CriteriaBuilder): Predicate? {
    val predicates = MutableList<Predicate>(4) { criteriaBuilder.conjunction() }

    predicates.add(criteriaBuilder.equal(root.get<String>("prisonNumber"), prisonNumber))

    isActive?.let {
      when (it) {
        true -> {
          predicates.add(criteriaBuilder.lessThanOrEqualTo(root.get("activeFrom"), criteriaBuilder.literal(LocalDate.now())))
          predicates.add(criteriaBuilder.or(criteriaBuilder.isNull(root.get<LocalDate>("activeTo")), criteriaBuilder.greaterThan(root.get("activeTo"), criteriaBuilder.literal(LocalDate.now()))))
        }
        false -> {
          predicates.add(criteriaBuilder.or(criteriaBuilder.greaterThan(root.get("activeFrom"), criteriaBuilder.literal(LocalDate.now())), criteriaBuilder.lessThanOrEqualTo(root.get("activeTo"), criteriaBuilder.literal(LocalDate.now()))))
        }
      }
    }

    alertType?.let {
      predicates.add(root.get<AlertCode>("alertCode").get<AlertType>("alertType").get<String>("code").`in`(alertType.split(",")))
    }

    return criteriaBuilder.and(*predicates.toTypedArray())
  }
}
