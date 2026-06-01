-- Database creation used during deployment.

CREATE TABLE IF NOT EXISTS account(

    id            SERIAL       PRIMARY KEY,
    username      VARCHAR(15)  NOT NULL UNIQUE, -- Anything bigger than 15 is quite long.
    -- Variable length hash, not fixed length since the hash can get very long by changing the settings, and I want to allow that for future proofing.
    password      BYTEA        NOT NULL, -- Argon2id
    creation_date TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,

    -- I use the discord username validation since they fit our use cases.
    CONSTRAINT chk_account_username_valid CHECK (
        LENGTH(username) > 2 AND
        username  ~ '^[a-z0-9_.]+$' AND
        username !~ '\\.\\.'
    )
);

CREATE TABLE IF NOT EXISTS server(

    id                SERIAL       PRIMARY KEY,
    ip_address        INET         NOT NULL,
    port              INT          NOT NULL,
    name              VARCHAR(255) NOT NULL,
    whitelist_enabled BOOLEAN      NOT NULL DEFAULT FALSE,
    -- Last heartbeat available used in case the server goes offline.
    heartbeat         TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    -- The period of the heartbeat in milliseconds.
    heartbeat_period  INT          NOT NULL DEFAULT 5000,

    CONSTRAINT u_server_ip_port UNIQUE(ip_address, port),
    CONSTRAINT chk_server_port_valid CHECK (port >= 0 AND port <= 65535)
);

CREATE TABLE IF NOT EXISTS server_whitelist(

    id         SERIAL PRIMARY KEY,
    server_id  INT    NOT NULL,
    account_id INT    NOT NULL,

    CONSTRAINT u_server_whitelist_server_account UNIQUE(server_id, account_id),
    CONSTRAINT fk_server_whitelist_server  FOREIGN KEY(server_id)  REFERENCES server(id),
    CONSTRAINT fk_server_whitelist_account FOREIGN KEY(account_id) REFERENCES account(id)
);

CREATE TABLE IF NOT EXISTS login(

    id         SERIAL      PRIMARY KEY,
    account_id INT         NOT NULL,
    ip_address INET        NOT NULL,
    login_date TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_login_user FOREIGN KEY(account_id) REFERENCES account(id)
);
-- When creating a new account, to limit the amount of account that can be created, the whole table is checked for IP addresses.
-- So an Index will really improve performances in this case.
CREATE INDEX idx_login_ip_address ON login(ip_address);

CREATE TABLE IF NOT EXISTS account_session(

    id              SERIAL      PRIMARY KEY,
    account_id      INT         NOT NULL UNIQUE, -- There can be only one session at a time.
    session_cookie  BYTEA       NOT NULL UNIQUE, -- Sha256
    expiration_date TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_account_session FOREIGN KEY(account_id) REFERENCES account(id),
    CONSTRAINT chk_session_cookie_length CHECK (LENGTH(session_cookie) = 32)
);

CREATE TABLE IF NOT EXISTS online_account(

    id SERIAL    PRIMARY KEY,
    account_id   INT NOT NULL UNIQUE, -- the account can only play on one server at the time.
    server_id    INT NOT NULL, -- the server the account is online on.
    display_name VARCHAR(255) NOT NULL, -- the name the account is currently using.
    join_date    TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_online_account_account_id FOREIGN KEY(account_id) REFERENCES account(id),
    CONSTRAINT fk_online_account_server_id  FOREIGN KEY(server_id)  REFERENCES server (id)
);

CREATE TABLE IF NOT EXISTS server_account_history(

    id           SERIAL       PRIMARY KEY,
    display_name VARCHAR(255) NOT NULL,
    account_id   INT          NOT NULL,
    server_id    INT          NOT NULL,
    join_date    TIMESTAMPTZ  NOT NULL,
    leave_date   TIMESTAMPTZ  NOT NULL,

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

CREATE TYPE punishment_issuer_type AS ENUM ('account', 'server');

CREATE TYPE punishment_type AS ENUM ('ban', 'kick', 'warn', 'mute');

CREATE TABLE IF NOT EXISTS punishment_issuer(
    id              SERIAL                 PRIMARY KEY,
    type            punishment_issuer_type NOT NULL,
    account_id      INT                    NULL DEFAULT NULL,
    server_id       INT                    NULL DEFAULT NULL,

    CONSTRAINT fk_punishment_issuer_account FOREIGN KEY(account_id) REFERENCES account(id),
    CONSTRAINT fk_punishment_issuer_server  FOREIGN KEY(server_id)  REFERENCES server(id),
    CONSTRAINT chk_punishment_issuer_exclusive CHECK (
        (type = 'account' AND account_id IS NOT NULL AND server_id  IS NULL) OR
        (type = 'server'  AND server_id  IS NOT NULL AND account_id IS NULL)
    ),
    -- Unique to avoid generating duplicate data.
    CONSTRAINT u_punishment_issuer UNIQUE (type, account_id, server_id)
);

CREATE TABLE IF NOT EXISTS ban(

    id              SERIAL      PRIMARY KEY,
    account_id      INT         NOT NULL,
    issuer_id       INT         NOT NULL,
    server_id       INT         NOT NULL,
    reason          TEXT        NOT NULL,
    creation_date   TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    -- Null for bans that are permanent.
    expiration_date TIMESTAMPTZ NULL DEFAULT NULL,

    CONSTRAINT fk_ban_user   FOREIGN KEY(account_id) REFERENCES account(id),
    CONSTRAINT fk_ban_issuer FOREIGN KEY(issuer_id)  REFERENCES punishment_issuer(id),
    CONSTRAINT fk_ban_server FOREIGN KEY(server_id)  REFERENCES server(id),
    CONSTRAINT chk_ban_expiration_after_creation CHECK (
        expiration_date IS NULL OR creation_date <= expiration_date
    )
);

CREATE TABLE IF NOT EXISTS unban(

    id            SERIAL      PRIMARY KEY,
    ban_id        INT         NOT NULL UNIQUE,
    issuer_id     INT         NOT NULL,
    creation_date TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_unban_ban    FOREIGN KEY(ban_id)    REFERENCES ban(id),
    CONSTRAINT fk_unban_issuer FOREIGN KEY(issuer_id) REFERENCES punishment_issuer(id)
);

CREATE TABLE IF NOT EXISTS kick(

    id            SERIAL      PRIMARY KEY,
    account_id    INT         NOT NULL,
    issuer_id     INT         NOT NULL,
    server_id     INT         NOT NULL,
    reason        TEXT        NOT NULL,
    creation_date TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_kick_user   FOREIGN KEY(account_id) REFERENCES account(id),
    CONSTRAINT fk_kick_issuer FOREIGN KEY(issuer_id)  REFERENCES punishment_issuer(id),
    CONSTRAINT fk_kick_server FOREIGN KEY(server_id)  REFERENCES server(id)
);

CREATE TABLE IF NOT EXISTS warn(

    id            SERIAL      PRIMARY KEY,
    account_id    INT         NOT NULL,
    issuer_id     INT         NOT NULL,
    server_id     INT         NOT NULL,
    reason        TEXT        NOT NULL,
    seen          BOOLEAN     NOT NULL DEFAULT FALSE, -- False if it needs to be shown to the user.
    creation_date TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_warn_user   FOREIGN KEY(account_id) REFERENCES account(id),
    CONSTRAINT fk_warn_issuer FOREIGN KEY(issuer_id)  REFERENCES punishment_issuer(id),
    CONSTRAINT fk_warn_server FOREIGN KEY(server_id)  REFERENCES server(id)
);

CREATE TABLE IF NOT EXISTS mute(

    id              SERIAL      PRIMARY KEY,
    account_id      INT         NOT NULL,
    issuer_id       INT         NOT NULL,
    server_id       INT         NOT NULL,
    reason          TEXT        NOT NULL,
    creation_date   TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    expiration_date TIMESTAMPTZ NOT NULL,

    CONSTRAINT fk_mute_user   FOREIGN KEY(account_id) REFERENCES account(id),
    CONSTRAINT fk_mute_issuer FOREIGN KEY(issuer_id)  REFERENCES punishment_issuer(id),
    CONSTRAINT fk_mute_server FOREIGN KEY(server_id)  REFERENCES server(id),
    CONSTRAINT chk_mute_expiration_after_creation CHECK (
        expiration_date IS NULL OR creation_date <= expiration_date
    )
);

CREATE TABLE IF NOT EXISTS appeal(

    id            SERIAL      PRIMARY KEY,
    account_id    INT         NOT NULL,
    message       TEXT        NOT NULL,
    creation_date TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_appeal_account FOREIGN KEY(account_id) REFERENCES account(id)
);

CREATE TABLE IF NOT EXISTS appeal_reply(

    id            SERIAL      PRIMARY KEY,
    appeal_id     INT         NOT NULL,
    staff_id      INT         NOT NULL,
    accepted      BOOLEAN     NOT NULL,
    message       TEXT        NOT NULL DEFAULT '',
    creation_date TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_appeal_reply_appeal FOREIGN KEY(appeal_id) REFERENCES appeal(id),
    CONSTRAINT fk_appeal_reply_staff  FOREIGN KEY(staff_id)  REFERENCES account(id)
);

CREATE TABLE IF NOT EXISTS ip_blacklist(

    id            SERIAL      PRIMARY KEY,
    issuer_id     INT         NOT NULL,
    ip_address    INET        NOT NULL UNIQUE,
    reason        TEXT        NOT NULL DEFAULT '',
    creation_date TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_blacklist_issuer FOREIGN KEY(issuer_id) REFERENCES punishment_issuer(id)
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

    id         SERIAL      PRIMARY KEY,
    account_id INT         NOT NULL,
    role_id    INT         NOT NULL,
    grant_date TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_roles_user FOREIGN KEY(account_id) REFERENCES account(id) ON DELETE CASCADE,
    CONSTRAINT fk_roles_role FOREIGN KEY(role_id)    REFERENCES role(id)    ON DELETE CASCADE,
    CONSTRAINT u_account_role UNIQUE(account_id, role_id)
);

CREATE TABLE IF NOT EXISTS account_role_history(

    id          SERIAL      PRIMARY KEY,
    account_id  INT         NOT NULL,
    role_id     INT         NOT NULL,
    grant_date  TIMESTAMPTZ NOT NULL,
    revoke_date TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_roles_user FOREIGN KEY(account_id) REFERENCES account(id) ON DELETE CASCADE,
    CONSTRAINT fk_roles_role FOREIGN KEY(role_id)    REFERENCES role(id)    ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS role_permission(

    id            SERIAL PRIMARY KEY,
    role_id       INT    NOT NULL,
    permission_id INT    NOT NULL,

    CONSTRAINT fk_role       FOREIGN KEY(role_id)       REFERENCES role(id)       ON DELETE CASCADE,
    CONSTRAINT fk_permission FOREIGN KEY(permission_id) REFERENCES permission(id) ON DELETE CASCADE,
    CONSTRAINT u_role_permission UNIQUE(role_id, permission_id)
);

CREATE TABLE IF NOT EXISTS punishment_queue(

    id            SERIAL          PRIMARY KEY,
    type          punishment_type NOT NULL,
    creation_date TIMESTAMPTZ     NOT NULL DEFAULT CURRENT_TIMESTAMP, -- Used for the automatic cleanup.
--  Only ONE of them is non-null at a time, and it is decided by the type.
    ban_id        INT             NULL UNIQUE,
    warn_id       INT             NULL UNIQUE,
    kick_id       INT             NULL UNIQUE,
    mute_id       INT             NULL UNIQUE,

    CONSTRAINT fk_ban_queue  FOREIGN KEY(ban_id ) REFERENCES ban (id) ON DELETE CASCADE,
    CONSTRAINT fk_warn_queue FOREIGN KEY(warn_id) REFERENCES warn(id) ON DELETE CASCADE,
    CONSTRAINT fk_kick_queue FOREIGN KEY(kick_id) REFERENCES kick(id) ON DELETE CASCADE,
    CONSTRAINT fk_mute_queue FOREIGN KEY(mute_id) REFERENCES mute(id) ON DELETE CASCADE
);

-- I'm forced to do it this way since I can't be sure when all the servers have read the queue or not.
-- So the best strategy is to let the server acknowledge the queue so it knows it has already handled the row.
-- Then after all servers had enough time to read the queue, I can remove the old elements from the queue.
CREATE TABLE IF NOT EXISTS punishment_acknowledge(

    id        INT PRIMARY KEY,
    queue_id  INT NOT NULL,
    server_id INT NOT NULL,

    CONSTRAINT fk_punishment_queue      FOREIGN KEY(queue_id ) REFERENCES punishment_queue(id) ON DELETE CASCADE,
    CONSTRAINT fk_server_ack            FOREIGN KEY(server_id) REFERENCES server(id)           ON DELETE CASCADE,
    CONSTRAINT u_punishment_acknowledge UNIQUE(queue_id, server_id)
);

CREATE TABLE IF NOT EXISTS configuration(

    id        SERIAL PRIMARY KEY,
    key       TEXT   NOT NULL,
    server_id INT    NULL DEFAULT NULL, -- For server-specific configuration, for global configuration null should be used.
    value     JSONB  NOT NULL,

    CONSTRAINT fk_server_config    FOREIGN KEY(server_id) REFERENCES server(id) ON DELETE CASCADE,
    CONSTRAINT u_key_server_config UNIQUE NULLS NOT DISTINCT(key, server_id)
);

-- I insert inside the punishment queue table.
CREATE OR REPLACE FUNCTION insert_punishment_queue() RETURNS trigger AS $$
BEGIN
    IF TG_TABLE_NAME    = 'ban'  THEN
        INSERT INTO punishment_queue(type, ban_id ) VALUES ('ban', NEW.id);
    ELSIF TG_TABLE_NAME = 'kick' THEN
        INSERT INTO punishment_queue(type, kick_id) VALUES ('kick', NEW.id);
    ELSIF TG_TABLE_NAME = 'warn' THEN
        INSERT INTO punishment_queue(type, warn_id) VALUES ('warn', NEW.id);
    ELSIF TG_TABLE_NAME = 'mute' THEN
        INSERT INTO punishment_queue(type, mute_id) VALUES ('mute', NEW.id);
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Insert triggers
CREATE TRIGGER notify_insert_ban AFTER INSERT ON ban
FOR EACH ROW EXECUTE FUNCTION insert_punishment_queue();

CREATE TRIGGER notify_insert_kick AFTER INSERT ON kick
FOR EACH ROW EXECUTE FUNCTION insert_punishment_queue();

CREATE TRIGGER notify_insert_warn AFTER INSERT ON warn
FOR EACH ROW EXECUTE FUNCTION insert_punishment_queue();

CREATE TRIGGER notify_insert_mute AFTER INSERT ON mute
FOR EACH ROW EXECUTE FUNCTION insert_punishment_queue();