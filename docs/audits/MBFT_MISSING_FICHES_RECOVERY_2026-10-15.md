# Auditoria MBFT — recuperação de fichas ausentes — 15/10/2026

## Escopo

Esta auditoria trata uma parte da issue #7, sem encerrar a investigação completa de referências ausentes.

Foram recuperadas três fichas vigentes que estavam ausentes do catálogo local:

- `551-71` — art. 181, XIV;
- `566-50` — art. 182, X;
- `567-31` — art. 183.

## Evidência oficial

As três fichas foram conferidas individualmente em documentos oficiais do Participa Brasil/gov.br identificados como aprovados:

- 551-71: https://www.gov.br/participamaisbrasil/blob/baixar/6392
- 566-50: https://www.gov.br/participamaisbrasil/blob/baixar/6456
- 567-31: https://www.gov.br/participamaisbrasil/blob/baixar/6458

A existência desses enquadramentos também é consistente com a relação oficial de códigos do Volume I e com referências cruzadas presentes em outras fichas do MBFT.

## Promoção

As fichas foram incluídas como `VERIFIED` por patch versionado em:

`tools/mbft/amendments/001-baseline-recovery-2026-10-15.json`

O catálogo de produção passou de 405 para 408 registros. A versão do dataset foi alterada para `2026-10-15-generated`, sem alterar retroativamente a data global `verifiedAt` de 13/10/2026; cada nova ficha registra `lastCheckedAt: 2026-10-15`.

## Relações

O dataset de relações foi reconstruído contra a nova versão do MBFT. O total passou de 1173 para 1186 relações e as referências não resolvidas caíram de 42 para 34.

Os códigos `551-71`, `566-50` e `567-31` não permanecem na lista de referências não resolvidas.

Não foi criado nenhum vínculo por aproximação ou substituição heurística.

## Pendências da issue #7

Continuam sem promoção neste lote:

- `516-91`;
- `519-30`;
- `640-80`;
- `744-71`.

Códigos classificados como históricos, revogados ou possíveis erros textuais continuam fora do catálogo até conclusão individual da auditoria.

## Validação

O pipeline de relações foi executado com sucesso no runner dedicado. O pipeline MBFT e o Android CI completo são requisitos para conclusão deste lote; seus resultados devem ser registrados no PR antes de merge.
