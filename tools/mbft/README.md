# Pipeline MBFT

Esta ferramenta constrói o dataset de infrações do Enquadra+ a partir da fonte oficial.

O APK não lê o PDF em tempo de execução. O PDF é processado durante o desenvolvimento e o resultado usa o mesmo schema JSON já consumido por MbftAssetImporter. O funcionamento do app continua offline.

Fluxo:
1. baixa o PDF oficial indicado em source-manifest.json;
2. extrai automaticamente as fichas encontradas;
3. marca a extração automática como PARTIAL_VALIDATION;
4. sobrepõe as fichas já revisadas e VERIFIED;
5. aplica patches versionados das resoluções que alteraram o MBFT;
6. valida códigos, duplicidades, campos mínimos, fontes e quantidade;
7. compara, de forma independente, o conjunto de códigos dos cabeçalhos oficiais com o conjunto gerado; a regra obrigatória é `OFFICIAL_EXPECTED_CODES == GENERATED_CODES`;
8. somente depois o JSON gerado pode substituir o asset de produção.

O extrator nunca promove uma ficha para VERIFIED sozinho. Essa decisão exige conferência na fonte oficial.

Execução local no PowerShell:

    py -m pip install -r tools/mbft/requirements.txt

    py tools/mbft/mbft_pipeline.py extract --pdf MBVT20222.pdf --manifest tools/mbft/source-manifest.json --verified-overrides app/src/main/assets/mbft/infractions.json --patch-dir tools/mbft/amendments --output mbft-generated.json --report mbft-report.json --min-records 400

Validação do dataset atual:

    py tools/mbft/mbft_pipeline.py validate --dataset app/src/main/assets/mbft/infractions.json

Cada arquivo JSON em tools/mbft/amendments pode ter resolution, removeCodes e upserts. Os upserts usam o mesmo schema das fichas do asset. Uma alteração marcada como PATCH_REQUIRED no manifesto ainda não deve ser considerada integralmente incorporada.

## Integridade semântica e status VERIFIED

`VERIFIED` não é um congelamento permanente do registro. Em cada extração, o pipeline compara os campos jurídicos e operacionais da ficha armazenada com a extração corrente da fonte oficial.

- correspondência integral e sem anomalia estrutural: o registro pode permanecer `VERIFIED`;
- divergência entre fonte fresca e registro armazenado: `DIFF_REVIEW_REQUIRED`, representado em staging como `PARTIAL_VALIDATION`;
- correção manual: somente por `recordPatches` restrito aos campos revisados, com evidência de revisão e hash da fonte;
- cabeçalho de outra coluna, token órfão `Art.`, caractere Unicode de substituição ou texto truncado em `utilizar`: gate fail-closed.

Patches baseline não devem substituir uma ficha inteira quando a revisão abrange somente campos específicos. Full upserts ficam reservados a inclusão/remoção oficial de fichas ou a casos explicitamente auditados.

