alter table audit_event
    add column alert_uuid uuid;

drop index idx_audit_event_alert_id;

update audit_event ae
set alert_uuid = a.alert_uuid
from alert a
where a.alert_id = ae.alert_id;

alter table audit_event
    alter column alert_uuid set not null,
    drop column alert_id;

alter table audit_event
    rename column alert_uuid to alert_id;

create index idx_audit_event_alert_id on audit_event (alert_id);

drop sequence if exists audit_event_alert_id_seq;

drop index idx_alert_alert_uuid;

alter table alert
    drop column alert_id;

alter table alert
    rename column alert_uuid to id;

alter table alert
    add primary key (id);

alter table audit_event
    add constraint alert_id_fk foreign key (alert_id) references alert (id);