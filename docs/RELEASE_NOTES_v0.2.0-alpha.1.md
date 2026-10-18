# Enquadra+ v0.2.0-alpha.1

Primeira prévia da arquitetura de importação do MBFT no Enquadra+.

## Destaques

- Catálogo MBFT versionado em JSON dentro do app.
- Importação transacional para Room.
- Metadados de versão e rastreabilidade do conjunto normativo.
- Validação de catálogo antes da importação.
- Busca Full Text Search com SQLite FTS4 e tokenizer Unicode.
- Pesquisa por código, artigo, tipificação, procedimentos, observações e enquadramentos relacionados.
- Primeiras fichas usadas para validar o pipeline: 682-31, 682-32, 683-12 e 696-30.
- Fichas ainda incompletas aparecem com o selo “Em validação”.
- A ficha 682-32 permanece como registro verificado do lote inicial.

## Situação

Esta é uma versão **alpha** para validação da arquitetura e do fluxo de dados. O catálogo incluído ainda não representa o MBFT completo ou consolidado.

## Banco de dados

O schema Room passa para a versão 2. Nesta fase, a migração é destrutiva porque a v0.1 contém apenas dados normativos embarcados. Antes da inclusão de favoritos, histórico persistente ou outros dados do usuário no mesmo banco, serão adotadas migrações explícitas.

## Build

O projeto não foi validado por GitHub Actions porque a cota de Actions do repositório está esgotada. A publicação desta alpha representa o estado do código-fonte para testes e validação local.
