alter table audit_event
    drop constraint audit_pk,
    add column version int  not null             default 0,
    add column id      uuid not null primary key default gen_random_uuid();

alter table alert
    add column version int not null default 0;