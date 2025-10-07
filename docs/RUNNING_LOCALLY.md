# Running the service locally using run-local.sh
This will run the service locally. It starts the database and localstack containers then start the service via a bash script.
It connects to the dev version of hmpps-auth.

## Environment variables

The script expects the following environment variables to be set:

```
HMPPS_ALERTS_CLIENT_ID
HMPPS_ALERTS_CLIENT_SECRET
HMPPS_ALERTS_MANAGE_USERS_CLIENT_ID
HMPPS_ALERTS_MANAGE_USERS_CLIENT_SECRET
HMPPS_ALERTS_PRISONER_SEARCH_CLIENT_ID
HMPPS_ALERTS_PRISONER_SEARCH_CLIENT_SECRET
```

These environment variables should be set to the dev secrets values. Remember to escape any `$` characters with `\$`.
It's easiest to set these in your `.bash_profile` or `.zprofile`

## Running the service locally

Run the following commands from the root directory of the project:

1. docker compose -f docker-compose-local.yml pull
2. docker compose -f docker-compose-local.yml up --no-start
3. docker compose -f docker-compose-local.yml start db localstack
4. ./run-local.sh
