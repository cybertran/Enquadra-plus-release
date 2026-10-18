# Contribuindo com o Enquadra+

Obrigado pelo interesse em contribuir.

## Fluxo Git

Não faça alterações diretamente em `main`. Crie uma branch específica, mantenha a mudança pequena e revisável e abra um pull request.

Antes do merge, revise o diff e confirme que nenhum arquivo sensível, credencial, dado privado ou material de assinatura foi incluído.

## Validação obrigatória

Quando o código Android estiver presente, uma mudança só deve ser considerada pronta após as verificações aplicáveis: testes, build, Android lint, security policy gate, secret scan, dependency review e CodeQL.

Falhas de segurança não devem ser contornadas desativando controles, reduzindo validações ou ignorando testes.

## Segurança

Nunca envie:

- keystores, chaves privadas ou certificados privados;
- senhas, tokens, chaves de API ou credenciais de nuvem;
- arquivos `.env` com segredos;
- `local.properties`, `google-services.json` ou configurações locais sensíveis;
- dumps, logs ou fixtures contendo dados privados.

Toda entrada externa deve ser tratada como não confiável. Alterações em rede, arquivos, banco, permissões, criptografia, atualização normativa e armazenamento exigem revisão de segurança adicional.

## Dependências

Evite dependências novas quando a plataforma já resolver o problema. Ao adicionar ou atualizar uma dependência, justifique a necessidade e verifique manutenção, origem, licenciamento e vulnerabilidades conhecidas.

## Código gerado por IA

Código gerado ou modificado por IA não é considerado confiável por padrão. Ele precisa passar pelos mesmos testes, revisão e verificações de segurança que código escrito manualmente.

## Vulnerabilidades

Não abra issue pública com detalhes exploráveis, segredos ou material que facilite abuso. Siga [SECURITY.md](SECURITY.md).
