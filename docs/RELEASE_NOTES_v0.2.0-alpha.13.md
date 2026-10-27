# Enquadra+ v0.2.0-alpha.13

Hotfix de experiência de uso na **Consulta rápida** da tela inicial.

## Correção

- Ao tocar em **Consulta rápida**, o app abre **Infrações** com o campo de pesquisa já focado.
- O teclado é solicitado imediatamente, eliminando a necessidade de um segundo toque antes de digitar.
- A abertura normal de **Infrações** pela navegação inferior continua sem forçar o teclado.
- Os atalhos **Consultas recentes** e **Favoritos** também preservam o comportamento normal, sem foco automático.

## Escopo técnico

A correção usa um pedido de foco de uso único na navegação Home → Infrações. Depois que o campo recebe foco, o pedido é consumido para não reaparecer em navegações posteriores.

Não foram adicionadas dependências, permissões, chamadas de rede ou alterações na base jurídica.

## Distribuição

A versão mantém a mesma cadeia oficial de assinatura iniciada na alpha.10.

Fingerprint SHA-256 esperado do certificado:

```text
36:37:7E:0A:E2:63:B8:ED:79:B2:A9:1F:C4:A4:82:69:34:BC:5B:9D:FB:D4:4E:B7:B9:3D:6F:01:7A:2B:F0:53
```

O `app-update.json` só deve ser promovido para a alpha.13 depois que o APK oficial estiver assinado, publicado e tiver o SHA-256 validado.
