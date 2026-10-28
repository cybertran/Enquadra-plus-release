# Enquadra+ v0.2.0-alpha.15

## Correção — verificação de atualização do app

- Cada verificação consulta o manifesto público oficial usando uma URL única, reduzindo respostas desatualizadas de caches de CDN.
- A consulta solicita revalidação e não armazenamento por meio de cabeçalhos HTTP, mantendo HTTPS, host oficial fixo e redirecionamentos bloqueados.
- Se o servidor responder com uma versão **anterior** à instalada, o resultado passa a ser **inconclusivo** e não afirma incorretamente que o aplicativo está atualizado.
- Quando não há versão superior identificada, a tela mostra a versão oficial consultada para facilitar o diagnóstico.
- Os testes de regressão cobrem URL, endpoint permitido, nonce, versão nova, igual e anterior.

## Compatibilidade

Preserva as correções da alpha.14 (busca numérica e foco/teclado da Consulta rápida), sem modificar dados jurídicos ou incluir permissões novas.

## Publicação

Este arquivo descreve uma candidata em preparação, **não** uma atualização oficial já publicada. O CI gera apenas APK não assinado. A assinatura oficial é feita localmente com a chave da alpha.10+; só depois da release pública assinada e validada pode ocorrer a promoção de `app-update.json`. O manifesto permanece apontando para a alpha.14 até essa etapa.
