# ADR007: NOMIS system audit columns will be migrated however full audit history will not

## Status: Accepted by David Winchurch (Technical Architect) and Andy Marke (Syscon)

## Context

The primary NOMIS database has two types of auditing; business and system. Business auditing captures data either entered by the user or via the NOMIS form's business logic. They are directly relevant to the data domain and valid to display to users. They are separate from the system auditing columns which are set by database triggers using the database user's properties. The purpose of the NOMIS system auditing columns is support of the NOMIS application itself. They are not intended to be used as a record of user activities.

DPS did use these system auditing columns as if they were business auditing values for Alerts. The created and modified timestamps and user information were displayed directly to the users as business auditing information. This is now considered a mistake and should have been resolved by adding defined data capture and columns for who added the alert, last changed it and subsequently ended the alert.

A separate database exists for auditing every action in NOMIS. This database is so large that it cannot be queried normally. When an investigation is required, a subset of the data covering the time period in question is extracted and put in a temporary database. As of late 2024, there are no plans to migrate this audit database.

The view of alerts on historic bookings could also be considered audit information and potentially relevant to an investigation. In line with the overall HMPPS decision to not maintain booking information and entity linking via it in DPS, and as decided in [ADR001: Associating Prisoner Alerts at the Person Level](001-person-level-association.md), this historic view of alerts will not be migrated.

## Decision

- NOMIS Alerts system auditing columns; `CREATE_DATETIME`, `CREATE_USER_ID`, `MODIFY_DATETIME` and `MODIFY_USER_ID` will be both migrated and synced to DPS
- During migration, these audit values will be used to create audit events with the source of these events marked as NOMIS
- Audit events from NOMIS are accepted as best efforts and will not be considered accurate
- No other system auditing columns will be migrated or synced to DPS
- No attempt will be made to migrate the full audit history either by inferring it from alerts on historic bookings or from the separate NOMIS audit database

## Consequences

- Alert audit events populated using NOMIS system audit values will continue to be displayed to users
- Following the nationwide switch off of the NOMIS alerts screen on 11/11/2024, all audit events except for OCG Nominal alerts will be added via DPS
- The OCG Nominal alert audit events will be added via DPS following the release of the bulk alerts tool in December 2024
- This makes 11/11/2024 an "auditing epoc"
- Investigations for dates prior to this auditing epoc should use the NOMIS alerts data set and audit database
- Investigations after should use the DPS audit events
