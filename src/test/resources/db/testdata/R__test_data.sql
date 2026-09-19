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

INSERT INTO cliente (codigo, tipo_cliente, nome, idade, telefone, endereco, usuario_id)
VALUES
    (100, 'ALUNO', 'Student One', 20, '1111111111', 'Address One', 3),
    (101, 'ALUNO', 'Student Two', 21, '2222222222', 'Address Two', 4);
INSERT INTO aluno (codigo) VALUES (100), (101);

INSERT INTO exemplar (codigo, tipo_exemplar, nome)
VALUES (100, 'Livro', 'Security Engineering'), (101, 'Livro', 'Domain-Driven Design');
INSERT INTO livro (codigo, autor, editora, edicao)
VALUES
    (100, 'Ross Anderson', 'Wiley', 3),
    (101, 'Eric Evans', 'Addison-Wesley', 1);

INSERT INTO emprestimo (id, data_emprestimo, data_devolucao, cliente_id, exemplar_id)
VALUES
    (100, CURRENT_DATE, CURRENT_DATE + 14, 100, 100),
    (101, CURRENT_DATE, CURRENT_DATE + 14, 101, 101);

SELECT setval(pg_get_serial_sequence('usuarios', 'id'), 4, TRUE);
SELECT setval(pg_get_serial_sequence('cliente', 'codigo'), 101, TRUE);
SELECT setval(pg_get_serial_sequence('exemplar', 'codigo'), 101, TRUE);
SELECT setval(pg_get_serial_sequence('emprestimo', 'id'), 101, TRUE);
