update bulk_plan
set cleanup_mode = null
where cleanup_mode is not null;

alter table bulk_plan
    add constraint ch_bulk_plan_cleanup_mode check ( cleanup_mode in ('KEEP_ALL', 'EXPIRE_FOR_PRISON_NUMBERS_NOT_SPECIFIED') );