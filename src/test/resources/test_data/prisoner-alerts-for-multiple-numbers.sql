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
    'A1234AA',
    'Active alert type ''A'' - ''Social Care'' code ''ADSC'' - ''Adult Social Care'' alert for prison number ''A1234AA'' active from yesterday with no active to date. Alert code is active. Created yesterday and not modified since',
    'A. Approver',
    now() - interval '1 day',
    null,
    now() - interval '1 day',
    null,
    null
),
(
    gen_random_uuid(),
    (SELECT alert_code_id FROM alert_code WHERE code = 'AS'),
    'B2345BB',
    'Active alert type ''A'' - ''Social Care'' code ''AS'' - ''Social Care'' alert for prison number ''B2345BB'' active from today with no active to date. Alert code is active. Created two days ago and modified yesterday and today',
    'External Provider',
    now(),
    null,
    now() - interval '2 day',
    now(),
    null
),
(
    gen_random_uuid(),
    (SELECT alert_code_id FROM alert_code WHERE code = 'URS'),
    'C3456CC',
    'Active alert type ''U'' - ''COVID unit management'' code ''URS'' - ''Refusing to shield'' alert for prison number ''C3456CC'' active from today with no active to date. Alert code is inactive. Created today and modified shortly after',
    null,
    now(),
    null,
    now() - interval '1 hour',
    now() - interval '1 minute',
    null
),
(
    gen_random_uuid(),
    (SELECT alert_code_id FROM alert_code WHERE code = 'MAS'),
    'A1234AA',
    'Inactive alert type ''M'' - ''Medical'' code ''MAS'' - ''Asthmatic'' alert for prison number ''A1234AA'' active from tomorrow with no active to date. Alert code is active. Created three days ago and not modified since',
    'B. Approver',
    now() + interval '1 day',
    null,
    now() - interval '3 days',
    null,
    null
),
(
    gen_random_uuid(),
    (SELECT alert_code_id FROM alert_code WHERE code = 'MEP'),
    'B2345BB',
    'Inactive alert type ''M'' - ''Medical'' code ''MEP'' - ''Epileptic'' alert for prison number ''B2345BB'' active from yesterday to today. Alert code is active. Created four days ago and modified yesterday',
    null,
    now() - interval '1 day',
    now(),
    now() - interval '4 days',
    now() - interval '1 day',
    null
),
(
    'a2c6af2c-9e70-4fd7-bac3-f3029cfad9b8',
    (SELECT alert_code_id FROM alert_code WHERE code = 'ORFW'),
    'A1234AA',
    'Deleted active alert type ''O'' - ''Other'' code ''ORFW'' - ''Ready For Work'' alert for prison number ''A1234AA'' which would have been active from today with no active to date. Alert code is active. Created today and not modified since',
    'C. Approver',
    now(),
    null,
    now(),
    now(),
    now()
),
(
    gen_random_uuid(),
    (SELECT alert_code_id FROM alert_code WHERE code = 'ADSC'),
    'B2345BB',
    'Active alert type ''A'' - ''Social Care'' code ''ADSC'' - ''Adult Social Care'' alert for prison number ''B2345BB'' active from yesterday with no active to date. Alert code is active. Created yesterday and not modified since',
    null,
    now() - interval '1 day',
    null,
    now() - interval '1 day',
    null,
    null
);
