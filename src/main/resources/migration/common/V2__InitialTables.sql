create table alerts
(
    alert_id bigserial NOT NULL CONSTRAINT alerts_pk PRIMARY KEY,
    alert_uuid uuid not null,
    alert_type varchar(12) not null,
    alert_code varchar(12) not null,
    offender_id varchar(10) not null,
    authorised_by varchar(40) not null,
    valid_from timestamptz not null,
    valid_to timestamptz,
    removed_at timestamptz
);

CREATE INDEX idx_alerts_alert_uuid ON alerts (alert_uuid);
CREATE INDEX idx_alerts_offender_id ON alerts (offender_id);

alter table nomis_alert
drop column alert_uuid;

alter table nomis_alert
add column alert_id bigserial constraint nomis_alerts_alert_id_fk references alerts(alert_id);

create table comments
(
    comment_id bigserial not null constraint comment_pk primary key,
    comment text not null,
    alert_id bigserial not null constraint comments_alert_id_fk references alerts(alert_id),
    created_at timestamptz not null,
    created_by varchar(255)
);

create table audit_events
(
    audit_event_id bigserial not null constraint audit_pk primary key,
    alert_id bigserial not null constraint audit_events_alert_id_fk references alerts(alert_id),
    event_description text not null,
    created_at timestamptz not null,
    created_by varchar(255) not null,
    created_by_captured_name varchar(255) not null
);

create table verifications
(
    verification_id bigserial not null constraint verifications_pk primary key,
    alert_id bigserial not null constraint verifications_alert_id_fk references alerts(alert_id),
    action text not null,
    verified_by varchar(255) not null,
    verified_at timestamptz not null
);