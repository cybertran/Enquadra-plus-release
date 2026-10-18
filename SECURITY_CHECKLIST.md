# Checklist de Segurança — Enquadra+ público

Este checklist registra o estado de segurança do repositório público. Itens específicos de uma mudança devem ser revistos em cada pull request.

## Bootstrap do repositório

- [x] Repositório público separado do histórico privado de desenvolvimento.
- [x] Bootstrap realizado em branch dedicada, não diretamente em `main`.
- [x] `.gitignore` bloqueia keystores, chaves privadas, arquivos de credenciais e ambientes locais.
- [x] Chave oficial de assinatura e senha permanecem fora do Git.
- [x] Fingerprint público do certificado oficial documentado.
- [x] Política de segurança adicionada.
- [x] Regras de contribuição adicionadas.
- [x] Baseline de segurança adicionada.
- [x] Security policy gate configurado.
- [x] Secret scan especializado configurado.
- [x] Dependency Review configurado.
- [x] Dependabot configurado para Gradle e GitHub Actions.
- [x] CodeQL configurado para Java/Kotlin.
- [x] Workflows públicos usam runners hospedados pelo GitHub.
- [x] Workflows de bootstrap usam permissões mínimas.
- [x] Actions de terceiros no bootstrap estão fixadas por commit SHA.
- [ ] Ruleset/proteção de `main` configurado no GitHub.
- [ ] Private vulnerability reporting habilitado nas configurações do GitHub.
- [ ] Código sanitizado importado do repositório privado.
- [ ] Build Android aprovado após a importação do código.
- [ ] Testes unitários aprovados após a importação do código.
- [ ] Android lint aprovado após a importação do código.
- [ ] Scanner de segredos aprovado após a importação do código.
- [ ] Dependency Review aprovado após a importação do código.
- [ ] CodeQL aprovado após a importação do código.
- [ ] Revisão final confirmou ausência de material de assinatura no histórico público.

## Revisão por mudança

- [ ] Mudança feita fora de `main`.
- [ ] Diff revisado.
- [ ] Nenhum segredo ou dado privado adicionado.
- [ ] Entradas externas validadas.
- [ ] Arquivos e caminhos revisados quando aplicável.
- [ ] Rede e TLS revisados quando aplicável.
- [ ] Permissões Android revisadas quando aplicável.
- [ ] Armazenamento e backup revisados quando aplicável.
- [ ] Dependências revisadas quando aplicável.
- [ ] Testes/build/lint executados quando aplicável.
- [ ] Riscos residuais documentados.
