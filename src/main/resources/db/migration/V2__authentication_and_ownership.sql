INSERT INTO perfis (nome)
VALUES ('ROLE_STUDENT'), ('ROLE_STAFF'), ('ROLE_ADMIN')
ON CONFLICT (nome) DO NOTHING;

INSERT INTO usuarios_perfis (usuarios_id, perfis_id)
SELECT usuarios_perfis.usuarios_id, student.id
FROM usuarios_perfis
JOIN perfis legacy ON legacy.id = usuarios_perfis.perfis_id
CROSS JOIN perfis student
WHERE legacy.nome = 'ROLE_USER'
  AND student.nome = 'ROLE_STUDENT'
ON CONFLICT (usuarios_id, perfis_id) DO NOTHING;

DELETE FROM usuarios_perfis
WHERE perfis_id IN (SELECT id FROM perfis WHERE nome = 'ROLE_USER');
DELETE FROM perfis WHERE nome = 'ROLE_USER';

ALTER TABLE cliente
    ADD COLUMN usuario_id BIGINT,
    ADD CONSTRAINT fk_cliente_usuario
        FOREIGN KEY (usuario_id) REFERENCES usuarios (id),
    ADD CONSTRAINT uk_cliente_usuario UNIQUE (usuario_id);

CREATE TABLE refresh_tokens (
    id UUID PRIMARY KEY,
    token_hash VARCHAR(64) NOT NULL,
    usuario_id BIGINT NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
    revoked_at TIMESTAMP WITH TIME ZONE,
    replaced_by_token_hash VARCHAR(64),
    CONSTRAINT uk_refresh_tokens_hash UNIQUE (token_hash),
    CONSTRAINT fk_refresh_tokens_usuario
        FOREIGN KEY (usuario_id) REFERENCES usuarios (id) ON DELETE CASCADE
);

CREATE INDEX idx_refresh_tokens_usuario_active
    ON refresh_tokens (usuario_id, revoked_at, expires_at);
CREATE INDEX idx_cliente_usuario_id ON cliente (usuario_id);
