package uk.gov.justice.digital.hmpps.hmppsalertsapi.backfill

import io.swagger.v3.oas.annotations.Operation
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.hmppsalertsapi.resource.ROLE_CASE_NOTES
import java.time.LocalDate

@RestController
@RequestMapping("alerts/case-notes", produces = [MediaType.APPLICATION_JSON_VALUE])
class BackfillCaseNotesController(private val forCaseNotes: GetAlertsForCaseNotes) {
  @Operation(hidden = true)
  @ResponseStatus(HttpStatus.OK)
  @GetMapping("/{prisonNumber}")
  @PreAuthorize("hasRole('$ROLE_CASE_NOTES')")
  fun getAppropriateAlerts(
    @PathVariable prisonNumber: String,
    @RequestParam from: LocalDate,
    @RequestParam to: LocalDate,
  ): CaseNoteAlertResponse = CaseNoteAlertResponse(forCaseNotes.get(prisonNumber, from, to))
}

data class CaseNoteAlertResponse(val content: List<CaseNoteAlert>)
