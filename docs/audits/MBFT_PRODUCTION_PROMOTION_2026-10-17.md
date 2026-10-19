# Promoção do dataset MBFT para produção — 17/10/2026

## Artefato promovido

O arquivo `.mbft-staging/infractions.generated.json` validado pelo pipeline foi promovido integralmente para:

`app/src/main/assets/mbft/infractions.json`

Não houve reconstrução manual do JSON durante a promoção.

## Provas antes da promoção

- fichas geradas: 415;
- fichas `VERIFIED`: 415;
- `OFFICIAL_EXPECTED_CODES`: 415;
- `GENERATED_CODES`: 415;
- `OFFICIAL_EXPECTED_CODES - GENERATED_CODES`: vazio;
- `GENERATED_CODES - OFFICIAL_EXPECTED_CODES`: vazio;
- códigos duplicados: 0;
- emendas pendentes: 0;
- emendas não parseadas: 0;
- Resolução CONTRAN nº 1.031/2026 aplicada com vigência em 17/10/2026.

## Consequências esperadas

A alteração do asset deve disparar novamente:
- pipeline de relações entre infrações;
- pipeline de texto integral e relações normativas;
- Android CI, incluindo validação de assets, testes unitários, lint, build e validação do APK.

A promoção não autoriza merge enquanto esses gates não estiverem concluídos com sucesso.
