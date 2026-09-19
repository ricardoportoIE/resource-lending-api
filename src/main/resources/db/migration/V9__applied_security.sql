ALTER TABLE usuarios
    ADD COLUMN failed_login_attempts INTEGER NOT NULL DEFAULT 0,
    ADD COLUMN locked_until TIMESTAMPTZ,
    ADD COLUMN security_version INTEGER NOT NULL DEFAULT 0,
    ADD CONSTRAINT chk_usuarios_failed_login_attempts CHECK (failed_login_attempts >= 0),
    ADD CONSTRAINT chk_usuarios_security_version CHECK (security_version >= 0);

CREATE TABLE identity_tokens (
    id UUID PRIMARY KEY,
    token_hash VARCHAR(64) NOT NULL UNIQUE,
    usuario_id BIGINT NOT NULL REFERENCES usuarios(id) ON DELETE CASCADE,
    purpose VARCHAR(30) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    expires_at TIMESTAMPTZ NOT NULL,
    consumed_at TIMESTAMPTZ,
    CONSTRAINT chk_identity_token_purpose
        CHECK (purpose IN ('EMAIL_CONFIRMATION', 'PASSWORD_RESET'))
);

CREATE INDEX idx_identity_tokens_user_purpose
    ON identity_tokens (usuario_id, purpose, created_at DESC);

CREATE TABLE revoked_access_tokens (
    jwt_id VARCHAR(64) PRIMARY KEY,
    expires_at TIMESTAMPTZ NOT NULL,
    revoked_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX idx_revoked_access_tokens_expiry ON revoked_access_tokens (expires_at);
