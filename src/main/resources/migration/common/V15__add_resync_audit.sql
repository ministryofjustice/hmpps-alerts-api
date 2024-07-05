create table if not exists resync_audit
(
    id                        bigserial    not null
        constraint pk_resync_audit primary key,
    prison_number             varchar(10)  not null,
    request                   jsonb        not null,
    requested_at              timestamp    not null,
    requested_by              varchar(32)  not null,
    requested_by_display_name varchar(255) not null,
    source                    varchar(12)  not null,
    completed_at              timestamp    not null,
    alerts_deleted            uuid[]       not null,
    alerts_created            uuid[]       not null
);

CREATE INDEX idx_resync_audit_prison_number ON resync_audit (prison_number);
