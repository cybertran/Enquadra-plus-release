# Publicação oficial com assinatura offline

## Princípio

A chave privada oficial do Enquadra+ permanece fora do GitHub, fora dos GitHub Actions e fora do repositório. O CI pode compilar, testar e validar candidatos, mas não recebe o keystore nem as senhas de assinatura.

A assinatura continua offline como na v0.2.0-alpha.9, mas a alpha.10 inicia uma nova cadeia porque a chave privada usada na alpha.9 foi considerada perdida. A transição alpha.9 → alpha.10 exige desinstalação e reinstalação manual; a partir da alpha.10 a nova chave deve ser preservada permanentemente.

## Fluxo canônico

1. O código é desenvolvido e validado no repositório privado.
2. A parte publicável é sincronizada com `cybertran/Enquadra-plus-release`.
3. O CI privado e/ou o workflow público **Release Candidate** executam testes, lint, gates de segurança e geram um APK release **não assinado**.
4. O artifact `Enquadra-release-unsigned.apk` é baixado para o `Notebook-enquadra`.
5. A assinatura é feita localmente, fora do GitHub Actions, usando o keystore oficial.
6. O APK assinado é validado localmente: alinhamento, assinatura, fingerprint do certificado, `versionCode`, `versionName` e SHA-256.
7. O arquivo final recebe sempre o nome `Enquadra.apk`.
8. A release oficial é criada no repositório público e recebe somente o APK já assinado e validado.
9. Depois que a release pública existir, `app-update.json` é atualizado em PR separado com a versão, URLs e SHA-256 reais.
10. O manifesto só entra em `main` depois dos gates do repositório público.


## Candidato unsigned

No repositório público, use **Actions → Release Candidate → Run workflow** na branch `main`.

O artifact esperado é:

```text
enquadra-release-candidate-unsigned
├── Enquadra-release-unsigned.apk
└── Enquadra-release-unsigned.apk.sha256
```

O workflow privado `Publish controlled release` também pode gerar o mesmo tipo de candidate unsigned quando houver um arquivo de controle válido em `.github/releases/`. Ele não assina nem publica APK oficial.

## Assinatura local no Windows

A forma preferida é executar, na raiz do repositório privado:

```powershell
.\Publicar-Enquadra.ps1
```

O script procura automaticamente o candidato unsigned mais recente em `Downloads`, aceitando tanto `Enquadra-release-unsigned.apk` quanto o ZIP baixado do artifact `enquadra-release-candidate-unsigned`.

Se o caminho do keystore oficial estiver definido localmente em `ENQUADRA_RELEASE_KEYSTORE`, ele é usado automaticamente. Caso contrário, o script solicita apenas o caminho do arquivo `.jks`. As senhas são pedidas depois, de forma interativa, e não são gravadas em arquivo nem no repositório.

Também é possível informar tudo explicitamente:

```powershell
.\Publicar-Enquadra.ps1 \
  -Candidate "C:\caminho\Enquadra-release-unsigned.apk" \
  -Keystore "C:\caminho-seguro\enquadra-release.jks"
```

O alias padrão é `app-release`. Se necessário, use `-Alias`. Para escolher outra pasta de saída, use `-OutputDirectory`. O script não sobrescreve uma release local existente sem `-Force`.

`Publicar-Enquadra.ps1` chama internamente `tools/release/sign_offline_windows.py` e executa toda a etapa local:

- localiza ou extrai o candidato unsigned;
- confirma Python 3.12+;
- solicita as senhas localmente;
- executa `zipalign`;
- assina com `apksigner`;
- rejeita certificado de debug;
- exige o fingerprint SHA-256 oficial;
- confere `versionCode` e `versionName` contra `app/build.gradle.kts`;
- gera `Enquadra.apk`;
- gera `Enquadra.apk.sha256`;
- informa claramente quando o APK está pronto para publicação.

As senhas existem apenas na memória dos processos locais durante a assinatura e não são persistidas pelo fluxo.

## Identidade oficial

Fingerprint SHA-256 do certificado oficial:

```text
36:37:7E:0A:E2:63:B8:ED:79:B2:A9:1F:C4:A4:82:69:34:BC:5B:9D:FB:D4:4E:B7:B9:3D:6F:01:7A:2B:F0:53
```

O keystore e as senhas não devem ser colocados em:

- GitHub Actions Secrets;
- arquivos de workflow;
- commits;
- artifacts de CI;
- issues ou pull requests;
- logs.

## Publicação pública

Crie a release no repositório `cybertran/Enquadra-plus-release` somente depois da validação local.

Para uma alpha:

- tag: por exemplo `v0.2.0-alpha.10`;
- marcar como pre-release;
- asset: exatamente `Enquadra.apk`;
- registrar nas notas o SHA-256 do APK e o fingerprint do certificado.

Nunca publique `Enquadra-release-unsigned.apk` como download para usuários.

## Manifesto de atualização do aplicativo

`app-update.json` continua sendo o mecanismo usado pela função **Versão → Verificar atualização**.

A assinatura offline não muda esse mecanismo. O manifesto contém apenas metadados públicos da release já existente:

- `versionCode`;
- `versionName`;
- URL da release;
- URL de `Enquadra.apk`;
- SHA-256 do APK;
- fingerprint do certificado.

O manifesto nunca deve apontar para um candidate unsigned, artifact de CI ou versão que ainda não possua release oficial assinada.

## Fail-closed

A publicação deve ser interrompida se qualquer uma destas condições ocorrer:

- gates de segurança, testes ou lint falharem;
- o APK unsigned não corresponder à versão esperada;
- o APK final não estiver alinhado;
- a assinatura for inválida;
- o certificado divergir do fingerprint oficial;
- `versionCode` ou `versionName` divergirem;
- o SHA-256 não for calculado;
- a release/tag já existir de forma conflitante.

A chave privada nunca é um requisito de CI.
