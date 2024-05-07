# ADR002: Adopting a Two-Way Sync for Alerts

## Status

Approved by @Alice Noakes as Service Owner and @Richard Adams as Principal Technical Architect

## Context

The HMPPS strategy for decommissioning NOMIS by decomposing it into a set of microservices relies heavily on a sync capability provided by SYScon. The SYScon migrate and sync service suite coordinates the processes that keeps data held in DPS and NOMIS in sync. These services are event driven and can subscribe to events being published from DPS and/or NOMIS to drive synchronisation calls.

Each new service that is developed requires a decision on sync. Theoretically a service can choose not to sync any data however in practice, all NOMIS replacement services so far have adopted at least a one-way sync from DPS to NOMIS to support legacy clients and existing reports. This is usually supported by an initial migration of existing data into the new DPS service.

Developing the synchronisation support for a new service is not free. The mappings between the new services need defining, relevant events identifying or adding and endpoints to support sync creating in both the new service and (usually) the Prison API. This then needs thorough testing before considering deploying to production. This is a non-trivial amount of work and can be particularly complex if the data model of the two services differ significantly or there is any form of consolidating or flattening logic when syncing the data. It should be noted however that **SYScon themselves estimate that a two-way sync is only 10% more effort than a one-way sync**.

The HMPPS Technical Architect community has a preference for adopting one-way sync from DPS to NOMIS. This motivates teams to extract all business logic from NOMIS and simplifies the path to eventually decommissioning the NOMIS functionality and data completely. 

## Decision

We propose adopting a two-way sync between DPS and NOMIS during service roll out then moving to a one-way sync once the service has been released to all prisons.

In collaboration with SYScon, we will:

- Develop a migration strategy including the flattening decision covered in [ADR001: Associating Alerts at the Person Level](architectural_design_record/001-person-level-association.md)
- Support alerts created and updated via the new service by syncing the changes back into NOMIS
- Support alerts created and updated via NOMIS by syncing the changes from NOMIS to the new service
- Document and plan all work needed to move to a one-way sync for example the end goal for prisoner merge events in [ADR003: Prisoner and Booking Merge Events](architectural_design_record/003-prisoner-merge.md)

## Consequences

- Two-way sync provides maximum flexibility for rollout strategy and maximum safety for rollback
  - The DPS functionality can be switched to use the new API independently of switching the NOMIS screens off
  - Both sets of switches can be toggled on and off at any time without service disruption
- Two-way sync supports decoupling the data migration from the roll-out itself
  - The full alerts dataset can be migrated and kept in sync prior to toggling any of the feature switches
  - This synced dataset can be checked for accuracy over a number of weeks to gain confidence that sync is working as intended
- Full prison estate roll-out following a smaller private beta becomes significantly more simple as the alerts data has already been migrated into the new service and kept in sync since

Following full prison estate roll out, a plan will be put in place to move to a one way sync. This will include a proposal covering the NOMIS process that copies alerts from previous bookings, prisoner and booking merge events and any NOMIS process that generate alerts.
