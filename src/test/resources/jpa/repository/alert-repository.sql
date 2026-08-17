INSERT INTO alert (id, alert_code_id, prison_number, active_from, active_to, deleted_at, created_at)
VALUES
    ('00000000-0000-0000-0000-000000000101', (SELECT alert_code_id FROM alert_code WHERE code = 'AS'), 'A1234AA', current_date - 1, NULL, NULL, current_date - 1),
    ('00000000-0000-0000-0000-000000000102', (SELECT alert_code_id FROM alert_code WHERE code = 'AS'), 'A1234AA', current_date - 1, current_date + 1, NULL, current_date - 1),
    ('00000000-0000-0000-0000-000000000103', (SELECT alert_code_id FROM alert_code WHERE code = 'AS'), 'A1234AA', current_date - 1, current_date, NULL, current_date - 1),
    ('00000000-0000-0000-0000-000000000104', (SELECT alert_code_id FROM alert_code WHERE code = 'AS'), 'A1234AA', current_date - 2, current_date - 1, NULL, current_date - 2),
    ('00000000-0000-0000-0000-000000000105', (SELECT alert_code_id FROM alert_code WHERE code = 'AS'), 'B2345BB', current_date - 1, NULL, NULL, current_date - 1),
    ('00000000-0000-0000-0000-000000000106', (SELECT alert_code_id FROM alert_code WHERE code = 'AAR'), 'A1234AA', current_date - 1, NULL, NULL, current_date - 1),
    ('00000000-0000-0000-0000-000000000107', (SELECT alert_code_id FROM alert_code WHERE code = 'AS'), 'A1234AA', current_date - 1, NULL, current_date, current_date - 1);

INSERT INTO audit_event (alert_id, action, description, actioned_at, actioned_by, actioned_by_display_name, source)
VALUES
    ('00000000-0000-0000-0000-000000000101', 'CREATED', 'Created', '2024-01-10 10:00:00', 'REPOSITORY_TEST', 'Repository Test', 'DPS'),
    ('00000000-0000-0000-0000-000000000102', 'UPDATED', 'Updated', '2024-01-10 10:30:00', 'REPOSITORY_TEST', 'Repository Test', 'DPS'),
    ('00000000-0000-0000-0000-000000000103', 'INACTIVE', 'Inactive', '2024-01-10 11:00:00', 'REPOSITORY_TEST', 'Repository Test', 'DPS'),
    ('00000000-0000-0000-0000-000000000104', 'CREATED', 'Created', '2024-01-10 09:59:59', 'REPOSITORY_TEST', 'Repository Test', 'DPS'),
    ('00000000-0000-0000-0000-000000000105', 'INACTIVE', 'Inactive', '2024-01-10 10:30:00', 'REPOSITORY_TEST', 'Repository Test', 'DPS'),
    ('00000000-0000-0000-0000-000000000106', 'CREATED', 'Created', '2024-01-10 11:00:01', 'REPOSITORY_TEST', 'Repository Test', 'DPS'),
    ('00000000-0000-0000-0000-000000000107', 'DELETED', 'Deleted', '2024-01-10 10:30:00', 'REPOSITORY_TEST', 'Repository Test', 'DPS');