# ADR005: Prisoner Alerts data flattening implemented as a NOMIS data fix

## Status: Superseded by [ADR008](008-no-historic-alerts-data-fix.md). Originally accepted by Claire Fraiser and Paul Morris from the classic NOMIS team and Darren Betts-Graves

## Context

A small percentage of prisoner alerts associated with historic bookings exist that are not on the current booking. Early on in the prisoner alerts project, the team took the decision to include the latest instance of any missed prisoner alerts when migrating the prisoner alerts data from NOMIS.

This however resulted in a difference in the view of affected prisoners’ alerts between NOMIS and DPS. The prisoner alerts from historic bookings would be displayed in DPS but not in NOMIS by default. Several options to resolve this were presented in a subsequent options paper with option 3b: apply a NOMIS data fix to copy historic prisoner alerts to the current booking post migration being the chosen option.

A meeting to discuss the practicalities of this decision was held on the 3rd of July 2024 with Joe Hyland-Deeson, David Winchurch, Claire Fraiser, Paul Morris, Andy Marke and Darren Betts-Graves in attendance.

## Decision

The following was agreed at the meeting:

- The ability to add prisoner alerts to historic bookings in NOMIS will be removed
- Historic prisoner alerts not on the current booking for a prisoner should be brought forward and added to that current booking
  - This will be done via a NOMIS data fix implemented by Paul Morris
  - The new prisoner alerts on the current booking will be synced across to DPS via the existing two-way sync process
  - The existing database triggers that add a case note for active and inactive prisoner alerts will add a new case note on the current booking per prisoner alert copied to the current booking
- The NOMIS data fix will copy all "missed" prisoner alerts onto the current booking regardless of status
  - **This is a change to the agreed flattening logic of only copying the most recent alert**
- A list of prisoners and their prisons will be generated as an output of the NOMIS data fix
  - This list will be sent to the prisons affected in coordination with Darren, so they can choose to review the prisoners
  - The NART team will be informed that this is happening as the new prisoner alerts may affect their reporting trends
- M&I and Syscon will look into applying the fix to the mistakes made when a prisoner is recalled event from DPS to NOMIS

## Consequences

- All instances of prisoner alerts not present on the current booking will be brought forward
- Prisoner alerts used to create a timeline narrative, for example ACCT prisoner alerts, will be brought forward retaining that narrative
- No decision on which instance of a prisoner alert is most appropriate will be made. The prisons who own the data can make that choice later
- No possibility of lost prisoner alerts once the prisoner alerts data is removed from NOMIS. The DPS service will have the full data set
- One-off data fix to NOMIS only. No further changes required to the Alert API or sync process
- The removal of the ability to add prisoner alerts to historic bookings in NOMIS prevents any further creation of prisoner alerts that are not present on the current booking
