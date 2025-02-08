-- Database creation used during deployment.

CREATE TABLE IF NOT EXISTS account(

    id            SERIAL       PRIMARY KEY,
    username      VARCHAR(15)  NOT NULL UNIQUE, -- Anything bigger than 15 is quite long.
    display_name  TINYTEXT     NOT NULL,
    creation_date DATETIME(3)  NOT NULL DEFAULT UTC_TIMESTAMP(3),
    -- argon2id variable length hash, I use TEXT because the hash can get very long by changing the settings, and I want to allow that for future proofing.
    password      TEXT         NOT NULL,

    -- I use the discord username validation since they fit our use cases.
    CONSTRAINT chk_username_valid CHECK (
            CHAR_LENGTH(username) > 2 AND
            username REGEXP '^[a-z0-9_.]+$' AND
            username NOT REGEXP '\\.\\.'
    )
);

-- TODO Server authorization based on user role.
CREATE TABLE IF NOT EXISTS server(

    id               SERIAL      PRIMARY KEY,
    ip_address       INET4       NOT NULL,
    port             SMALLINT    NOT NULL,
    name             TINYTEXT    NOT NULL,
    -- Last heartbeat available used in case the server goes offline.
    heartbeat        DATETIME(3) NOT NULL DEFAULT UTC_TIMESTAMP(3),
    -- The period of the heartbeat in milliseconds.
    heartbeat_period INT         NOT NULL DEFAULT 5000,

    CONSTRAINT u_server_ip_port UNIQUE(ip_address, port)
);

CREATE TABLE IF NOT EXISTS login(

    id         SERIAL      PRIMARY KEY,
    account_id INT         NOT NULL,
    ip_address INET4       NOT NULL,
    login_date DATETIME(3) NOT NULL DEFAULT UTC_TIMESTAMP(3),

    CONSTRAINT fk_login_user FOREIGN KEY(account_id) REFERENCES account(id)
);

CREATE TABLE IF NOT EXISTS account_session(

    id              SERIAL      PRIMARY KEY,
    account_id      INT         NOT NULL UNIQUE, -- There can be only one session at a time.
    session_cookie  BINARY(32)  NOT NULL UNIQUE,
    expiration_date DATETIME(3) NOT NULL DEFAULT UTC_TIMESTAMP(3),

    CONSTRAINT fk_account_session FOREIGN KEY(account_id) REFERENCES account(id)
);

CREATE TABLE IF NOT EXISTS server_join(

    id         SERIAL      PRIMARY KEY,
    account_id INT         NOT NULL,
    server_id  INT         NOT NULL,
    join_date  DATETIME(3) NOT NULL DEFAULT UTC_TIMESTAMP(3),
    leave_date DATETIME(3) NULL     DEFAULT NULL,

    CONSTRAINT fk_server_join_user   FOREIGN KEY(account_id) REFERENCES account(id),
    CONSTRAINT fk_server_join_server FOREIGN KEY(server_id)  REFERENCES server(id)
);

CREATE TABLE IF NOT EXISTS report(

    id            SERIAL      PRIMARY KEY,
    account_id    INT         NOT NULL,
    reported_id   INT         NOT NULL,
    short_reason  TINYTEXT    NOT NULL,
    long_reason   TEXT        NOT NULL DEFAULT '',
    creation_date DATETIME(3) NOT NULL DEFAULT UTC_TIMESTAMP(3),

    CONSTRAINT fk_report_account  FOREIGN KEY(account_id)  REFERENCES account(id),
    CONSTRAINT fk_report_reported FOREIGN KEY(reported_id) REFERENCES account(id)
);

CREATE TABLE IF NOT EXISTS report_reply(

    id            SERIAL      PRIMARY KEY,
    report_id     INT         NOT NULL UNIQUE,
    staff_id      INT         NOT NULL,
    accepted      BOOLEAN     NOT NULL,
    message       TEXT        NOT NULL DEFAULT '',
    creation_date DATETIME(3) NOT NULL DEFAULT UTC_TIMESTAMP(3),

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
    creation_date   DATETIME(3) NOT NULL DEFAULT UTC_TIMESTAMP(3),
    -- Null for bans that are permanent.
    expiration_date DATETIME(3) NULL,

    CONSTRAINT fk_ban_user   FOREIGN KEY(account_id) REFERENCES account(id),
    CONSTRAINT fk_ban_staff  FOREIGN KEY(staff_id)   REFERENCES account(id),
    CONSTRAINT fk_ban_server FOREIGN KEY(server_id)  REFERENCES server(id)
);

CREATE TABLE IF NOT EXISTS unban(

    id            SERIAL      PRIMARY KEY,
    ban_id        INT         NOT NULL UNIQUE,
    staff_id      INT         NOT NULL,
    creation_date DATETIME(3) NOT NULL DEFAULT UTC_TIMESTAMP(3),

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
    creation_date DATETIME(3) NOT NULL DEFAULT UTC_TIMESTAMP(3),

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
    creation_date DATETIME(3) NOT NULL DEFAULT UTC_TIMESTAMP(3),

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
    creation_date   DATETIME(3) NOT NULL,
    expiration_date DATETIME(3) NOT NULL,

    CONSTRAINT fk_mute_user   FOREIGN KEY(account_id) REFERENCES account(id),
    CONSTRAINT fk_mute_staff  FOREIGN KEY(staff_id)   REFERENCES account(id),
    CONSTRAINT fk_mute_server FOREIGN KEY(server_id)  REFERENCES server(id)
);

CREATE TABLE IF NOT EXISTS ban_appeal(

    id            SERIAL      PRIMARY KEY,
    account_id    INT         NOT NULL,
    ban_id        INT         NOT NULL,
    message       TEXT        NOT NULL,
    creation_date DATETIME(3) NOT NULL DEFAULT UTC_TIMESTAMP(3),

    CONSTRAINT fk_ban_appeal_account FOREIGN KEY(account_id) REFERENCES account(id),
    CONSTRAINT fk_ban_appeal_ban     FOREIGN KEY(ban_id)     REFERENCES ban(id)
);

CREATE TABLE IF NOT EXISTS ban_appeal_reply(

    id            SERIAL      PRIMARY KEY,
    ban_appeal_id INT         NOT NULL,
    staff_id      INT         NOT NULL,
    accepted      BOOLEAN     NOT NULL,
    message       TEXT        NOT NULL DEFAULT '',
    creation_date DATETIME(3) NOT NULL DEFAULT UTC_TIMESTAMP(3),

    CONSTRAINT fk_ban_appeal_reply_ban   FOREIGN KEY(ban_appeal_id) REFERENCES ban_appeal(id),
    CONSTRAINT fk_ban_appeal_reply_staff FOREIGN KEY(staff_id)      REFERENCES account(id)
);

CREATE TABLE IF NOT EXISTS ip_blacklist(

    id            SERIAL      PRIMARY KEY,
    staff_id      INT         NOT NULL,
    ip_address    INET4       NOT NULL UNIQUE,
    reason        TEXT        NOT NULL DEFAULT '',
    creation_date DATETIME(3) NOT NULL DEFAULT UTC_TIMESTAMP(3),

    CONSTRAINT fk_ip_blacklist_staff FOREIGN KEY(staff_id) REFERENCES account(id)
);

CREATE TABLE IF NOT EXISTS role(

    id       SERIAL      PRIMARY KEY,
    name     TINYTEXT    NOT NULL,
    -- The order of which the roles are displayed, smallest first.
    priority TINYINT     NOT NULL,
    symbol   VARCHAR(16) NOT NULL,
    color    CHAR(8)     NOT NULL
);

CREATE TABLE IF NOT EXISTS permission(

    id       SERIAL   PRIMARY KEY,
    -- Lowercase with - as space separators.
    property TINYTEXT NOT NULL UNIQUE
);

CREATE TABLE IF NOT EXISTS account_role(

    id         SERIAL PRIMARY KEY,
    account_id INT    NOT NULL,
    role_id    INT    NOT NULL,

    CONSTRAINT fk_roles_user FOREIGN KEY(account_id) REFERENCES account(id) ON DELETE CASCADE,
    CONSTRAINT fk_roles_role FOREIGN KEY(role_id)    REFERENCES role(id)    ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS role_permission(

    id            SERIAL PRIMARY KEY,
    role_id       INT    NOT NULL,
    permission_id INT    NOT NULL,

    CONSTRAINT fk_role       FOREIGN KEY(role_id)       REFERENCES role(id)       ON DELETE CASCADE,
    CONSTRAINT fk_permission FOREIGN KEY(permission_id) REFERENCES permission(id) ON DELETE CASCADE
);
