CREATE TABLE iot_pareamento (
    ponto_id BIGINT PRIMARY KEY REFERENCES pontos_agua(id) ON DELETE CASCADE,
    chave_hash VARCHAR(64) NOT NULL,
    expira_ms BIGINT NOT NULL
);
CREATE TABLE iot_leitura (
    ponto_id BIGINT NOT NULL REFERENCES pontos_agua(id) ON DELETE CASCADE,
    evento UUID NOT NULL,
    medida_ms BIGINT NOT NULL,
    percentual INTEGER NOT NULL CHECK (percentual BETWEEN 0 AND 100),
    PRIMARY KEY (ponto_id, evento)
);
CREATE INDEX ix_iot_leitura_ponto_tempo ON iot_leitura (ponto_id, medida_ms);
