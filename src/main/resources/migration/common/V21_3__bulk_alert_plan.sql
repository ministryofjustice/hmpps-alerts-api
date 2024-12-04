alter table bulk_plan
    add column started_at              timestamp,
    add column started_by              varchar(64),
    add column started_by_display_name varchar(255),
    add column completed_at            timestamp,
    add column created_count           int,
    add column updated_count           int,
    add column unchanged_count         int,
    add column expired_count           int;
