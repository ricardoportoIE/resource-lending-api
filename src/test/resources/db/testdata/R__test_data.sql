INSERT INTO perfis (id, nome)
VALUES
    (1, 'ROLE_ADMIN'),
    (2, 'ROLE_USER');

INSERT INTO usuarios (id, nome, sobrenome, email, senha, is_confirmado)
VALUES
    (
        1,
        'Admin',
        'do Sistema',
        'admin@email.com',
        '$2a$10$HKveMsPlst41Ie2LQgpijO691lUtZ8cLfcliAO1DD9TtZxEpaEoJe',
        TRUE
    ),
    (
        2,
        'Usuario',
        'do Sistema',
        'user@email.com',
        '$2a$10$HKveMsPlst41Ie2LQgpijO691lUtZ8cLfcliAO1DD9TtZxEpaEoJe',
        TRUE
    );

INSERT INTO usuarios_perfis (usuarios_id, perfis_id)
VALUES
    (1, 1),
    (2, 2);

SELECT setval(pg_get_serial_sequence('perfis', 'id'), 2, TRUE);
SELECT setval(pg_get_serial_sequence('usuarios', 'id'), 2, TRUE);
