INSERT INTO perfis (nome)
VALUES ('ROLE_STUDENT'), ('ROLE_STAFF'), ('ROLE_ADMIN')
ON CONFLICT (nome) DO NOTHING;

INSERT INTO usuarios (id, nome, sobrenome, email, senha, is_confirmado)
VALUES
    (1, 'Admin', 'do Sistema', 'admin@email.com',
     '$2a$10$HKveMsPlst41Ie2LQgpijO691lUtZ8cLfcliAO1DD9TtZxEpaEoJe', TRUE),
    (2, 'Staff', 'do Sistema', 'staff@email.com',
     '$2a$10$HKveMsPlst41Ie2LQgpijO691lUtZ8cLfcliAO1DD9TtZxEpaEoJe', TRUE),
    (3, 'Student', 'One', 'student1@email.com',
     '$2a$10$HKveMsPlst41Ie2LQgpijO691lUtZ8cLfcliAO1DD9TtZxEpaEoJe', TRUE),
    (4, 'Student', 'Two', 'student2@email.com',
     '$2a$10$HKveMsPlst41Ie2LQgpijO691lUtZ8cLfcliAO1DD9TtZxEpaEoJe', TRUE);

INSERT INTO usuarios_perfis (usuarios_id, perfis_id)
SELECT 1, id FROM perfis WHERE nome = 'ROLE_ADMIN'
UNION ALL
SELECT 2, id FROM perfis WHERE nome = 'ROLE_STAFF'
UNION ALL
SELECT 3, id FROM perfis WHERE nome = 'ROLE_STUDENT'
UNION ALL
SELECT 4, id FROM perfis WHERE nome = 'ROLE_STUDENT';

SELECT setval(pg_get_serial_sequence('usuarios', 'id'), 4, TRUE);
