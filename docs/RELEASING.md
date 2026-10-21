# Publicação oficial do Enquadra+

## Princípio

O GitHub Actions pode compilar e validar o aplicativo, mas **não recebe a chave privada oficial de assinatura**. O candidato gerado no CI é deliberadamente não assinado. A assinatura final é feita em ambiente local confiável.

Certificado oficial esperado:

```text
SHA-256: 50:D7:B4:DD:CA:B4:B9:37:62:C1:1A:3E:BB:1C:EF:44:4B:45:F3:EE:8F:F4:79:B7:3F:07:F2:51:15:C9:E1:18
Alias: app-release
```

## 1. Gerar o candidato

No GitHub, abra **Actions → Release Candidate → Run workflow** na branch `main`.

O workflow executa testes unitários, Android lint e `:app:assembleRelease`, e publica temporariamente o artefato:

```text
enquadra-release-candidate-unsigned
└── Enquadra-release-unsigned.apk
```

Esse APK ainda não deve ser distribuído a usuários.

## 2. Alinhar e assinar fora do GitHub

Use as ferramentas oficiais do Android SDK. Não informe a senha na linha de comando; deixe o `apksigner` solicitá-la interativamente.

```bash
zipalign -p -f 4 \
  Enquadra-release-unsigned.apk \
  Enquadra-aligned.apk

apksigner sign \
  --ks /caminho/seguro/enquadra-release.jks \
  --ks-key-alias app-release \
  --out Enquadra.apk \
  Enquadra-aligned.apk
```

## 3. Verificar antes de publicar

```bash
apksigner verify --verbose --print-certs Enquadra.apk
sha256sum Enquadra.apk
```

A impressão SHA-256 do certificado deve corresponder exatamente à identidade oficial documentada acima. O script `tools/release/verify-official-apk.sh` automatiza essa conferência.

## 4. Publicar no GitHub Releases

Crie uma release para a versão correspondente e envie o APK final com o nome exato:

```text
Enquadra.apk
```

Nunca publique `Enquadra-release-unsigned.apk` como download para usuários.

Quando houver uma release estável marcada como release normal (não pre-release), o endereço permanente para usuários será:

```text
https://github.com/cybertran/Enquadra-plus-release/releases/latest/download/Enquadra.apk
```

Para versões alpha ou beta marcadas como pre-release, use o endereço específico da tag até a primeira release estável.

## 5. Atualizar o manifesto consumido pelo aplicativo

Somente **depois** de a release oficial estar publicada e de `Enquadra.apk` ter passado pela verificação de assinatura e SHA-256, atualize `app-update.json`.

Preencha:

- `versionCode` e `versionName` exatamente como no APK publicado;
- `releaseUrl` para a tag oficial;
- `apkUrl` para o asset `Enquadra.apk` daquela mesma tag;
- `apkSha256` com o SHA-256 calculado do APK oficial;
- `signingCertificateSha256` com o fingerprint oficial documentado.

Execute:

```bash
python3 tools/release/verify-app-update-manifest.py
```

O manifesto pode ficar atrás da versão do código-fonte enquanto uma candidata ainda não foi publicada. Ele **não pode** ficar à frente do `versionCode` do código público e nunca deve apontar para APK de CI, debug, candidato não assinado ou arquivo de terceiros.

A alteração do manifesto deve ser revisável e passar pelo CI antes de entrar em `main`. Assim, usuários antigos só recebem o aviso depois que o APK oficial realmente existe.
