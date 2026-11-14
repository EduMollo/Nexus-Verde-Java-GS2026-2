CREATE TABLE eventos_pendentes (
    id UUID PRIMARY KEY,
    solicitacao_id BIGINT NOT NULL REFERENCES solicitacoes(id),
    criado_em TIMESTAMP WITH TIME ZONE NOT NULL,
    publicado_em TIMESTAMP WITH TIME ZONE
);

CREATE INDEX idx_eventos_publicacao ON eventos_pendentes(publicado_em, criado_em);
