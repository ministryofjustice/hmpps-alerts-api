drop table nomis_alert;
drop table verifications;
drop table comments;
drop table audit_events;
drop table alerts;

-- Creating only to support transition from NOMIS alerts to alerts
CREATE TABLE nomis_alert
(
    nomis_alert_id              bigserial       NOT NULL CONSTRAINT nomis_alert_pk PRIMARY KEY,
    offender_book_id            bigint          NOT NULL,
    alert_seq                   int             NOT NULL,
    alert_uuid                  uuid            NOT NULL,
    nomis_alert_data            jsonb           NOT NULL,
    upserted_at                 timestamp       NOT NULL,
    removed_at                  timestamp
);

-- Agreed audit tables
create table alert
(
    alert_id                    bigserial       NOT NULL CONSTRAINT alert_pk PRIMARY KEY,
    alert_uuid                  uuid            NOT NULL,
    alert_code_id               bigserial       NOT NULL CONSTRAINT alert_alert_code_fk REFERENCES alert_code(alert_code_id),
    prison_number               varchar(10)     NOT NULL,
    description                 text,
    authorised_by               varchar(40),
    active_from                 date            NOT NULL,
    active_to                   date,
    deleted_at                  timestamp
);

CREATE INDEX idx_alert_alert_uuid ON alert (alert_uuid);
CREATE INDEX idx_alert_alert_code ON alert (alert_code_id);
CREATE INDEX idx_alert_prison_number ON alert (prison_number);
CREATE INDEX idx_alert_deleted_at ON alert (deleted_at);

create table comment
(
    comment_id                  bigserial       NOT NULL CONSTRAINT comment_pk PRIMARY KEY,
    comment_uuid                uuid            NOT NULL,
    alert_id                    bigserial       NOT NULL CONSTRAINT comment_alert_fk REFERENCES alert(alert_id),
    comment                     text            NOT NULL,
    created_at                  timestamp       NOT NULL,
    created_by                  varchar(32)     NOT NULL,
    created_by_display_name     varchar(255)    NOT NULL
);

CREATE INDEX idx_comment_comment_uuid ON comment (comment_uuid);
CREATE INDEX idx_comment_alert_id ON comment (alert_id);

create table audit_event
(
    audit_event_id              bigserial       NOT NULL CONSTRAINT audit_pk PRIMARY KEY,
    alert_id                    bigserial       NOT NULL CONSTRAINT audit_event_alert_fk REFERENCES alert(alert_id),
    action                      varchar(40)     NOT NULL,
    description                 text            NOT NULL,
    actioned_at                 timestamp       NOT NULL,
    actioned_by                 varchar(32)     NOT NULL,
    actioned_by_display_name    varchar(255)    NOT NULL
);

CREATE INDEX idx_audit_event_alert_id ON audit_event (alert_id);
CREATE INDEX idx_audit_event_action ON audit_event (action);
