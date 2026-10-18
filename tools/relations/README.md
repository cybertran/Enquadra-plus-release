# Relações entre enquadramentos

Este pipeline gera o dataset local de relações operacionais a partir do MBFT já revisado do Enquadra+.

As regras automáticas são deliberadamente conservadoras. Somente fichas `VERIFIED` podem originar relações oficiais automáticas, e somente quando o código relacionado aparece explicitamente no texto do campo oficial.

Tipos gerados:

- `ALTERNATIVE`: referência explícita no campo `whenNotToCite`;
- `CONCOMITANT`: procedimento com indicação explícita de autuação concomitante, outro AIT ou equivalente;
- `PREREQUISITE`: procedimento que exige autuação precedente;
- `RELATED`: relação curada já existente no projeto e não coberta por uma regra oficial explícita.

O dataset registra a versão exata do MBFT de origem, hash canônico e evidência textual. Referências genéricas do parser, sem regra operacional clara, não são promovidas automaticamente. Códigos citados em regras de alta confiança mas ausentes do catálogo local são registrados no relatório como referências não resolvidas e não viram relações por aproximação. Códigos citados em regras de alta confiança mas ausentes do catálogo local são registrados no relatório como referências não resolvidas e não viram relações por aproximação.

Validação local:

    python tools/relations/relations_pipeline.py build --mbft app/src/main/assets/mbft/infractions.json --output relations.json --report relations-report.json

    python tools/relations/relations_pipeline.py validate --mbft app/src/main/assets/mbft/infractions.json --dataset relations.json
