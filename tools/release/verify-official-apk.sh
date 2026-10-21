#!/usr/bin/env bash
set -euo pipefail

APK="${1:-}"
if [[ -z "$APK" || ! -f "$APK" ]]; then
  echo "Uso: $0 /caminho/Enquadra.apk" >&2
  exit 2
fi

EXPECTED="36377E0AE263B8ED79B2A91FC4A4826934BC5B9DFBD44EB7B93D6F017A2BF053"
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
