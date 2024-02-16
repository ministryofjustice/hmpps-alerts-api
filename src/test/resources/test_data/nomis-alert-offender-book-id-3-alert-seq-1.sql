INSERT INTO alerts
(
    alert_uuid,
    alert_type,
    alert_code,
    offender_id,
    authorised_by,
    valid_from
)
values
(
    'bf0da40f-5a8d-4630-94dd-a7412f007023',
    'ABC',
 'ABC',
 'A1234AB',
 'A. Authorizor',
 NOW() - INTERVAL '1 DAYS'
);

INSERT INTO nomis_alert
(
    nomis_alert_id,
    offender_book_id,
    alert_seq,
    alert_id,
    nomis_alert_data,
    upserted_at,
    removed_at
)
VALUES
(
    1,
    3,
    1,
    (select alert_id from alerts where offender_id = 'A1234AB' order by valid_from desc limit 1),
    JSON '{}',
    NOW() - INTERVAL '1 DAYS',
    NULL
);
