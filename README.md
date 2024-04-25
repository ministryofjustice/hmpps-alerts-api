# HMPPS Alerts API
[![repo standards badge](https://img.shields.io/badge/dynamic/json?color=blue&style=flat&logo=github&label=MoJ%20Compliant&query=%24.result&url=https%3A%2F%2Foperations-engineering-reports.cloud-platform.service.justice.gov.uk%2Fapi%2Fv1%2Fcompliant_public_repositories%2Fhmpps-alerts-api)](https://operations-engineering-reports.cloud-platform.service.justice.gov.uk/public-github-repositories.html#hmpps-alerts-api "Link to report")
[![CircleCI](https://circleci.com/gh/ministryofjustice/hmpps-alerts-api/tree/main.svg?style=svg)](https://circleci.com/gh/ministryofjustice/hmpps-alerts-api)
[![codecov](https://codecov.io/github/ministryofjustice/hmpps-alerts-api/branch/main/graph/badge.svg)](https://codecov.io/github/ministryofjustice/hmpps-alerts-api)
[![Docker Repository on Quay](https://quay.io/repository/hmpps/hmpps-alerts-api/status "Docker Repository on Quay")](https://quay.io/repository/hmpps/hmpps-alerts-api)
[![API docs](https://img.shields.io/badge/API_docs_-view-85EA2D.svg?logo=swagger)](https://hmpps-alerts-api-dev.hmpps.service.justice.gov.uk/webjars/swagger-ui/index.html?configUrl=/v3/api-docs)


## Architectural Decision Records

For detailed insights into the architectural decisions made during the development of the Alerts UI, refer to our ADRs:
- [ADR001: Associating Alerts at the Person Level](architectural_design_record/001-Person-Level-Association.md)


## HMPPS Project Setup instructions

For more instructions and general hmpps project setup guidelines:
- [Running the service locally using run-local.sh](docs/RUNNING_LOCALLY.md).

## Load testing

For guidance in running the load test suite against local and deployed environments:
- [Running the load tests](docs/LOAD_TESTING.md)
