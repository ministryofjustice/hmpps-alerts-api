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