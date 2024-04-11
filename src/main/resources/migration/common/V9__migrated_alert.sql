CREATE TABLE migrated_alert
(
    migrated_alert_id           bigserial       NOT NULL CONSTRAINT migrated_alert_pk PRIMARY KEY,
    offender_book_id            bigint          NOT NULL,
    booking_seq                 int             NOT NULL,
    alert_seq                   int             NOT NULL,
    alert_id                    bigserial       NOT NULL CONSTRAINT migrated_alert_alert_fk REFERENCES alert(alert_id),
    migrated_at                 timestamp       NOT NULL
);

CREATE UNIQUE INDEX idx_migrated_alert_composite_id ON migrated_alert (offender_book_id, alert_seq);
CREATE INDEX idx_migrated_alert_booking_seq ON migrated_alert (booking_seq);
CREATE INDEX idx_migrated_alert_id ON migrated_alert (alert_id);
