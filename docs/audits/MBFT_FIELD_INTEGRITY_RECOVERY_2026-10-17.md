# Auditoria MBFT — integridade de campos escalares — 17/10/2026

## Escopo

Correção restrita a três campos de penalidade cuja contaminação por coluna vizinha foi confirmada na fonte oficial do MBFT.

## Registros corrigidos

- `606-85`: Penalidade = `Multa`.
- `641-60`: Penalidade = `Multa`.
- `776-50`: Penalidade = `Multa e suspensão do direito de dirigir.`.

Fonte primária: Manual Brasileiro de Fiscalização de Trânsito - MBFT / Resolução CONTRAN nº 985/2022:
https://www.gov.br/transportes/pt-br/assuntos/transito/conteudo-contran/resolucoes/mbvt20222.pdf

Nenhum outro campo dessas fichas foi alterado neste lote.

## Prevenção de regressão

O validador do pipeline passa a rejeitar campos escalares que contenham rótulos estruturais de outra coluna do MBFT, como `Tipificação do Enquadramento:`, `Penalidade:`, `Medida Administrativa:` e equivalentes.

A correção também foi registrada como patch `BASELINE` para sobreviver a reconstruções futuras do dataset sem ser tratada como emenda normativa.

## Limite

Este lote não resolve outras anomalias heurísticas apontadas pela auditoria. Elas permanecem para revisão individual e não foram alteradas por aproximação.
