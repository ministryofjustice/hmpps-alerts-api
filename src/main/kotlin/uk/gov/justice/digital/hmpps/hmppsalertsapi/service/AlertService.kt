package uk.gov.justice.digital.hmpps.hmppsalertsapi.service

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.hmppsalertsapi.repository.AlertRepository

@Service
@Transactional
class AlertService(
  private val alertRepository: AlertRepository,
)
