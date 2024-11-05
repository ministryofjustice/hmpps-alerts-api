alter table alert_code
    alter column alert_type_id type bigint,
    alter column alert_type_id set not null;
drop sequence alert_code_alert_type_id_seq cascade;

alter table alert
    alter column alert_code_id type bigint,
    alter column alert_code_id set not null;
drop sequence alert_alert_code_id_seq cascade;

alter index idx_audit_event_alert_uuid rename to idx_audit_event_alert_id;