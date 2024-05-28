insert into alert
(
    alert_id,
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
    1,
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
    2,
    gen_random_uuid(),
    (SELECT alert_code_id FROM alert_code WHERE code = 'AS'),
    'A1234AA',
    'Active alert type ''A'' - ''Social Care'' code ''AS'' - ''Social Care'' alert for prison number ''A1234AA'' active from today with no active to date. Alert code is active. Created two days ago and modified yesterday and today',
    'External Provider',
    now(),
    null,
    now() - interval '2 day',
    now(),
    null
),
(
    3,
    gen_random_uuid(),
    (SELECT alert_code_id FROM alert_code WHERE code = 'URS'),
    'A1234AA',
    'Active alert type ''U'' - ''COVID unit management'' code ''URS'' - ''Refusing to shield'' alert for prison number ''A1234AA'' active from today with no active to date. Alert code is inactive. Created today and modified shortly after',
    null,
    now(),
    null,
    now() - interval '1 hour',
    now() - interval '1 minute',
    null
),
(
    4,
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
    5,
    gen_random_uuid(),
    (SELECT alert_code_id FROM alert_code WHERE code = 'MEP'),
    'A1234AA',
    'Inactive alert type ''M'' - ''Medical'' code ''MEP'' - ''Epileptic'' alert for prison number ''A1234AA'' active from yesterday to today. Alert code is active. Created four days ago and modified yesterday',
    null,
    now() - interval '1 day',
    now(),
    now() - interval '4 days',
    now() - interval '1 day',
    null
),
(
    6,
    '84856971-0072-40a9-ba5d-e994b0a9754f',
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
    7,
    gen_random_uuid(),
    (SELECT alert_code_id FROM alert_code WHERE code = 'ADSC'),
    'B2345BB',
    'Active alert type ''A'' - ''Social Care'' code ''ADSC'' - ''Adult Social Care'' alert for prison number ''B2345BB'' active from three days ago with active to from two days ago. Alert code is inactive. Created yesterday and not modified since',
    null,
    now() - interval '3 days',
    now() - interval '2 days',
    now() - interval '2 days',
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
    2,
    'UPDATED',
    'First alert update',
    now() - interval '1 day',
    'TEST_USER',
    'Test User',
    'DPS'
),
(
    2,
    'UPDATED',
    'Second alert update',
    now(),
    'TEST_USER',
    'Test User',
    'DPS'
),
(
    3,
    'CREATED',
    'Alert created',
    now() - interval '1 hour',
    'TEST_USER',
    'Test User',
    'DPS'
),
(
    3,
    'UPDATED',
    'Alert updated',
    now() - interval '1 minute',
    'TEST_USER',
    'Test User',
    'DPS'
),
(
    4,
    'CREATED',
    'Alert created',
    now() - interval '3 days',
    'TEST_USER',
    'Test User',
    'DPS'
),
(
    5,
    'CREATED',
    'Alert created',
    now() - interval '4 days',
    'TEST_USER',
    'Test User',
    'DPS'
),
(
    5,
    'UPDATED',
    'Alert updated',
    now() - interval '1 day',
    'TEST_USER',
    'Test User',
    'DPS'
),
(
    6,
    'CREATED',
    'Alert created',
    now(),
    'TEST_USER',
    'Test User',
    'DPS'
),
(
    6,
    'DELETED',
    'Alert deleted',
    now(),
    'TEST_USER',
    'Test User',
    'DPS'
),
(
    7,
    'CREATED',
    'Alert created',
    now() - interval '1 day',
    'TEST_USER',
    'Test User',
    'DPS'
);

insert into comment
(
    comment_uuid,
    alert_id,
    comment,
    created_at,
    created_by,
    created_by_display_name
)
values
(
    gen_random_uuid(),
    2,
    'First and only comment for alert 2. Search for ''Search Comment'' to find alert with this comment',
    now(),
    'TEST_USER',
    'Test User'
),
(
    gen_random_uuid(),
    5,
    'First comment for alert 5',
    now() - interval '3 hours',
    'TEST_USER',
    'Test User'
),
(
    gen_random_uuid(),
    5,
    'Second comment for alert 5. Search for ''Search Comment'' to find alert with this comment',
    now() - interval '2 hours',
    'TEST_USER',
    'Test User'
),
(
    gen_random_uuid(),
    5,
    'Third comment for alert 5',
    now() - interval '1 hour',
    'TEST_USER',
    'Test User'
),
(
    gen_random_uuid(),
    6,
    'Comment for deleted alert. Search for ''Search Comment'' to find alert with this comment',
    now(),
    'TEST_USER',
    'Test User'
);
