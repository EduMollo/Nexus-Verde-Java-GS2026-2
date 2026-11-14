INSERT INTO regioes (nome, cidade, nivel_risco, situacao, descricao, populacao_afetada, atualizado_em, versao)
VALUES ('Bairro Mathias Velho', 'Canoas', 'CRITICO', 'Abastecimento interrompido',
        'Cenário acadêmico fictício de enchente e interrupção no fornecimento.', 45000, CURRENT_TIMESTAMP, 0),
       ('Bairro São Geraldo', 'Porto Alegre', 'ALTO', 'Abastecimento parcial',
        'Cenário acadêmico fictício de distribuição emergencial.', 7600, CURRENT_TIMESTAMP, 0);

INSERT INTO pontos_agua (regiao_id, nome, tipo, endereco, cidade, latitude, longitude, status, capacidade_litros,
                         estoque_litros, horario_funcionamento, atualizado_em, versao)
SELECT id, 'Posto Ginásio (demonstração)', 'POSTO_DISTRIBUICAO', 'Endereço fictício de demonstração', cidade,
       -29.91, -51.18, 'DISPONIVEL', 20000, 14500, '08h às 20h', CURRENT_TIMESTAMP, 0
FROM regioes WHERE nome = 'Bairro Mathias Velho' AND cidade = 'Canoas';

INSERT INTO pontos_agua (regiao_id, nome, tipo, endereco, cidade, latitude, longitude, status, capacidade_litros,
                         estoque_litros, horario_funcionamento, atualizado_em, versao)
SELECT id, 'Abrigo São Geraldo (demonstração)', 'ABRIGO', 'Endereço fictício de demonstração', cidade,
       -30.01, -51.20, 'ESTOQUE_BAIXO', 8000, 1200, '09h às 18h', CURRENT_TIMESTAMP, 0
FROM regioes WHERE nome = 'Bairro São Geraldo' AND cidade = 'Porto Alegre';
