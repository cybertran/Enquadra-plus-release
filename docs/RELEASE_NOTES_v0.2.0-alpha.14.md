# Enquadra+ v0.2.0-alpha.14

## Correções

- Ao pesquisar um **código de enquadramento exato**, a ficha correspondente aparece primeiro.
- Ao pesquisar o **número de um artigo do CTB**, as fichas referentes àquele artigo aparecem antes de resultados de outros artigos. Exemplo: `180` prioriza `537-10 — Art. 180`.
- Diferencia artigos com sufixo: `180` não é considerado correspondência exata de `180-A`.
- Permite pesquisar um código completo também sem hífen, como `53710`.
- Mantém a regra já aprovada de priorização pela tipificação para pesquisas textuais.
- Preserva a correção da alpha.13: **Consulta rápida** abre Infrações com o campo de pesquisa focado e solicitação imediata do teclado.

## Validação

- Testes de regressão para código/artigo exatos, prefixos, sufixos de artigo, separadores e pesquisa textual.
- Sem alteração das fichas jurídicas, suas fontes oficiais, permissões ou dependências.

## Publicação oficial

O GitHub CI produz apenas APK **não assinado**. A publicação como atualização requer assinatura local pela mesma chave oficial da alpha.10+, conferência do certificado e SHA-256 e somente então promoção de `app-update.json`.
