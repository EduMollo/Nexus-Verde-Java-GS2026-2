#!/bin/sh
set -eu
umask 077
cred=$(mktemp)
fluxo=$(mktemp)
trap 'rm -f "$cred" "$fluxo"' EXIT
node /opt/nexus/credenciais.mjs "$cred" "$fluxo"
n8n import:credentials --input="$cred"
rm -f "$cred"
n8n import:workflow --input="$fluxo"
rm -f "$fluxo"
n8n publish:workflow --id=NexusAguaGS2026
node /opt/nexus/entrada.mjs &
exec n8n start
