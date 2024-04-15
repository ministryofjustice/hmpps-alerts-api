package uk.gov.justice.digital.hmpps.hmppsalertsapi.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.hmppsalertsapi.entity.MigratedAlert

@Repository
interface MigratedAlertRepository : JpaRepository<MigratedAlert, Long> {
  fun findByOffenderBookIdAndAlertSeq(offenderBookId: Long, alertSeq: Int): MigratedAlert?
}
