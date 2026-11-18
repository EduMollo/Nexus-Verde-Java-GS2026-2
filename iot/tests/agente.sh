#!/usr/bin/env bash
set -euo pipefail
docker network create nexus-teste >/dev/null
docker build -f n8n/Dockerfile -t nexus-agente-teste .
docker run -d --name modelo --network nexus-teste -v "$PWD/tests:/testes:ro" node:22-alpine node /testes/modelo-de-teste.mjs
chave=$(openssl rand -hex 32)
docker run -d --name agente --network nexus-teste -p 127.0.0.1:5679:10000 \
  -e IOT_N8N_TOKEN="$chave" -e GROQ_API_KEY=credencial-ficticia -e MODEL_BASE_URL=http://modelo:9000/v1 nexus-agente-teste
trap 'docker logs modelo --tail 5; docker logs agente --tail 45; docker rm -f agente modelo >/dev/null; docker network rm nexus-teste >/dev/null' EXIT
curl --retry 55 --retry-delay 3 --retry-all-errors --max-time 5 -fsS http://127.0.0.1:5679/healthz
test "$(curl -s -o /dev/null -w '%{http_code}' -X POST http://127.0.0.1:5679/webhook/abastecimento)" = 401
curl -fsS -H "X-Copiloto-Key: $chave" -H 'Content-Type: application/json' --data @tests/entrada.json \
  http://127.0.0.1:5679/webhook/abastecimento | jq -e '.fonte == "EST-04" and (.texto | length>10)'
