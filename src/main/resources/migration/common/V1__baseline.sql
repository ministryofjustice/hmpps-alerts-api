CREATE TABLE nomis_alert
(
    nomis_alert_id      bigserial   NOT NULL CONSTRAINT nomis_alert_pk PRIMARY KEY,
    offender_book_id    bigint      NOT NULL,
    alert_seq           int         NOT NULL,
    alert_uuid          uuid        NOT NULL,
    nomis_alert_data    jsonb       NOT NULL,
    upserted_at         timestamp   NOT NULL,
    removed_at          timestamp
);

CREATE UNIQUE INDEX idx_nomis_alert_composite_id ON nomis_alert (offender_book_id, alert_seq);
CREATE INDEX idx_nomis_alert_alert_uuid ON nomis_alert (alert_uuid);
CREATE INDEX idx_nomis_alert_nomis_alert_data ON nomis_alert (nomis_alert_data);
CREATE INDEX idx_nomis_alert_upserted_at ON nomis_alert (upserted_at);
CREATE INDEX idx_nomis_alert_removed_at ON nomis_alert (removed_at);
