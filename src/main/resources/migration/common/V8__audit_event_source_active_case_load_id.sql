alter table audit_event add column source varchar(12);
alter table audit_event add column active_case_load_id varchar(6);

update audit_event ae set source = 'DPS';

alter table audit_event alter column source set not null;
