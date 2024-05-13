alter table audit_event add column description_updated BOOLEAN;
alter table audit_event add column authorised_by_updated BOOLEAN;
alter table audit_event add column active_from_updated BOOLEAN;
alter table audit_event add column active_to_updated BOOLEAN;
alter table audit_event add column comment_appended BOOLEAN;