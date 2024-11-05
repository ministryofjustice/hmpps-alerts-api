do
$$
    begin
        if exists(select 1
                  from information_schema.columns
                  where table_name = 'alert'
                    and column_name = 'alert_id')
        then
            alter table audit_event
                add column if not exists alert_uuid uuid;

            create index if not exists idx_audit_event_alert_uuid on audit_event (alert_uuid);

            create unique index if not exists unq_alert_alert_uuid on alert(alert_uuid);

            update audit_event ae
            set alert_uuid = a.alert_uuid
            from alert a
            where a.alert_id = ae.alert_id
              and ae.alert_uuid is null;

            alter table audit_event
                alter column alert_uuid set not null,
                drop column if exists alert_id;

            alter table audit_event
                rename column alert_uuid to alert_id;

            alter table alert
                drop column if exists alert_id;
        end if;
    end
$$;

drop sequence if exists audit_event_alert_id_seq;

do
$$
    begin
        if exists(select 1
                  from information_schema.columns
                  where table_name = 'alert'
                    and column_name = 'alert_uuid')
        then
            alter table alert
                rename column alert_uuid to id;
        end if;
    end
$$;

do
$$
    begin
        if not exists(select 1
                      from pg_constraint
                      where conname = 'alert_pk')
        then
            alter table alert
                add constraint alert_pk primary key using index unq_alert_alert_uuid;
        end if;
    end
$$;

drop index if exists idx_alert_alert_uuid;

do
$$
    begin
        if not exists(select 1
                      from pg_constraint
                      where conname = 'alert_id_fk')
        then
            alter table audit_event
                add constraint alert_id_fk foreign key (alert_id) references alert (id);
        end if;
    end
$$;

