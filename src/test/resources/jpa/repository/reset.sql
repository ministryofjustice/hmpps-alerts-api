DELETE FROM audit_event;
DELETE FROM plan_person;
DELETE FROM alert;
DELETE FROM bulk_plan;
DELETE FROM person_summary;
DELETE FROM alert_code WHERE alert_code_id >= 900000;
