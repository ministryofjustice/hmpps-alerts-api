create table if not exists resync_audit(
    id bigserial NOT NULL CONSTRAINT pk_resync_audit PRIMARY KEY,
    request jsonb not null,
    requested_at timestamp not null,
    requested_by varchar(32) not null,
    requested_by_display_name varchar(255) not null,
    source varchar(12) not null,
    completed_at timestamp not null,
    alerts_deleted uuid[] not null,
    alerts_created uuid[] not null
);