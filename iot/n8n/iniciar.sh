#!/bin/sh
set -eu
umask 077
cred=$(mktemp)
trap 'rm -f "$cred"' EXIT
node /opt/nexus/credenciais.mjs "$cred"
n8n import:credentials --input="$cred"
rm -f "$cred"
n8n import:workflow --input=/opt/nexus/abastecimento.json
n8n publish:workflow --id=NexusAguaGS2026
node /opt/nexus/entrada.mjs &
exec n8n start
