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
        '00000000-0000-0000-0000-000000000004',
        (SELECT alert_code_id FROM alert_code WHERE code = 'ADSC'),
        'C3456CC',
        'Inactive alert type ''A'' - ''Social Care'' code ''ADSC'' - ''Adult Social Care'' alert for prison number ''C3456CC'' active from tomorrow with no active to date',
        'B. Approver',
        now() + interval '1 day',
        null,
        now() - interval '3 days',
        null,
        null
    ),
    (
        '00000000-0000-0000-0000-000000000005',
        (SELECT alert_code_id FROM alert_code WHERE code = 'DOCGM'),
        'A1234AA',
        'Active alert type ''D'' - ''Security. Do not share with offender'' code ''DOCGM'' - ''OCG Nominal - Do not share'' alert for prison number ''A1234AA'' active from today with no active to date',
        'External Provider',
        now(),
        null,
        now() - interval '2 day',
        now(),
        null
    );