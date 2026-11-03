-- Mot de passe haché (BCrypt) : nullable car les comptes Keycloak (étape 4B) n'auront pas de mot de passe local
ALTER TABLE users
    ADD COLUMN password_hash          VARCHAR(100),
    ADD COLUMN failed_login_attempts  INTEGER NOT NULL DEFAULT 0,
    ADD COLUMN locked_until           TIMESTAMPTZ;

-- On ne stocke que l'empreinte SHA-256 du refresh token : une fuite de la base ne donne aucun jeton utilisable
CREATE TABLE refresh_tokens (
    id          BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    user_id     BIGINT      NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    token_hash  VARCHAR(64) NOT NULL UNIQUE,
    expires_at  TIMESTAMPTZ NOT NULL,
    revoked_at  TIMESTAMPTZ,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_refresh_tokens_user_id ON refresh_tokens (user_id);

-- Journal d'audit de chaque tentative de connexion
CREATE TABLE login_events (
    id          BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    email       VARCHAR(255) NOT NULL,
    user_id     BIGINT       REFERENCES users (id) ON DELETE SET NULL,
    ip_address  VARCHAR(45)  NOT NULL,
    outcome     VARCHAR(20)  NOT NULL CHECK (outcome IN ('SUCCESS', 'BAD_CREDENTIALS', 'UNKNOWN_EMAIL', 'LOCKED')),
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE INDEX idx_login_events_created_at ON login_events (created_at);
