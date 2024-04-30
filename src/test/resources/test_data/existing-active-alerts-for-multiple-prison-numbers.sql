insert into alert
(
    alert_uuid,
    alert_code_id,
    prison_number,
    description,
    authorised_by,
    active_from,
    active_to,
    created_at,
    last_modified_at,
    deleted_at
)
values
(
    gen_random_uuid(),
    (SELECT alert_code_id FROM alert_code WHERE code = 'ADSC'),
    'B2345BB',
    'Active alert type ''A'' - ''Social Care'' code ''ADSC'' - ''Adult Social Care'' alert for prison number ''B2345BB'' active from yesterday with no active to date',
    'A. Approver',
    now() - interval '1 day',
    null,
    now() - interval '1 day',
    null,
    null
),
(
    gen_random_uuid(),
    (SELECT alert_code_id FROM alert_code WHERE code = 'DOCGM'),
    'B2345BB',
    'Active alert type ''D'' - ''Security. Do not share with offender'' code ''DOCGM'' - ''OCG Nominal - Do not share'' alert for prison number ''B2345BB'' active from today with no active to date',
    'External Provider',
    now(),
    null,
    now() - interval '2 day',
    now(),
    null
),
(
    gen_random_uuid(),
    (SELECT alert_code_id FROM alert_code WHERE code = 'DOCGM'),
    'C3456CC',
    'Inactive alert type ''D'' - ''Security. Do not share with offender'' code ''DOCGM'' - ''OCG Nominal - Do not share'' alert for prison number ''C3456CC'' active from tomorrow with no active to date',
    'B. Approver',
    now() + interval '1 day',
    null,
    now() - interval '3 days',
    null,
    null
);

insert into audit_event
(
    alert_id,
    action,
    description,
    actioned_at,
    actioned_by,
    actioned_by_display_name,
    source
)
values
(
    1,
    'CREATED',
    'Alert created',
    now() - interval '1 day',
    'TEST_USER',
    'Test User',
    'DPS'
),
(
    2,
    'CREATED',
    'Alert created',
    now() - interval '2 days',
    'TEST_USER',
    'Test User',
    'DPS'
),
(
    3,
    'CREATED',
    'Alert created',
    now() - interval '3 days',
    'TEST_USER',
    'Test User',
    'DPS'
);
