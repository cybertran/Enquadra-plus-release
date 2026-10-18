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
- [x] Dependency graph do GitHub habilitado para tornar o Dependency Review operacional.
- [x] Dependabot configurado para Gradle e GitHub Actions.
- [x] CodeQL configurado para Java/Kotlin.
- [x] Workflows públicos usam runners hospedados pelo GitHub.
- [x] Workflows de bootstrap usam permissões mínimas.
- [x] Actions de terceiros no bootstrap estão fixadas por commit SHA.
- [x] Ruleset/proteção de `main` configurado no GitHub.
- [x] Private vulnerability reporting habilitado nas configurações do GitHub.
- [x] Código sanitizado importado do repositório privado.
- [x] Build Android aprovado após a importação do código.
- [x] Testes unitários aprovados após a importação do código.
- [x] Android lint aprovado após a importação do código.
- [x] Scanner de segredos aprovado após a importação do código.
- [x] Dependency Review aprovado após a importação do código.
- [x] CodeQL aprovado após a importação do código.
- [x] Revisão final confirmou ausência de material de assinatura no histórico público.

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
