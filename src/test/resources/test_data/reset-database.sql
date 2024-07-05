TRUNCATE TABLE alert RESTART IDENTITY CASCADE;
DELETE FROM alert_code WHERE created_at > NOW() - interval '1 hour';
DELETE FROM alert_type WHERE created_at > NOW() - interval '1 hour';
TRUNCATE TABLE resync_audit;