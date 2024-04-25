CREATE TABLE bulk_alert
(
    bulk_alert_id               bigserial       NOT NULL CONSTRAINT bulk_alert_pk PRIMARY KEY,
    bulk_alert_uuid             uuid            NOT NULL,
    request                     jsonb           NOT NULL,
    requested_at                timestamp       NOT NULL,
    requested_by                varchar(32)     NOT NULL,
    requested_by_display_name   varchar(255)    NOT NULL,
    completed_at                timestamp       NOT NULL,
    successful                  boolean         NOT NULL,
    messages                    jsonb           NOT NULL,
    alerts_created              jsonb           NOT NULL,
    alerts_updated              jsonb           NOT NULL,
    alerts_expired              jsonb           NOT NULL
);

CREATE UNIQUE INDEX idx_bulk_alert_bulk_alert_uuid ON bulk_alert (bulk_alert_uuid);
