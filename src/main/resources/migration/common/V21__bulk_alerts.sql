drop table bulk_alert cascade;

create table bulk_plan
(
    id                      uuid         not null primary key,
    alert_code_id           int,
    version                 int          not null,
    created_at              timestamp    not null,
    created_by              varchar(64)  not null,
    created_by_display_name varchar(255) not null
);

create table if not exists person_summary
(
    prison_number          varchar(10) primary key not null,
    first_name             varchar(64)             not null,
    last_name              varchar(64)             not null,
    status                 varchar(16)             not null,
    restricted_patient     boolean                 not null,
    prison_code            varchar(16),
    cell_location          varchar(64),
    supporting_prison_code varchar(16),
    version                int                     not null
);