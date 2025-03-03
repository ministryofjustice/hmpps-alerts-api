COMMENT ON TABLE alert_type IS 'First level categorisation of alerts';
COMMENT ON TABLE alert_code IS 'Second level categorisation of alerts grouped by type';
COMMENT ON TABLE alert IS 'Individual alerts associated with a person identifier (prison number)';
COMMENT ON TABLE audit_event IS 'Detailed change history of alerts. Became the system of record for alert changes from 20th of June 2024';
COMMENT ON TABLE resync_audit IS 'Audit of alert changes caused by a resync call driven by a prisoner level change to alerts';
COMMENT ON TABLE bulk_plan IS 'Bulk alert journey session data';
COMMENT ON TABLE person_summary IS 'Data for the people who are included in a bulk alert journey';
COMMENT ON TABLE plan_person IS 'Joining table for people included in a bulk alert journey';

