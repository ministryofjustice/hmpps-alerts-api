CREATE INDEX IF NOT EXISTS idx_alert_prison_number_ordered
    ON alert (prison_number, active_from DESC, created_at DESC)
    WHERE deleted_at IS NULL;
