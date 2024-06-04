#
# This script is used to run the Alerts API locally, to interact with
# existing PostgreSQL and localstack containers.
#
# It runs with a combination of properties from the default spring profile (in application.yaml) and supplemented
# with the -local profile (from application-local.yml). The latter overrides some of the defaults.
#
# The environment variables here will also override values supplied in spring profile properties, specifically
# around removing the SSL connection to the database and setting the DB properties, SERVER_PORT and client credentials
# to match those used in the docker-compose files.
#

# Provide the DB connection details to local container-hosted Postgresql DB
# Match with the credentials set in docker-compose.yml
export DB_SERVER=localhost:5433
export DB_NAME=alerts
export DB_USER=alerts
export DB_PASS=alerts
export DB_SSL_MODE=prefer

# AWS configuration
export AWS_REGION=eu-west-2

# Client credentials from environment variables
export MANAGE_USERS_CLIENT_ID="$HMPPS_ALERTS_MANAGE_USERS_CLIENT_ID"
export MANAGE_USERS_CLIENT_SECRET="$HMPPS_ALERTS_MANAGE_USERS_CLIENT_SECRET"
export PRISONER_SEARCH_CLIENT_ID="$HMPPS_ALERTS_PRISONER_SEARCH_CLIENT_ID"
export PRISONER_SEARCH_CLIENT_SECRET="$HMPPS_ALERTS_PRISONER_SEARCH_CLIENT_SECRET"

# Provide URLs to other dependent services. Dev services used here (can be local if you set up the dependent services locally)
export API_BASE_URL_HMPPS_AUTH=https://sign-in-dev.hmpps.service.justice.gov.uk/auth
export API_BASE_URL_MANAGE_USERS=https://manage-users-api-dev.hmpps.service.justice.gov.uk
export API_BASE_URL_PRISONER_SEARCH=https://prisoner-search-dev.prison.service.justice.gov.uk

# Run the application with stdout and local profiles active
SPRING_PROFILES_ACTIVE=stdout,local ./gradlew bootRun

# End
