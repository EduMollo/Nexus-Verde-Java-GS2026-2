#!/usr/bin/env bash
# Ensaio com pesos de um modelo local e as mesmas ferramentas do agente.
set -euo pipefail
tmp=$(mktemp -d)
chave=$(openssl rand -hex 32)
docker network create avaliacao >/dev/null
trap 'docker logs ensaio --tail 20 2>/dev/null || true; docker rm -f ensaio ollama >/dev/null 2>&1 || true; docker network rm avaliacao >/dev/null; rm -f "$tmp/workflow.json"; rmdir "$tmp"' EXIT
docker run -d --name ollama --network avaliacao -p 127.0.0.1:11434:11434 -e OLLAMA_NUM_PARALLEL=1 ollama/ollama:0.12.6
curl -fsS --retry 20 --retry-all-errors --retry-delay 2 http://localhost:11434/api/version
docker exec ollama ollama pull qwen2.5:7b
curl -fsS http://localhost:11434/api/generate -d '{"model":"qwen2.5:7b","keep_alive":"10m"}' >/dev/null
docker build -f n8n/Dockerfile -t avaliacao-n8n .
jq '.settings.executionTimeout=300' n8n/abastecimento.json >"$tmp/workflow.json"
docker run -d --name ensaio --network avaliacao -p 127.0.0.1:5679:10000 \
  -v "$tmp/workflow.json:/opt/nexus/abastecimento.json:ro" -e IOT_N8N_TOKEN="$chave" \
  -e MODEL_ID=qwen2.5:7b -e IOT_AGENT_TIMEOUT_MS=300000 -e GROQ_API_KEY=local-sem-autenticacao -e MODEL_BASE_URL=http://ollama:11434/v1 avaliacao-n8n
curl -fsS --retry 60 --retry-all-errors --retry-delay 3 http://localhost:5679/healthz
curl -fsS --max-time 310 -H "X-Copiloto-Key: $chave" -H 'Content-Type: application/json' --data @tests/entrada.json \
  http://localhost:5679/webhook/abastecimento > resultado-generativo.json
jq -e '.fonte=="EST-04" and (.texto|length>10)' resultado-generativo.json
cat resultado-generativo.json
