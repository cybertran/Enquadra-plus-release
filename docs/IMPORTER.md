# Importador do MBFT

A partir da v0.2.0, as fichas do MBFT deixam de ser cadastradas diretamente em Kotlin.

## Fonte de dados

O catálogo embarcado fica em:

`app/src/main/assets/mbft/infractions.json`

O arquivo possui uma versão própria, data de geração, data de verificação, documento-base, URL oficial e lista de alterações normativas conhecidas. O lote atual é um conjunto de validação do pipeline, não representa ainda o MBFT completo ou consolidado.

## Fluxo

Na inicialização, o `MbftAssetImporter` lê o arquivo JSON e compara sua versão com `dataset_metadata`.

Se a versão já estiver instalada, nenhuma escrita é feita.

Se houver uma versão nova, o catálogo normativo é substituído dentro de uma única transação Room. A atualização inclui fichas, notas, relações, fontes e o índice de busca.

## Busca

A v0.2 usa SQLite FTS4 com tokenizer Unicode. O índice inclui:

- código de enquadramento;
- artigo;
- tipificação resumida;
- tipificação completa;
- quando autuar;
- quando não autuar;
- definições e procedimentos;
- exemplos de observação do AIT;
- códigos relacionados.

A entrada do usuário é tokenizada antes de ser enviada ao operador `MATCH`, evitando passar sintaxe FTS arbitrária diretamente ao SQLite.

## Migração v0.1 → v0.2

Como a v0.1 contém apenas dados normativos embarcados e ainda não possui dados pessoais do usuário, a versão 2 do banco usa migração destrutiva. O banco é recriado e imediatamente repopulado pelo importador.

Antes de favoritos, histórico persistente ou outros dados do usuário entrarem no mesmo banco, essa estratégia deve ser substituída por migrações Room explícitas.

## Validação normativa

A Resolução CONTRAN nº 985/2022 aprovou o MBFT e a página oficial do CONTRAN registra alterações posteriores, inclusive pelas Resoluções nº 1003/2023, 1009/2024, 1012/2024, 1013/2024, 1022/2026, 1027/2026 e 1031/2026. A alteração promovida pela Resolução nº 1031/2026 no Anexo do MBFT tem vigência prevista para 60 dias após a publicação de 18/08/2026.

Cada ficha importada mantém sua própria referência de origem e data de verificação. Novos lotes só devem ser publicados depois de conferir as alterações normativas que possam afetar as fichas incluídas.


## Evolução para atualização autônoma

O importador atual descreve a carga do asset MBFT empacotado no APK. A evolução para atualização direta pelo próprio aplicativo não poderá reutilizar esse fluxo como sobrescrita da base ativa.

O mecanismo futuro deverá seguir integralmente [SAFE_NORMATIVE_UPDATE_DOSSIER.md](SAFE_NORMATIVE_UPDATE_DOSSIER.md): fonte oficial direta, parser versionado, geração candidata isolada, diff obrigatório, validação fail-closed, promoção atômica e rollback. Mudança de formato que o parser instalado não consiga provar como compatível deve bloquear a atualização em vez de tentar adaptação heurística.
