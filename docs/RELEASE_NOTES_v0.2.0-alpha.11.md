# Enquadra+ v0.2.0-alpha.11

Pré-release focada no novo fluxo de feedback do aplicativo.

## Principais mudanças

- Nova ação **Enviar feedback** na tela **Sobre**.
- O formulário do projeto é aberto no Google Forms somente após ação do usuário.
- O feedback pode ser enviado sem identificação pessoal obrigatória; o e-mail permanece opcional.
- Quando aberto pelo app, o formulário recebe um identificador aleatório próprio da instalação e a versão/build do Enquadra+ para ajudar no diagnóstico.
- O identificador técnico é um UUID gerado pelo próprio app e armazenado em `noBackupFilesDir`; não usa `ANDROID_ID`, IMEI, Advertising ID, telefone ou conta Google.
- Os dados técnicos aparecem em campo próprio, visível e editável no Google Forms; não são tratados como campo oculto nem como prova de identidade.
- O formulário permanece externo ao app e nenhuma consulta operacional é enviada automaticamente.

## Segurança e privacidade

A funcionalidade não adiciona login, conta obrigatória, permissão sensível ou servidor próprio. O identificador de instalação não é derivado de identificadores do sistema e é renovado após desinstalação/reinstalação.

A chave privada oficial continua fora do GitHub e do CI. Esta versão deve ser assinada com a mesma cadeia oficial iniciada na alpha.10.

Fingerprint SHA-256 esperado do certificado:

```text
36:37:7E:0A:E2:63:B8:ED:79:B2:A9:1F:C4:A4:82:69:34:BC:5B:9D:FB:D4:4E:B7:B9:3D:6F:01:7A:2B:F0:53
```

## Compatibilidade

A atualização **alpha.10 → alpha.11** mantém a mesma cadeia de assinatura e deve ser instalável por cima da alpha.10 normalmente, sujeita às proteções padrão do Android.
