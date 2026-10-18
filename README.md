# Enquadra+

Repositório público e open source do Enquadra+, aplicativo Android para consulta rápida de informações oficiais de trânsito e apoio à fiscalização.

## Estado

O código-fonte da versão **v0.2.0-alpha.8** foi importado para revisão pública com histórico Git limpo, após sanitização do snapshot de desenvolvimento.

O projeto usa fontes públicas e oficiais, como CTB, MBFT, Resoluções CONTRAN, atos da Senatran e demais normas aplicáveis. O Enquadra+ é independente e não incorpora análises jurídicas, comentários, resumos de jurisprudência ou outros conteúdos autorais de produtos de terceiros.

A arquitetura é Android nativo com Kotlin, Jetpack Compose e Material Design 3, com funcionamento local-first e pesquisa offline. O roteiro funcional está em [docs/FUNCTIONAL_ROADMAP.md](docs/FUNCTIONAL_ROADMAP.md).

## Documentação

Consulte [docs/PRODUCT.md](docs/PRODUCT.md), [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md), [docs/DATA_MODEL.md](docs/DATA_MODEL.md), [docs/SOURCES.md](docs/SOURCES.md), [docs/IMPORTER.md](docs/IMPORTER.md) e [docs/SAFE_NORMATIVE_UPDATE_DOSSIER.md](docs/SAFE_NORMATIVE_UPDATE_DOSSIER.md).

O processo da primeira importação pública está registrado em [docs/OPEN_SOURCE_IMPORT.md](docs/OPEN_SOURCE_IMPORT.md).

## Builds oficiais

O código-fonte pode ser compilado por qualquer pessoa. Isso não transforma uma build de terceiros em build oficial do Enquadra+.

A identidade criptográfica oficial do aplicativo é verificada pelo certificado de assinatura. Fingerprint SHA-256 oficial:

```text
BE:58:EA:ED:F7:AD:76:B6:5F:75:FC:08:6A:DC:71:89:F4:91:A7:25:67:B5:8F:E7:BB:F3:EA:DA:3A:2C:93:A9
```

A chave privada de assinatura e sua senha não fazem parte deste repositório e não devem ser enviadas em issues, pull requests, workflows ou commits.

O fluxo público gera apenas um candidato de release **não assinado**. A assinatura oficial continua sendo feita fora do GitHub com a chave privada mantida offline. Depois da publicação da primeira release oficial, o arquivo para usuários será sempre chamado `Enquadra.apk`, permitindo usar o endereço permanente:

```text
https://github.com/cybertran/Enquadra-plus-release/releases/latest/download/Enquadra.apk
```

O procedimento completo está em [docs/RELEASING.md](docs/RELEASING.md).

## Segurança

Leia [SECURITY.md](SECURITY.md), [SECURITY_BASELINE.md](SECURITY_BASELINE.md) e [SECURITY_CHECKLIST.md](SECURITY_CHECKLIST.md) antes de contribuir. Código produzido ou alterado por IA é tratado como não confiável até passar pelas verificações aplicáveis.

## Contribuição

Consulte [CONTRIBUTING.md](CONTRIBUTING.md). Alterações devem ser feitas por branch e pull request; mudanças diretas na branch principal não fazem parte do fluxo normal de desenvolvimento.

## Licença

Código disponibilizado sob a [Apache License 2.0](LICENSE), salvo indicação específica em arquivos de terceiros.
