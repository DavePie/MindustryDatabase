-- Database creation used during deployment.

CREATE TABLE IF NOT EXISTS account(

    id            SERIAL       PRIMARY KEY,
    username      VARCHAR(15)  NOT NULL UNIQUE, -- Anything bigger than 15 is quite long.
    display_name  VARCHAR(255) NOT NULL,
    -- Variable length hash, not fixed length since the hash can get very long by changing the settings, and I want to allow that for future proofing.
    password      BYTEA        NOT NULL, -- Argon2id
    creation_date TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,

    -- I use the discord username validation since they fit our use cases.
    CONSTRAINT chk_username_valid CHECK (
            LENGTH(username) > 2 AND
            username  ~ '^[a-z0-9_.]+$' AND
            username !~ '\\.\\.'
    )
);

-- TODO Server authorization based on server_whitelist.
CREATE TABLE IF NOT EXISTS server(

    id               SERIAL       PRIMARY KEY,
    ip_address       INET         NOT NULL,
    port             INT          NOT NULL,
    name             VARCHAR(255) NOT NULL,
    -- Last heartbeat available used in case the server goes offline.
    heartbeat        TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    -- The period of the heartbeat in milliseconds.
    heartbeat_period INT          NOT NULL DEFAULT 5000,

    CONSTRAINT u_server_ip_port UNIQUE(ip_address, port),
    CONSTRAINT chk_port_valid CHECK (port >= 0 AND port <= 65535)
);

CREATE TABLE IF NOT EXISTS login(

    id         SERIAL      PRIMARY KEY,
    account_id INT         NOT NULL,
    ip_address INET        NOT NULL,
    login_date TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_login_user FOREIGN KEY(account_id) REFERENCES account(id)
);

CREATE TABLE IF NOT EXISTS account_session(

    id              SERIAL      PRIMARY KEY,
    account_id      INT         NOT NULL UNIQUE, -- There can be only one session at a time.
    session_cookie  BYTEA       NOT NULL UNIQUE, -- Sha256
    expiration_date TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_account_session FOREIGN KEY(account_id) REFERENCES account(id),
    CONSTRAINT chk_session_cookie_length CHECK (LENGTH(session_cookie) = 32)
);

CREATE TABLE IF NOT EXISTS server_join(

    id         SERIAL      PRIMARY KEY,
    account_id INT         NOT NULL,
    server_id  INT         NOT NULL,
    join_date  TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    leave_date TIMESTAMPTZ NULL     DEFAULT NULL,

    CONSTRAINT fk_server_join_user   FOREIGN KEY(account_id) REFERENCES account(id),
    CONSTRAINT fk_server_join_server FOREIGN KEY(server_id)  REFERENCES server(id)
);

CREATE TABLE IF NOT EXISTS report(

    id            SERIAL       PRIMARY KEY,
    account_id    INT          NOT NULL,
    reported_id   INT          NOT NULL,
    short_reason  VARCHAR(255) NOT NULL,
    long_reason   TEXT         NOT NULL DEFAULT '',
    creation_date TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_report_account  FOREIGN KEY(account_id)  REFERENCES account(id),
    CONSTRAINT fk_report_reported FOREIGN KEY(reported_id) REFERENCES account(id)
);

CREATE TABLE IF NOT EXISTS report_reply(

    id            SERIAL      PRIMARY KEY,
    report_id     INT         NOT NULL UNIQUE,
    staff_id      INT         NOT NULL,
    accepted      BOOLEAN     NOT NULL,
    message       TEXT        NOT NULL DEFAULT '',
    creation_date TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_report_reply_staff  FOREIGN KEY(staff_id)  REFERENCES account(id),
    CONSTRAINT fk_report_reply_report FOREIGN KEY(report_id) REFERENCES report(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS ban(

    id              SERIAL      PRIMARY KEY,
    -- Random value used to search for this ban.
    uuid            BIGINT      NOT NULL UNIQUE,
    account_id      INT         NOT NULL,
    staff_id        INT         NOT NULL,
    server_id       INT         NOT NULL,
    reason          TEXT        NOT NULL,
    handled         BOOLEAN     NOT NULL DEFAULT FALSE, -- False if it needs to be handled by the server, true if it has been handled.
    creation_date   TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    -- Null for bans that are permanent.
    expiration_date TIMESTAMPTZ NULL DEFAULT NULL,

    CONSTRAINT fk_ban_user   FOREIGN KEY(account_id) REFERENCES account(id),
    CONSTRAINT fk_ban_staff  FOREIGN KEY(staff_id)   REFERENCES account(id),
    CONSTRAINT fk_ban_server FOREIGN KEY(server_id)  REFERENCES server(id)
);

CREATE TABLE IF NOT EXISTS unban(

    id            SERIAL      PRIMARY KEY,
    ban_id        INT         NOT NULL UNIQUE,
    staff_id      INT         NOT NULL,
    creation_date TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_unban_ban   FOREIGN KEY(ban_id)   REFERENCES ban(id),
    CONSTRAINT fk_unban_staff FOREIGN KEY(staff_id) REFERENCES account(id)
);

CREATE TABLE IF NOT EXISTS kick(

    id            SERIAL      PRIMARY KEY,
    account_id    INT         NOT NULL,
    staff_id      INT         NOT NULL,
    server_id     INT         NOT NULL,
    reason        TEXT        NOT NULL,
    handled       BOOLEAN     NOT NULL DEFAULT FALSE, -- False if it needs to be handled by the server, true if it has been handled.
    creation_date TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_kick_user   FOREIGN KEY(account_id) REFERENCES account(id),
    CONSTRAINT fk_kick_staff  FOREIGN KEY(staff_id)   REFERENCES account(id),
    CONSTRAINT fk_kick_server FOREIGN KEY(server_id)  REFERENCES server(id)
);

CREATE TABLE IF NOT EXISTS warn(

    id            SERIAL      PRIMARY KEY,
    account_id    INT         NOT NULL,
    staff_id      INT         NOT NULL,
    server_id     INT         NOT NULL,
    reason        TEXT        NOT NULL,
    handled       BOOLEAN     NOT NULL DEFAULT FALSE, -- False if it needs to be handled by the server, true if it has been handled.
    creation_date TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_warn_user   FOREIGN KEY(account_id) REFERENCES account(id),
    CONSTRAINT fk_warn_staff  FOREIGN KEY(staff_id)   REFERENCES account(id),
    CONSTRAINT fk_warn_server FOREIGN KEY(server_id)  REFERENCES server(id)
);

CREATE TABLE IF NOT EXISTS mute(

    id              SERIAL      PRIMARY KEY,
    account_id      INT         NOT NULL,
    staff_id        INT         NOT NULL,
    server_id       INT         NOT NULL,
    reason          TEXT        NOT NULL,
    creation_date   TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    expiration_date TIMESTAMPTZ NOT NULL,

    CONSTRAINT fk_mute_user   FOREIGN KEY(account_id) REFERENCES account(id),
    CONSTRAINT fk_mute_staff  FOREIGN KEY(staff_id)   REFERENCES account(id),
    CONSTRAINT fk_mute_server FOREIGN KEY(server_id)  REFERENCES server(id)
);

CREATE TABLE IF NOT EXISTS ban_appeal(

    id            SERIAL      PRIMARY KEY,
    account_id    INT         NOT NULL,
    ban_id        INT         NOT NULL,
    message       TEXT        NOT NULL,
    creation_date TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_ban_appeal_account FOREIGN KEY(account_id) REFERENCES account(id),
    CONSTRAINT fk_ban_appeal_ban     FOREIGN KEY(ban_id)     REFERENCES ban(id)
);
-- TODO Automatically insert unban if accepted is true?
CREATE TABLE IF NOT EXISTS ban_appeal_reply(

    id            SERIAL      PRIMARY KEY,
    ban_appeal_id INT         NOT NULL,
    staff_id      INT         NOT NULL,
    accepted      BOOLEAN     NOT NULL,
    message       TEXT        NOT NULL DEFAULT '',
    creation_date TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_ban_appeal_reply_ban   FOREIGN KEY(ban_appeal_id) REFERENCES ban_appeal(id),
    CONSTRAINT fk_ban_appeal_reply_staff FOREIGN KEY(staff_id)      REFERENCES account(id)
);

CREATE TABLE IF NOT EXISTS ip_blacklist(

    id            SERIAL      PRIMARY KEY,
    staff_id      INT         NOT NULL,
    ip_address    INET        NOT NULL UNIQUE,
    reason        TEXT        NOT NULL DEFAULT '',
    creation_date TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_ip_blacklist_staff FOREIGN KEY(staff_id) REFERENCES account(id)
);

CREATE TABLE IF NOT EXISTS role(

    id       SERIAL       PRIMARY KEY,
    name     VARCHAR(255) NOT NULL UNIQUE,
    -- The order of which the roles are displayed, smallest first.
    priority SMALLINT     NOT NULL,
    symbol   VARCHAR(16)  NOT NULL,
    color    CHAR(8)      NOT NULL
);

CREATE TABLE IF NOT EXISTS permission(

    id       SERIAL       PRIMARY KEY,
    -- Lowercase with - as space separators.
    property VARCHAR(255) NOT NULL UNIQUE
);

CREATE TABLE IF NOT EXISTS account_role(

    id         SERIAL PRIMARY KEY,
    account_id INT    NOT NULL,
    role_id    INT    NOT NULL,

    CONSTRAINT fk_roles_user FOREIGN KEY(account_id) REFERENCES account(id) ON DELETE CASCADE,
    CONSTRAINT fk_roles_role FOREIGN KEY(role_id)    REFERENCES role(id)    ON DELETE CASCADE,
    CONSTRAINT u_account_role UNIQUE(account_id, role_id)
);

CREATE TABLE IF NOT EXISTS role_permission(

    id            SERIAL PRIMARY KEY,
    role_id       INT    NOT NULL,
    permission_id INT    NOT NULL,

    CONSTRAINT fk_role       FOREIGN KEY(role_id)       REFERENCES role(id)       ON DELETE CASCADE,
    CONSTRAINT fk_permission FOREIGN KEY(permission_id) REFERENCES permission(id) ON DELETE CASCADE,
    CONSTRAINT u_role_permission UNIQUE(role_id, permission_id)
);

-- Insert Notify sender
CREATE OR REPLACE FUNCTION notify_on_insert() RETURNS trigger AS $$
DECLARE
    channel_name text := 'channel_insert_' || TG_TABLE_NAME;
BEGIN
    PERFORM pg_notify(channel_name, row_to_json(NEW)::text);
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Insert triggers
CREATE TRIGGER notify_insert_ban AFTER INSERT ON ban
FOR EACH ROW EXECUTE FUNCTION notify_on_insert();

CREATE TRIGGER notify_insert_kick AFTER INSERT ON kick
FOR EACH ROW EXECUTE FUNCTION notify_on_insert();

CREATE TRIGGER notify_insert_warn AFTER INSERT ON warn
FOR EACH ROW EXECUTE FUNCTION notify_on_insert();
