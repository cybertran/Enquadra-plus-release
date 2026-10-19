# Auditoria MBFT — promoção das quatro fichas remanescentes — 17/10/2026

## Escopo

Este lote encerra as quatro fichas que permaneceram em `PARTIAL_VALIDATION` depois da reconstrução independente do catálogo oficial por cabeçalhos.

Foram revisadas:

- `594-01` — art. 203, III;
- `608-41` — art. 211;
- `640-80` — art. 221;
- `721-80` — art. 248.

A fonte primária do dataset continua sendo o MBFT / Resolução CONTRAN nº 985/2022:
https://www.gov.br/transportes/pt-br/assuntos/transito/conteudo-contran/resolucoes/mbvt20222.pdf

Como apoio à conferência individual das fichas, foram usados também os arquivos oficiais aprovados do Participa + Brasil:
- 594-01: https://www.gov.br/participamaisbrasil/blob/baixar/6502
- 608-41: https://www.gov.br/participamaisbrasil/blob/baixar/15882
- 640-80: https://www.gov.br/participamaisbrasil/blob/baixar/16740
- 721-80: https://www.gov.br/participamaisbrasil/blob/baixar/16448

## Decisões

### 594-01

A extração havia incorporado ao primeiro item de `whenToCite` um trecho pertencente a definições/procedimentos e havia acrescentado o fragmento espúrio `eículo` ao segundo exemplo de observação do AIT.

Foram preservados os demais campos e corrigidos apenas `whenToCite` e `aitObservations`.

### 608-41

Os campos estruturados extraídos conferem com a ficha oficial. A ficha foi promovida sem alteração semântica.

### 640-80

A extração tabular deslocou conteúdo de procedimentos para `administrativeMeasure` e perdeu a pontuação.

A conferência do raw table do PDF oficial vigente confirmou:
- `points`: não computável (`null` no schema);
- `verificationMethod`: `Vide Definições e Procedimentos.`;
- `administrativeMeasure`: `Retenção do veículo para regularização e apreensão das placas irregulares.`.

A ficha individual do Participa + Brasil apresenta metadados diferentes em alguns desses campos. Por isso, quando houve divergência, prevaleceu o MBFT vigente baixado pelo pipeline e identificado pelo SHA-256 da fonte. As listas operacionais oriundas desse PDF também foram preservadas.

### 721-80

Os campos estruturados extraídos conferem com a ficha oficial. A ficha foi promovida sem alteração semântica.

## Resultado esperado

Depois da aplicação deste patch de baseline, a reconstrução deve produzir 415 fichas e nenhuma ficha em `PARTIAL_VALIDATION`.

A promoção para o asset de produção só pode ocorrer depois de o pipeline confirmar novamente igualdade exata do conjunto de códigos, ausência de duplicidades, validações estruturais, aplicação das emendas conhecidas e nenhuma emenda pendente.
