CREATE SCHEMA IF NOT EXISTS legacy;

-- Preserve the original academic model for traceability while removing it from the active API schema.
ALTER TABLE IF EXISTS emprestimo SET SCHEMA legacy;
ALTER TABLE IF EXISTS aluno SET SCHEMA legacy;
ALTER TABLE IF EXISTS pai_de_aluno SET SCHEMA legacy;
ALTER TABLE IF EXISTS livro SET SCHEMA legacy;
ALTER TABLE IF EXISTS artigo SET SCHEMA legacy;
ALTER TABLE IF EXISTS periodico SET SCHEMA legacy;
ALTER TABLE IF EXISTS cliente SET SCHEMA legacy;
ALTER TABLE IF EXISTS exemplar SET SCHEMA legacy;
