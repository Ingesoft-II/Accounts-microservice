-- Cuentas de jugadores (RF_01).
CREATE TABLE accounts (
    id              UUID          NOT NULL,
    email           VARCHAR(255)  NOT NULL,
    display_name    VARCHAR(20)   NOT NULL,
    roles           TEXT          NOT NULL,
    status          VARCHAR(20)   NOT NULL,
    suspended_until TIMESTAMP     NULL,
    google_subject  VARCHAR(255)  NOT NULL,
    verified_at     TIMESTAMP     NULL,
    created_at      TIMESTAMP     NOT NULL,
    CONSTRAINT pk_accounts PRIMARY KEY (id),
    CONSTRAINT uq_accounts_email UNIQUE (email),
    CONSTRAINT uq_accounts_google_subject UNIQUE (google_subject)
);

-- Desafíos 2FA.
CREATE TABLE two_factor_challenges (
    id          UUID         NOT NULL,
    account_id  UUID         NOT NULL,
    code_hash   VARCHAR(64)  NOT NULL,
    salt        VARCHAR(64)  NOT NULL,
    expires_at  TIMESTAMP    NOT NULL,
    attempts    INTEGER      NOT NULL,
    consumed_at TIMESTAMP    NULL,
    created_at  TIMESTAMP    NOT NULL,
    CONSTRAINT pk_two_factor_challenges PRIMARY KEY (id),
    CONSTRAINT fk_challenges_account FOREIGN KEY (account_id) REFERENCES accounts (id)
);

CREATE INDEX idx_challenges_account_created ON two_factor_challenges (account_id, created_at);

-- Refresh tokens opacos, rotativos y organizados en familias.
CREATE TABLE refresh_tokens (
    id          UUID         NOT NULL,
    account_id  UUID         NOT NULL,
    token_hash  VARCHAR(64)  NOT NULL,
    family_id   UUID         NOT NULL,
    expires_at  TIMESTAMP    NOT NULL,
    rotated_at  TIMESTAMP    NULL,
    revoked_at  TIMESTAMP    NULL,
    CONSTRAINT pk_refresh_tokens PRIMARY KEY (id),
    CONSTRAINT fk_refresh_account FOREIGN KEY (account_id) REFERENCES accounts (id),
    CONSTRAINT uq_refresh_tokens_token_hash UNIQUE (token_hash)
);

CREATE INDEX idx_refresh_tokens_account ON refresh_tokens (account_id);
CREATE INDEX idx_refresh_tokens_family ON refresh_tokens (family_id);

CREATE TABLE processed_events (
    event_id     VARCHAR(64) NOT NULL,
    processed_at TIMESTAMP   NOT NULL,
    CONSTRAINT pk_processed_events PRIMARY KEY (event_id)
);
