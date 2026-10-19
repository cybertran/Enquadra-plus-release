# Enquadra+ v0.2.0-alpha.9

Pré-release de correção de integridade jurídica e semântica dos dados.

## Principais mudanças

- Reauditoria completa das 415 fichas MBFT.
- 410 fichas verificadas e 5 conflitos de fonte explicitamente documentados.
- Correção da ficha 501-00 para pontuação não computável.
- Remoção de corrupção estrutural em campos de "Quando Autuar", "Quando NÃO Autuar", procedimentos e observações do AIT.
- Novo modelo fail-closed de verificação contra a fonte oficial atual.
- Evidência por ficha vinculada a hash da fonte.
- Relações entre infrações reconstruídas a partir dos campos oficiais recuperados.
- Catálogo normativo atualizado e lacunas de full-text classificadas.
- Gates adicionais para impedir regressão de dados jurídicos.
- Alinhamento entre derivação de relações em Python e Kotlin.

## Situação jurídica dos dados

O conjunto atual contém 415/415 fichas. Cinco fichas permanecem marcadas como conflito de fonte por divergências presentes na própria documentação oficial. Referências a códigos ausentes do conjunto atual permanecem explicitamente não resolvidas, sem criação automática de fichas.

## APK

O APK desta versão somente é publicado pelo CI após testes unitários, Android lint, validações de dados, build, verificação ZIP, zipalign e apksigner.
