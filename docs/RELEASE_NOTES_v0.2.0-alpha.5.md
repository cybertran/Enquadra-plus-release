# Enquadra+ v0.2.0-alpha.5

Quinta prévia da v0.2 e hotfix de inicialização sobre a alpha.4.

## Correção principal

- Corrige um caminho em que uma falha durante o bootstrap inicial de datasets ou do agendamento de atualizações podia encerrar o processo logo após a abertura do app.
- O bootstrap agora registra a falha e mantém o aplicativo aberto, preservando o comportamento de fail-closed dos dados.
- `CancellationException` continua sendo propagada corretamente.
- Foram adicionados testes unitários específicos para impedir regressão desse comportamento.

## Base funcional

Mantém as funcionalidades consolidadas até a v0.19: pesquisa de infrações e MBFT, CTB local, catálogo normativo e texto integral offline, relações entre enquadramentos, valores/filtros/favoritos/histórico/anotações, ferramentas operacionais, 9 volumes de sinalização, PDFs oficiais e atualizador normativo seguro.

## Segurança e validação

A build candidata passa pelos validadores de datasets e guardrails, testes unitários, Android lint, build do APK, verificação ZIP/zipalign/assinatura e Security Policy Gate.

## Sobre a alpha.4

A alpha.4 apresentou encerramento logo após abrir no teste em dispositivo e foi substituída por esta alpha.5. Para testar o app, use a alpha.5 ou posterior.

## APK

O APK anexado é um **debug APK** de pré-release para testes. Ele é compilado a partir do commit-alvo da release pelo workflow controlado e validado antes do upload.
