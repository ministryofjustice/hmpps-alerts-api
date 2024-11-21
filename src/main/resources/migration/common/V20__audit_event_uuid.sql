do
$$
    begin
        if not exists(select 1
                      from information_schema.columns
                      where table_name = 'audit_event'
                        and column_name = 'id')
        then
            alter table audit_event
                add column version int  not null default 0,
                add column id      uuid not null default gen_random_uuid();
        end if;
    end
$$;

do
$$
    begin
        if not exists(select 1
                      from information_schema.columns
                      where table_name = 'alert'
                        and column_name = 'version')
        then
            alter table alert
                add column version int not null default 0;
        end if;
    end
$$;

create unique index if not exists audit_event_pk on audit_event (id);

do
$$
    begin
        if exists(select 1
                      from information_schema.constraint_column_usage
                      where table_name = 'audit_event'
                        and constraint_name = 'audit_pk')
        then
            alter table audit_event
                drop constraint if exists audit_pk,
                add constraint audit_event_pk primary key using index audit_event_pk;
        end if;
    end
$$;


