INSERT INTO nomis_alert
(
    nomis_alert_id,
    offender_book_id,
    alert_seq,
    alert_uuid,
    nomis_alert_data,
    upserted_at,
    removed_at
)
VALUES
(
    1,
    3,
    1,
    'bf0da40f-5a8d-4630-94dd-a7412f007023',
    JSON '{}',
    NOW() - INTERVAL '1 DAYS',
    NULL
);
