alter table bulk_plan
    alter column description type text;

alter table alert_code
    alter column created_by type varchar(64),
    alter column modified_by type varchar(64),
    alter column deactivated_by type varchar(64);

alter table alert_type
    alter column created_by type varchar(64),
    alter column modified_by type varchar(64),
    alter column deactivated_by type varchar(64);

alter table audit_event
    alter column actioned_by type varchar(64);

alter table resync_audit
    alter column requested_by type varchar(64);