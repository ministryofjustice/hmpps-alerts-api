package uk.gov.justice.digital.hmpps.hmppsalertsapi

import com.fasterxml.uuid.Generators
import java.util.UUID

object IdGenerator {
  fun newUuid(): UUID {
    return Generators.timeBasedEpochGenerator().generate()
  }
}
