# Auditoria final de integridade jurídica e de dados — 2026-10-17

## Escopo

Branch: `fix/mbft-semantic-integrity-2026-10-17`

Baseline auditada original: `c1b975b271a45f345c2e160a421a5cef64991200`

Este relatório encerra o dossiê de correção das inconsistências jurídicas e de dados identificadas no MBFT, CTB, relações, vínculos normativos e mecanismos de atualização segura.

## Resultado MBFT

- 415 fichas esperadas.
- 415 fichas geradas.
- 0 ausentes.
- 0 inesperadas.
- 0 códigos duplicados.
- 410 fichas em `VERIFIED`.
- 5 fichas em `SOURCE_CONFLICT`: 516-91, 516-92, 611-40, 757-91 e 757-92.
- 0 estados `PARTIAL_VALIDATION` ou `MANUAL_REVIEW_REQUIRED` em produção.
- 0 defeitos estruturais P0 detectados pelos gates semânticos.
- 18 divergências históricas entre dados antigos e fonte fresca foram todas encerradas: 2 `CORRECTED` e 16 `EXPLICIT_OVERRIDE`.
- Nenhuma divergência permanece sem resolução final.

A ficha 501-00 foi corrigida com `points = null`, correspondente a "Pontuação: Não Computável", e teve os campos operacionais recompostos com base na fonte oficial. A cauda corrompida `. possuir` foi removida.

## Conflitos de fonte preservados

As fichas 516-91, 516-92, 757-91 e 757-92 preservam a divergência interna da Resolução CONTRAN nº 1.031/2026 entre o campo de medida administrativa do Anexo III e o art. 12 do corpo da resolução.

A ficha 611-40 preserva a divergência entre o MBFT, que publica "Grave / 7 pontos", e o art. 259, II, do CTB, que estabelece 5 pontos para infração grave.

Esses casos não foram normalizados silenciosamente. Permanecem identificados como `SOURCE_CONFLICT`, com evidência, hash de fonte e justificativa de revisão.

## Relações entre infrações

O dataset passou de 1.231 para 1.604 relações após a recuperação de campos oficiais que estavam truncados ou resumidos na baseline anterior.

Comparação contra a baseline original:

- 398 relações oficiais adicionadas.
- 25 relações removidas.
- saldo líquido: +373.
- as 398 relações adicionadas têm origem `MBFT_OFFICIAL_FIELD`.
- 362 vieram de `whenNotToCite`.
- 36 vieram de `procedures`.
- as relações removidas incluem 16 relações curadas e 9 relações oficiais cuja evidência mudou após a reextração.
- 14 das 16 relações curadas removidas foram substituídas por relações oficiais extraídas diretamente do MBFT.
- as 2 relações curadas restantes não possuem suporte equivalente no conteúdo oficial corrigido e não foram preservadas artificialmente.

Distribuição final:

- ALTERNATIVE: 1.516
- CONCOMITANT: 59
- PREREQUISITE: 28
- RELATED: 1

O aumento foi aceito porque decorre de recuperação de texto oficial e não de duplicação de chaves.

## Referências não resolvidas

O relatório passou de 26 para 37 ocorrências não resolvidas.

O aumento decorre da recuperação de referências que já estavam no texto oficial, mas não apareciam na extração anterior. Elas permanecem explicitamente não resolvidas porque os códigos citados não pertencem ao conjunto oficial atual de 415 fichas.

Códigos citados sem ficha atual correspondente:

`527-43`, `606-83`, `628-32`, `640-30`, `664-30`, `676-91`, `676-93`, `682-02`, `693-02`, `693-03`, `693-30`, `696-40`, `705-62`, `712-91`, `724-20`, `744-71`, `773-31` e `773-32`.

Nenhuma ficha foi inventada para satisfazer essas referências. O sistema permanece fail-closed e mantém as pendências auditáveis.

## Catálogo normativo e full-text

- 3.204 atos catalogados.
- 3.131 atos com full-text extraído.
- 73 lacunas classificadas.
- 51: `LEGACY_DOC_EXTRACTION_FAILURE`.
- 22: `INDEX_ONLY_NO_DOCUMENT_URL`.
- cobertura global: 97,7216%.
- cobertura de PDFs: 100%.
- 1.352 vínculos infração × norma.
- 415/415 infrações auditadas pelo pipeline normativo.

O CI aceita somente as duas categorias de lacuna previamente auditadas acima. Qualquer categoria nova ou não classificada volta a bloquear a build.

## Gates e arquitetura

Foram implementados:

- comparação de registros `VERIFIED` contra a extração oficial atual;
- invalidação de `VERIFIED` quando a fonte diverge;
- evidência por ficha com hash da fonte;
- estados `CORRECTED`, `EXPLICIT_OVERRIDE` e `SOURCE_CONFLICT`;
- gates contra cabeçalhos de coluna em campo incorreto;
- gates contra `Art.` órfão, Unicode de substituição e frases truncadas;
- gate contra cauda corrompida de AIT;
- reconstrução de citações de artigo apoiada no CTB;
- preferência por células estruturadas da tabela oficial;
- proteção contra duplicação de fragmentos;
- bloqueio de aprovações históricas como solução automática de divergências novas;
- validação fail-closed do updater;
- alinhamento entre gerador Python e gerador Kotlin de relações, incluindo fichas `SOURCE_CONFLICT`.

## Verificações concluídas antes do bump alpha.9

No commit `b406f337c229091a73c2a0bb651ec1909a98b326`, o Android CI confirmou:

- CTB asset: PASS.
- catálogo normativo: PASS.
- full-text e vínculos normativos: PASS.
- sinalização: PASS.
- reconstrução de relações: PASS.
- validação de relações: PASS.
- tooling audit: PASS.
- safe normative updater: PASS.
- testes unitários: PASS.
- Android lint: PASS.
- build debug APK: PASS.
- integridade ZIP: PASS.
- zipalign: PASS.
- apksigner: PASS.

A publicação foi bloqueada exclusivamente pelo gate de versão porque o aplicativo ainda estava em `0.2.0-alpha.8`. Este commit altera a versão para `0.2.0-alpha.9` para a rodada final de CI.

## Riscos residuais aceitos

- 37 ocorrências de referências oficiais para códigos sem ficha atual correspondente permanecem visíveis e não resolvidas.
- 51 documentos legados `.doc` permanecem sem extração por falha do `antiword`.
- 22 atos estão disponíveis apenas no índice oficial, sem URL de documento integral.
- cinco fichas permanecem em `SOURCE_CONFLICT` por divergência existente na própria fonte oficial.

Esses itens são explicitamente representados e não são mascarados como conteúdo verificado sem ressalva.

## Validação final do candidato alpha.9

Commit de código/dados e binário auditado:

`f6532c8702f534df18d8f507a99b0c04f71b087f`

Versão:

- `versionName = 0.2.0-alpha.9`
- `versionCode = 10`

Android CI final:

- run: `39062761325`
- conclusão: `SUCCESS`

A rodada final repetiu com sucesso:

- validação do CTB;
- catálogo normativo;
- full-text e vínculos normativos;
- sinalização;
- reconstrução e validação das relações;
- tooling audit;
- safe normative updater;
- 61 testes unitários;
- Android lint;
- build debug;
- validação do APK;
- integridade ZIP;
- zipalign;
- apksigner;
- gate fail-closed;
- publicação do candidato.

APK publicado:

`Enquadra-debug-candidate.apk`

Tamanho:

`19.622.448 bytes`

SHA-256:

`44cac7fa2d6c190ea137280d4b05c1005b148d3b2d457f534089b4ce0ca77a6d`

Tag de CI:

`ci-fix-mbft-semantic-integrity-2026-10-17`

O commit deste relatório é apenas documental; o binário auditado corresponde ao commit de código/dados indicado acima.

## Decisão final

O candidato `0.2.0-alpha.9` satisfaz os gates técnicos e de integridade definidos para este ciclo e pode seguir para revisão humana em pull request.

Nenhum merge automático está autorizado.
