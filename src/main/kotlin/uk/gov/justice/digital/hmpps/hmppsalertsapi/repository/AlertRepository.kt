package uk.gov.justice.digital.hmpps.hmppsalertsapi.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.hmppsalertsapi.entity.Alert

@Repository
interface AlertRepository : JpaRepository<Alert, Long>
