ALTER TABLE solicitacoes ADD COLUMN cadastrada_por_id BIGINT REFERENCES usuarios(id);
ALTER TABLE solicitacoes ADD COLUMN registro_painel UUID UNIQUE;
