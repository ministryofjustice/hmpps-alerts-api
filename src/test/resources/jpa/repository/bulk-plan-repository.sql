INSERT INTO bulk_plan (id, description, version, created_at, created_by, created_by_display_name)
VALUES
    ('00000000-0000-0000-0000-000000000201', 'Affect plan', 0, '2024-01-01 00:00:00', 'REPOSITORY_TEST', 'Repository Test'),
    ('00000000-0000-0000-0000-000000000202', 'Shared person plan', 0, '2024-01-01 00:00:00', 'REPOSITORY_TEST', 'Repository Test'),
    ('00000000-0000-0000-0000-000000000203', 'Unrelated plan', 0, '2024-01-01 00:00:00', 'REPOSITORY_TEST', 'Repository Test');

INSERT INTO person_summary (prison_number, first_name, last_name, status, restricted_patient, version)
VALUES
    ('A1111AA', 'Alice', 'Active', 'ACTIVE IN', false, 0),
    ('B2222BB', 'Bob', 'Create', 'ACTIVE IN', false, 0),
    ('C3333CC', 'Charlie', 'Update', 'ACTIVE IN', false, 0),
    ('Z9999ZZ', 'Zoe', 'Unrelated', 'ACTIVE IN', false, 0);

INSERT INTO plan_person (plan_id, prison_number)
VALUES
    ('00000000-0000-0000-0000-000000000201', 'A1111AA'),
    ('00000000-0000-0000-0000-000000000201', 'B2222BB'),
    ('00000000-0000-0000-0000-000000000201', 'C3333CC'),
    ('00000000-0000-0000-0000-000000000202', 'A1111AA'),
    ('00000000-0000-0000-0000-000000000203', 'Z9999ZZ');

INSERT INTO alert (id, alert_code_id, prison_number, active_from, active_to, created_at)
VALUES
    ('00000000-0000-0000-0000-000000000301', (SELECT alert_code_id FROM alert_code WHERE code = 'AS'), 'A1111AA', current_date - 1, NULL, current_date - 1),
    ('00000000-0000-0000-0000-000000000302', (SELECT alert_code_id FROM alert_code WHERE code = 'AS'), 'B2222BB', current_date - 1, current_date, current_date - 1),
    ('00000000-0000-0000-0000-000000000303', (SELECT alert_code_id FROM alert_code WHERE code = 'AS'), 'C3333CC', current_date - 1, current_date + 1, current_date - 1),
    ('00000000-0000-0000-0000-000000000304', (SELECT alert_code_id FROM alert_code WHERE code = 'AS'), 'D4444DD', current_date - 1, NULL, current_date - 1),
    ('00000000-0000-0000-0000-000000000305', (SELECT alert_code_id FROM alert_code WHERE code = 'AAR'), 'E5555EE', current_date - 1, NULL, current_date - 1);