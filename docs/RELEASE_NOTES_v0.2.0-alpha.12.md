# Enquadra+ v0.2.0-alpha.12

Hotfix da v0.2.0-alpha.11 para corrigir o fluxo de feedback do aplicativo.

## Correções

- Corrige o link público do formulário **Feedback Enquadra+** aberto pela tela **Sobre**.
- Corrige o identificador do campo **Dados técnicos do app** para o formulário atualmente publicado.
- Mantém o envio de UUID aleatório por instalação e versão/build apenas após ação do usuário.
- Adiciona teste de regressão para fixar o vínculo entre o app e o formulário público correto.

## Segurança e privacidade

A hotfix não altera a política de privacidade do feedback: o e-mail permanece opcional, o identificador técnico é gerado pelo próprio app e não usa `ANDROID_ID`, IMEI, Advertising ID, telefone ou conta Google.

A versão mantém a mesma cadeia oficial de assinatura iniciada na alpha.10.

Fingerprint SHA-256 esperado do certificado:

```text
36:37:7E:0A:E2:63:B8:ED:79:B2:A9:1F:C4:A4:82:69:34:BC:5B:9D:FB:D4:4E:B7:B9:3D:6F:01:7A:2B:F0:53
```

## Compatibilidade

A atualização **alpha.11 → alpha.12** deve instalar normalmente por cima da alpha.11, mantendo a mesma cadeia de assinatura.
