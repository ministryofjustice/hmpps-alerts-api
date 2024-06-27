# ADR004: Prisoner and booking changes in NOMIS triggers a prisoner alerts resync

## Status

Approved by @Richard Adams as Principal Technical Architect

## Context

There are a number of prisoner level scenarios in NOMIS that affect alerts:

1. Person re-enters prison under a new booking
2. Two prisoner records and their bookings are merged together
3. Person recalled to prison is initially incorrectly received on a new booking and then corrected by:
   1. Releasing the prisoner on the current booking
   2. Receive them back into prison on the previous booking which then becomes their current booking
   3. The process to copy alerts from the new previous booking to the new current booking **is not triggered** in this scenario
4. Potentially other prisoner and booking level processes

Following the rollout of alerts migration and sync to production on Thursday 20th of June 2024, the team has implemented a solution related to scenario 3. The same solution was applied to a variant of scenario 1. We are continuously monitoring for other scenarios that similarly affect alerts.

### Change categorisation

There are two distinct categories of alerts data change:

1. Alert level changes
   1. Changes that affect a singular alert e.g. add alert, update description, close an alert
   2. With two-way sync, these changes can be driven from DPS or NOMIS 
   3. No known sync complexities. Whether driven from DPS or NOMIS, all changes are applied via the direct mappings from the two services
2. Prisoner level alert changes
   1. Changes that can affect some or all of a prisoners’ alerts
   2. Currently driven from NOMIS via any of the above scenarios
   3. Complex and often difficult to understand NOMIS business logic and the resulting mapping to DPS
   4. NOMIS’ view of a prisoner’s alerts is authoritative after the event
   5. In future, logic could be driven by DPS

## Decision

The approach for all scenarios where prisoner level events have affected a prisoner’s alerts is to switch to a universal resync process. The detection of such an event by the sync service results in a full resync of their alerts in DPS. This resync is driven by NOMIS and replicates its view of alerts back into DPS. It is conceptually similar to re-migrating a single prisoner’s alerts.

The merge scenario involves resyncing two prisoners’ alerts. The retained prison number syncs the combined set of alerts and the removed prisoner syncs an empty list of alerts. This results in all alerts from the removed prisoner being deleted.

Note this is an alteration to prisoner level alert changes only e.g. new and switched bookings or merges. Alert level changes use the existing, proven two-way sync approach. It is this existing alert level two-way sync that supports the process to switch off the NOMIS alerts screen.

Resync can also become a two-way process. In future, the alerts copying process in NOMIS could be switched off and the sync service could use DPS as the source of truth for the alerts that should be on the current booking. In this way, the direction is reversed and DPS becomes the driver of alerts logic rather than NOMIS.

### Resync process

1. The sync service receives all known events that can affect a prisoners’ alerts
2. On receiving one such event, the sync service gets the list of alerts from the current booking in NOMIS
3. The sync service calls a new /resync/{prisonNumber}/alerts endpoint with that list
4. The Alerts API deletes all existing alerts for the prisoner and creates new ones in their place
5. The existing alerts audit history is retained and copied to the new alerts
6. The sync service deletes current mappings and creates new ones

### Consequences

- Significantly simplifies the investigation and development work needed to incorporate a new prisoner level alert change scenario
- All current and future prisoner level alert changes can be covered by the same process
- The resync implementation on the Alerts API side is free to improve independently of NOMIS
- This approach supports moving alert copying and flattening logic out of NOMIS and into DPS, **moving the dial on the new alerts service taking ownership of this business logic**
