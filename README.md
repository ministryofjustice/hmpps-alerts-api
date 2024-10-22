# HMPPS Alerts API
[![repo standards badge](https://img.shields.io/badge/dynamic/json?color=blue&style=flat&logo=github&label=MoJ%20Compliant&query=%24.result&url=https%3A%2F%2Foperations-engineering-reports.cloud-platform.service.justice.gov.uk%2Fapi%2Fv1%2Fcompliant_public_repositories%2Fhmpps-alerts-api)](https://operations-engineering-reports.cloud-platform.service.justice.gov.uk/public-github-repositories.html#hmpps-alerts-api "Link to report")
[![CircleCI](https://circleci.com/gh/ministryofjustice/hmpps-alerts-api/tree/main.svg?style=svg)](https://circleci.com/gh/ministryofjustice/hmpps-alerts-api)
[![codecov](https://codecov.io/github/ministryofjustice/hmpps-alerts-api/branch/main/graph/badge.svg)](https://codecov.io/github/ministryofjustice/hmpps-alerts-api)
[![Docker Repository on Quay](https://quay.io/repository/hmpps/hmpps-alerts-api/status "Docker Repository on Quay")](https://quay.io/repository/hmpps/hmpps-alerts-api)
[![API docs](https://img.shields.io/badge/API_docs_-view-85EA2D.svg?logo=swagger)](https://alerts-api-dev.hmpps.service.justice.gov.uk/swagger-ui/index.html)
[![Event docs](https://img.shields.io/badge/Event_docs-view-85EA2D.svg)](https://studio.asyncapi.com/?readOnly&url=https://raw.githubusercontent.com/ministryofjustice/hmpps-alerts-api/main/async-api.yml)


## Architectural Decision Records

For detailed insights into the architectural decisions made during the development of the Prisoner Alerts UI, refer to our ADRs:
- [ADR001: Associating Prisoner Alerts at the Person Level](architectural_design_record/001-person-level-association.md)
- [ADR002: Adopting a Two-Way Sync for Prisoner Alerts](architectural_design_record/002-two-way-sync.md)
- [ADR003: Prisoner and Booking Merge Events](architectural_design_record/003-prisoner-merge.md)
- [ADR004: Prisoner and booking changes in NOMIS triggers a prisoner alerts resync](architectural_design_record/004-prisoner-alerts-resync.md)
- [ADR005: Prisoner alerts data flattening implemented as a NOMIS data fix](architectural_design_record/005-alerts-flattening-data-fix.md)
- [ADR006: Prisoner alerts data extraction for Analytics Platform](architectural_design_record/006-analytics-platform.md)

## HMPPS Project Setup instructions

For more instructions and general hmpps project setup guidelines:
- [Running the service locally using run-local.sh](docs/RUNNING_LOCALLY.md).

## Load testing

For guidance in running the load test suite against local and deployed environments:
- [Running the load tests](docs/LOAD_TESTING.md)
