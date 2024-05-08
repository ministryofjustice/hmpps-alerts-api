# ADR003: Prisoner and Booking Merge Events

## Status

Proposed, accepted by @Andy Marke

## Context

Prisoner records can be merged together when it is discovered that two records represent the same person. This is a regular occurrence particularly in reception prisons due to people commonly being booked in under different names and aliases.

When two prisoner records are merged, the associated booking from the prisoner to be merged from is moved to the prisoner to be merged to. This booking becomes the new current booking by being assigned sequence 1. The alerts from what is now the previous booking are copied to the new booking, creating a combined list of alerts from both bookings.

A duplicate check is applied when copying the alerts from the previous booking. If any of the copied alerts have the same date, code and status, the duplicate on the current booking is made inactive. Because of the date check, **it is possible to end up with two active alerts with the same code if they have different dates**. This is considered invalid however it is possible and must therefore be allowed by the implementation for this ticket.

## Decision

We propose a two phase approach to handling prisoner merge events for alerts.

### Strategic short term goal

**NOMIS owns merge logic and drives DPS**

- Allow NOMIS to merge the alerts data and react appropriately
- This requires changes to the sync service and the Alerts API to listen for the merge event, update any sync mappings and create any new alerts.

### End goal for prisoner merge events

**DPS owns merge logic and drives NOMIS**

- The alerts service should take over responsibility for merging prisoner alerts data from NOMIS
- The service should own the business logic and drive the alerts data merge, syncing changes back into NOMIS.
- **Not doing so would prevent moving to a one way sync**.

Note that this requires a change request to switch off the NOMIS alerts data merge process and should not be attempted until after full roll out across the prison estate.

## Consequences

- We can rapidly develop and test merge logic based on the current NOMIS approach
- We can launch the service sooner without a dependency on a NOMIS change
- We have a plan to take over the alerts merging logic allowing it to be improved
