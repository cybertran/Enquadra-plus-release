#!/usr/bin/env bash
set -euo pipefail

APK="${1:-}"
if [[ -z "$APK" || ! -f "$APK" ]]; then
  echo "Uso: $0 /caminho/Enquadra.apk" >&2
  exit 2
fi

EXPECTED="BE58EAEDF7AD76B65F75FC086ADC7189F491A72567B58FE7BBF3EADA3A2C93A9"
APKSIGNER="${APKSIGNER:-apksigner}"

OUTPUT="$("$APKSIGNER" verify --verbose --print-certs "$APK")"
printf '%s\n' "$OUTPUT"

ACTUAL="$(
  printf '%s\n' "$OUTPUT" |
    grep -i -m1 'certificate SHA-256 digest' |
    sed -E 's/.*digest:[[:space:]]*//' |
    tr -d '[:space:]:' |
    tr '[:lower:]' '[:upper:]'
)"

if [[ -z "$ACTUAL" ]]; then
  echo "Não foi possível ler o SHA-256 do certificado do APK." >&2
  exit 1
fi

if [[ "$ACTUAL" != "$EXPECTED" ]]; then
  echo "ERRO: o APK não está assinado com o certificado oficial do Enquadra+." >&2
  exit 1
fi

echo "Assinatura oficial confirmada."
sha256sum "$APK"
