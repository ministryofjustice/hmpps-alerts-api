create index if not exists idx_audit_event_actioned_by on audit_event(actioned_by, actioned_at);