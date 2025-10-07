CREATE TABLE alert_code_privileged_user
(
    alert_code_id  bigserial    NOT NULL REFERENCES alert_code (alert_code_id),
    username       varchar(64)  NOT NULL,
    PRIMARY KEY (alert_code_id, username)
);

CREATE INDEX idx_alert_code_privileged_user_alert_code_id ON alert_code_privileged_user (alert_code_id);
CREATE INDEX idx_alert_code_privileged_user_username ON alert_code_privileged_user (username);

COMMENT ON TABLE alert_code_privileged_user IS 'Data for restricting certain alerts to be administered only by named users';
COMMENT ON COLUMN alert_code_privileged_user.alert_code_id IS 'Foreign key to alert code';
COMMENT ON COLUMN alert_code_privileged_user.username IS 'The username of the person who can administer the alert';

ALTER TABLE alert_code
    ADD COLUMN restricted boolean NOT NULL DEFAULT false;

COMMENT ON COLUMN alert_code.restricted IS 'Can the alert code be administered by all users (false) or named users only (true)?'