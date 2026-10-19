# Implementação do gate de cobertura MBFT — 17/10/2026

## Resultado

Foi implementada a prova reproduzível:

`OFFICIAL_EXPECTED_CODES == GENERATED_CODES`

A referência esperada é construída independentemente do dataset gerado, a partir dos cabeçalhos do PDF oficial do MBFT e dos patches normativos auditados.

## Estado promovido

- MBFT em produção na branch: `2026-10-17-generated`;
- fichas: 415;
- fichas `VERIFIED`: 415;
- conjunto oficial esperado: 415;
- conjunto gerado: 415;
- faltantes: 0;
- inesperadas: 0;
- duplicadas: 0;
- emendas pendentes: 0.

## Correções incluídas

- reconhecimento de `Código do Enquadramento`, `Código de Enquadramento` e variante oficial sem conector;
- Resolução CONTRAN nº 1.031/2026 aplicada na data de vigência;
- inclusão/atualização de `516-91`, `516-92`, `757-91`, `757-92` e remoção de `757-90`;
- recuperação e revisão de `594-01`, `608-41`, `640-80` e `721-80`;
- correção dos campos de penalidade de `606-85`, `641-60` e `776-50`;
- validação contra vazamento de rótulos de colunas para campos escalares;
- atos normativos suspensos passam a ser excluídos dos vínculos de infração.

## Prova estática

`tools/audit/validate_safe_update.py` agora verifica também:

- contagem 415/415 do gate de códigos;
- ausência de faltantes, inesperados e duplicados;
- todas as 415 fichas como `VERIFIED`;
- presença dos códigos recuperados e ausência de `757-90`;
- campos corrigidos de penalidade e da ficha `640-80`;
- presença do comparador no pipeline MBFT;
- ausência de vínculos para atos normativos com estado revogado, sem efeito, cancelado ou suspenso.

## Limites deliberados

`744-71` não foi criado como ficha MBFT porque não aparece no conjunto de cabeçalhos oficiais vigente. Ele permanece como referência operacional não resolvida a ser tratada em modelo próprio, sem inventar uma ficha.

Referências não resolvidas remanescentes continuam preservadas para revisão individual; não foram substituídas por aproximação heurística.
