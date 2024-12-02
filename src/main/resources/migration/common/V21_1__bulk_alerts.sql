alter table bulk_plan
    add column cleanup_mode varchar(64);

alter table plan_person
    add constraint fK_plan_id foreign key (plan_id) references bulk_plan (id),
    add constraint fK_prison_number foreign key (prison_number) references person_summary (prison_number);