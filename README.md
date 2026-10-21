# Enquadra+

Repositório público e open source do Enquadra+, aplicativo Android para consulta rápida de informações oficiais de trânsito e apoio à fiscalização.

## Estado

O código-fonte público está preparado como candidato **v0.2.0-alpha.10** (`versionCode = 11`), mantendo histórico Git público limpo e o fluxo de segurança/release próprio deste repositório. A última release oficial assinada continua sendo **v0.2.0-alpha.9** (`versionCode = 10`) até que uma nova build seja publicada e verificada.

O projeto usa fontes públicas e oficiais, como CTB, MBFT, Resoluções CONTRAN, atos da Senatran e demais normas aplicáveis. O Enquadra+ é independente e não incorpora análises jurídicas, comentários, resumos de jurisprudência ou outros conteúdos autorais de produtos de terceiros.

A arquitetura é Android nativo com Kotlin, Jetpack Compose e Material Design 3, com funcionamento local-first e pesquisa offline. O roteiro funcional está em [docs/FUNCTIONAL_ROADMAP.md](docs/FUNCTIONAL_ROADMAP.md).

## Documentação

Consulte [docs/PRODUCT.md](docs/PRODUCT.md), [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md), [docs/DATA_MODEL.md](docs/DATA_MODEL.md), [docs/SOURCES.md](docs/SOURCES.md), [docs/IMPORTER.md](docs/IMPORTER.md) e [docs/SAFE_NORMATIVE_UPDATE_DOSSIER.md](docs/SAFE_NORMATIVE_UPDATE_DOSSIER.md).

O processo da primeira importação pública está registrado em [docs/OPEN_SOURCE_IMPORT.md](docs/OPEN_SOURCE_IMPORT.md).

## Builds oficiais

O código-fonte pode ser compilado por qualquer pessoa. Isso não transforma uma build de terceiros em build oficial do Enquadra+.

A identidade criptográfica oficial do aplicativo é verificada pelo certificado de assinatura. Fingerprint SHA-256 oficial:

```text
36:37:7E:0A:E2:63:B8:ED:79:B2:A9:1F:C4:A4:82:69:34:BC:5B:9D:FB:D4:4E:B7:B9:3D:6F:01:7A:2B:F0:53
```

A chave privada de assinatura e sua senha não fazem parte deste repositório e não devem ser enviadas em issues, pull requests, workflows ou commits.

O fluxo público gera apenas um candidato de release **não assinado**. A assinatura oficial continua sendo feita fora do GitHub com a chave privada mantida offline. Depois da publicação da primeira release oficial, o arquivo para usuários será sempre chamado `Enquadra.apk`, permitindo usar o endereço permanente:

```text
https://github.com/cybertran/Enquadra-plus-release/releases/latest/download/Enquadra.apk
```

O arquivo [app-update.json](app-update.json) é o manifesto consumido pela verificação manual de atualização do aplicativo. Ele aponta **somente para uma release oficial já assinada e publicada**; nunca deve ser antecipado para uma versão que exista apenas no código-fonte ou no CI.

O procedimento completo está em [docs/RELEASING.md](docs/RELEASING.md).

## Segurança

Leia [SECURITY.md](SECURITY.md), [SECURITY_BASELINE.md](SECURITY_BASELINE.md) e [SECURITY_CHECKLIST.md](SECURITY_CHECKLIST.md) antes de contribuir. Código produzido ou alterado por IA é tratado como não confiável até passar pelas verificações aplicáveis.

## Contribuição

Consulte [CONTRIBUTING.md](CONTRIBUTING.md). Alterações devem ser feitas por branch e pull request; mudanças diretas na branch principal não fazem parte do fluxo normal de desenvolvimento.

## Licença

Código disponibilizado sob a [Apache License 2.0](LICENSE), salvo indicação específica em arquivos de terceiros.
