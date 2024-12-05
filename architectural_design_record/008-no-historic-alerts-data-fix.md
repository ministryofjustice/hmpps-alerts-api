# ADR008: Historic Prisoner Alerts data fix will not be run in production

## Status: Accepted by David Winchurch (Technical Architect), Joe Hyland Deeson (Product Manager) and Andy Marke (Syscon)

## Context

[ADR005](005-alerts-flattening-data-fix.md) originally recorded that the Syscon classic NOMIS team would develop and run a data fix to copy any historic Prisoner Alerts considered missing onto the current booking. This would have synced them to DPS and made them visible in both services.

The data fix script was developed by Paul Morris and tested in the pre prod environment. It was discovered that 400,000 alerts would have been surfaced by this script including up to 100 additional alerts for a small number of prisoners.

As of 11/11/2024, the DPS Alerts API is in use by all prisons and the NOMIS Alerts screen has been switched off nationwide. Prisons can no longer switch to a prisoner's previous booking and view historic alerts. As of 09/12/2024 no prison has requested the ability to view historic alerts in DPS.

## Decision

The team has decided not to run the data fix script in production.

## Consequences

- The DPS alerts data set is now the system of record for alerts and no additional NOMIS alerts data will be migrated
- Prisons will not be required to audit newly surfaced alerts with the assumption being that prisons have manually added back any alerts that were missed during the copying process prior to 19/03/2022
- Historic alerts data will remain only in NOMIS and backups of the NOMIS dataset
- This is in line with Syscon's request that NOMIS -> DPS migrations do not attempt to rebuild audit data from an incomplete source i.e. the booking history and NOMIS system audit columns
- Auditing is covered in more detail in [ADR007: NOMIS system audit columns will be migrated however full audit history will not](007-system-audit-columns-will-be-migrated.md) 
