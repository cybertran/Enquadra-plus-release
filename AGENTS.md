# AGENTS.md — Diretrizes de Desenvolvimento

## Regra fundamental

Todo código criado ou modificado por IA deve ser considerado não confiável
até ser validado.

Não considerar uma implementação concluída apenas porque compila ou
aparenta funcionar.

## Antes de modificar o projeto

Antes de realizar alterações:

1. Leia este AGENTS.md.
2. Consulte a documentação canônica, especificações, ADRs e demais
   instruções existentes no repositório.
3. Entenda a arquitetura e as convenções já utilizadas.
4. Verifique o estado atual do projeto e os testes existentes.

Não contradiga silenciosamente decisões documentadas.

Quando uma solicitação exigir mudança arquitetural ou de comportamento
documentado, identifique a divergência e atualize a documentação
correspondente junto da implementação.

## Git e mudanças

Quando houver fluxo Git disponível:

- Não faça alterações diretamente na branch principal.
- Crie uma branch específica para a alteração.
- Prefira mudanças pequenas, isoladas e revisáveis.
- Não misture refatorações não relacionadas com novas funcionalidades.
- Revise o diff antes de considerar a alteração concluída.

Não sobrescreva trabalho existente sem necessidade.

## Segurança

Nunca:

- coloque senhas, tokens, chaves de API ou outras credenciais no código;
- faça commit de arquivos .env contendo segredos;
- desative autenticação ou autorização para fazer uma funcionalidade funcionar;
- aceite certificados TLS inválidos;
- desative validações ou mecanismos de segurança apenas para fazer testes passarem;
- introduza permissões desnecessárias;
- registre segredos ou dados sensíveis em logs.

Trate toda entrada externa como não confiável.

Verifique especialmente:

- autenticação;
- autorização;
- validação de entradas;
- injeções;
- manipulação de arquivos;
- upload e importação de documentos;
- caminhos de arquivos;
- armazenamento;
- comunicação de rede;
- criptografia;
- permissões;
- exposição de dados;
- logs;
- dependências;
- segredos e credenciais.

Use queries parametrizadas em vez de construir comandos SQL por
concatenação.

Para arquivos recebidos externamente, valide conteúdo, tamanho, estrutura
e tipo real quando aplicável, e não confie apenas na extensão do arquivo.

## Dependências

Evite adicionar novas dependências quando a plataforma ou as bibliotecas
já existentes resolverem adequadamente o problema.

Antes de adicionar uma dependência:

- verifique sua necessidade;
- verifique manutenção e origem;
- considere o impacto de segurança;
- evite versões conhecidamente vulneráveis.

Não atualize dependências importantes sem verificar compatibilidade e
testes.

## Validação obrigatória

Antes de considerar uma implementação concluída, execute todas as
verificações disponíveis e aplicáveis ao projeto, incluindo:

- build;
- testes unitários;
- testes de integração quando existentes;
- lint;
- análise estática;
- scanner de segredos;
- scanner de dependências;
- verificações específicas de segurança da plataforma.

Não ignore uma falha apenas para concluir a tarefa.

Se uma verificação falhar, determine se a alteração introduziu o problema
antes de prosseguir.

## Código produzido por IA

Código produzido ou modificado por IA deve receber o mesmo nível de
validação de código produzido manualmente.

A IA não deve:

- inventar APIs ou comportamentos de bibliotecas;
- assumir requisitos ausentes;
- apagar testes para eliminar falhas;
- enfraquecer verificações de segurança;
- alterar arquitetura silenciosamente;
- ocultar falhas encontradas durante a implementação.

Quando houver dúvida relevante sobre uma API, comportamento de segurança
ou dependência, consulte a documentação oficial correspondente.

## Conclusão da tarefa

Ao concluir uma alteração, informe:

- o que foi alterado;
- quais testes foram executados;
- resultado do build;
- resultado do lint;
- quais verificações de segurança foram executadas;
- quais verificações não puderam ser executadas;
- riscos residuais conhecidos.

Não declare que o código está seguro quando verificações relevantes não
foram realizadas.

<!-- SECURITY-BASELINE:START -->
## Baseline de segurança
Este projeto segue a baseline canônica v1.1 de `cybertran/Dev-references`. Antes de alterar autenticação, autorização, APIs, banco, arquivos, rede, criptografia, segredos, permissões, dependências, armazenamento privado ou dados sensíveis, leia `SECURITY_BASELINE.md`, `SECURITY_PROFILE.yml` e `SECURITY_CHECKLIST.md`; trabalhe fora da principal; trate código de IA como não confiável até validação; execute testes/build/lint/scans disponíveis; faça revisão adversarial; não enfraqueça controles para passar testes; e reporte verificações executadas, não executadas e riscos residuais. Vulnerabilidades críticas ou altas conhecidas sem mitigação bloqueiam release.
<!-- SECURITY-BASELINE:END -->
