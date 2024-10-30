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
  val alertCode: String? = null,
  val activeFromStart: LocalDate? = null,
  val activeFromEnd: LocalDate? = null,
  val search: String? = null,
) : Specification<Alert> {
  override fun toPredicate(root: Root<Alert>, query: CriteriaQuery<*>, criteriaBuilder: CriteriaBuilder): Predicate? {
    val predicates = MutableList<Predicate>(4) { criteriaBuilder.conjunction() }

    predicates.add(criteriaBuilder.equal(root.get<String>("prisonNumber"), prisonNumber))

    isActive?.let {
      when (it) {
        true -> {
          predicates.add(criteriaBuilder.or(criteriaBuilder.isNull(root.get<LocalDate>("activeTo")), criteriaBuilder.greaterThan(root.get("activeTo"), criteriaBuilder.literal(LocalDate.now()))))
        }
        false -> {
          predicates.add(criteriaBuilder.lessThanOrEqualTo(root.get("activeTo"), criteriaBuilder.literal(LocalDate.now())))
        }
      }
    }

    alertType?.let {
      predicates.add(root.get<AlertCode>("alertCode").get<AlertType>("alertType").get<String>("code").`in`(alertType.split(",")))
    }

    alertCode?.let {
      predicates.add(root.get<AlertCode>("alertCode").get<String>("code").`in`(alertCode.split(",")))
    }

    search?.let {
      predicates.add(
        criteriaBuilder.or(
          criteriaBuilder.like(criteriaBuilder.lower(root.get("description")), "%${it.lowercase()}%"),
          criteriaBuilder.like(criteriaBuilder.lower(root.get("authorisedBy")), "%${it.lowercase()}%"),
        ),
      )
    }

    activeFromStart?.let {
      predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.get("activeFrom"), criteriaBuilder.literal(activeFromStart)))
    }

    activeFromEnd?.let {
      predicates.add(criteriaBuilder.lessThanOrEqualTo(root.get("activeFrom"), criteriaBuilder.literal(activeFromEnd)))
    }

    return criteriaBuilder.and(*predicates.toTypedArray())
  }
}
