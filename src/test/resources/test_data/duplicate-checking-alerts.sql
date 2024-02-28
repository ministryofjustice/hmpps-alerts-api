insert into alert
(
    alert_uuid,
    alert_code_id,
    prison_number,
    description,
    authorised_by,
    active_from,
    active_to,
    deleted_at
)
values
(
    gen_random_uuid(),
    (SELECT alert_code_id FROM alert_code WHERE code = 'HID'),
    'A1234AA',
    'Active Hidden Disability alert active from today with no active to date. Alert code is active',
    null,
    now(),
    null,
    null
),
(
    gen_random_uuid(),
    (SELECT alert_code_id FROM alert_code WHERE code = 'URS'),
    'A1234AA',
    'Active Refusing to shield alert active from today with no active to date. Alert code is inactive',
    null,
    now(),
    null,
    null
),
(
    gen_random_uuid(),
    (SELECT alert_code_id FROM alert_code WHERE code = 'AS'),
    'A1234AA',
    'Inactive Social Care alert active from tomorrow with no active to date. Alert code is active',
    null,
    now() + interval '1 day',
    null,
    null
),
(
    gen_random_uuid(),
    (SELECT alert_code_id FROM alert_code WHERE code = 'VI'),
    'A1234AA',
    'Inactive Victim alert active from yesterday to today. Alert code is active',
    null,
    now() - interval '1 day',
    now(),
    null
),
(
    gen_random_uuid(),
    (SELECT alert_code_id FROM alert_code WHERE code = 'ORFW'),
    'A1234AA',
    'Deleted active Ready For Work alert which would have been active from today with no active to date. Alert code is active',
    null,
    now(),
    now(),
    null
);