alter table alert add column created_at timestamp;
alter table alert add column last_modified_at timestamp;

create index idx_audit_event_actioned_at on audit_event (actioned_at);

update alert a set created_at = (select actioned_at from audit_event ae where ae.alert_id = a.alert_id and ae.action = 'CREATED' limit 1);
update alert a set last_modified_at = (select actioned_at from audit_event ae where ae.alert_id = a.alert_id and ae.action != 'CREATED' order by actioned_at desc limit 1);

alter table alert alter column created_at set not null;

create index idx_alert_created_at on alert (created_at);
create index idx_alert_last_modified_at on alert (last_modified_at);
