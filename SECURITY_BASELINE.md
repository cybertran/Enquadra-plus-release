# Baseline Canônica de Segurança para Sistemas com Identidade e Dados

**Versão:** 1.1  
**Status:** canônica vigente  
**Data de consolidação:** outubro de 2026

> Esta versão consolida integralmente a v1.0 e acrescenta automação, alvo ASVS explícito, matriz verificável, bootstrap, gates de CI, regras de descoberta automática e evidência de segurança. É a versão canônica vigente para novos projetos.

## 1. Finalidade

Estabelecer requisitos mínimos obrigatórios de segurança para sistemas que armazenem dados, possuam contas de usuários, autenticação, APIs, bancos de dados, arquivos, permissões ou comunicação em rede.

Código funcional não é considerado código seguro. Código produzido ou modificado por inteligência artificial deve ser considerado não confiável até revisão e validação.

A segurança deve ser implementada por projeto e ao longo do ciclo de desenvolvimento, não adicionada apenas depois da funcionalidade pronta.

## 2. Base normativa

Referências primárias:

- OWASP Application Security Verification Standard 5.0.0.
- NIST SP 800-63-4 — Digital Identity Guidelines.
- NIST SP 800-218 — Secure Software Development Framework.
- OWASP API Security.
- OWASP Cheat Sheet Series.
- OWASP MASVS/MASWE para aplicativos móveis.

Como política desta baseline, sistemas com autenticação ou dados privados devem buscar, no mínimo, cobertura equivalente ao **OWASP ASVS Nível 2**. Sistemas de risco elevado devem avaliar requisitos do Nível 3.

## 3. Princípio fundamental

Nenhuma funcionalidade deve confiar no cliente.

Aplicativos Android, web, desktop e demais clientes devem ser tratados como ambientes potencialmente controláveis por um atacante.

Ocultar um botão não constitui autorização. Desabilitar um campo não constitui autorização. Não exibir um identificador não constitui proteção.

A autorização deve ser aplicada no backend, banco de dados ou outra camada confiável equivalente em cada operação sensível.

Toda informação proveniente do usuário, cliente, arquivo, API externa, cabeçalho HTTP, banco externo ou integração deve ser tratada inicialmente como não confiável.

## 4. Modelo de ameaça obrigatório

Antes da implementação de funcionalidades sensíveis, o projeto deve identificar:

- ativos a proteger;
- atores legítimos;
- possíveis atacantes;
- fronteiras de confiança;
- superfícies de ataque;
- consequências de comprometimento.

Devem ser considerados, no mínimo, roubo de credenciais, vazamento de banco, comprometimento de conta, usuário autenticado malicioso, alteração de identificadores, escalada de privilégios, abuso de APIs, vazamento de tokens, comprometimento de dependências, exposição de backups, arquivos maliciosos, ataques automatizados e comprometimento do dispositivo cliente.

Mudanças arquiteturais relevantes devem atualizar o modelo de ameaça.

## 5. Autenticação

Sempre que possível, use um provedor de autenticação consolidado ou implementação nativa bem mantida do framework em vez de criar um sistema próprio.

Não deve existir login baseado apenas em verificações realizadas no frontend.

Respostas de autenticação não devem permitir enumeração desnecessária de contas.

Tentativas de autenticação devem possuir proteção contra abuso automatizado, como rate limiting, controles progressivos ou mecanismo equivalente adequado ao risco.

Operações sensíveis podem exigir reautenticação.

Alterações de senha, e-mail, MFA, chaves ou credenciais devem exigir comprovação adequada de posse da conta.

Contas administrativas devem receber proteção superior às contas comuns.

## 6. Senhas

Senhas nunca devem ser armazenadas em texto puro.

Senhas nunca devem ser armazenadas por criptografia reversível.

Quando a aplicação for responsável pelo armazenamento de senhas, deve utilizar algoritmo específico para password hashing, preferencialmente Argon2id, ou alternativa adequada e consolidada para a plataforma.

Cada senha deve possuir salt adequado. Um pepper pode ser utilizado como defesa adicional se for armazenado separadamente do banco de hashes.

MD5, SHA-1, SHA-256 ou outros hashes rápidos usados isoladamente são inadequados para armazenamento de senhas.

A política de senha deve favorecer comprimento, bloqueio de senhas comprometidas e compatibilidade com gerenciadores de senha, evitando regras arbitrárias que reduzam usabilidade sem benefício real.

Para senhas usadas como único fator, siga os requisitos atuais aplicáveis do NIST SP 800-63B. Sistemas devem permitir senhas longas e não devem impor troca periódica sem evidência de comprometimento.

## 7. MFA e passkeys

Aplicações com dados sensíveis devem disponibilizar autenticação multifator quando o risco justificar.

Contas administrativas ou de privilégio elevado devem utilizar MFA sempre que tecnicamente possível.

Para ambientes de maior risco, devem ser priorizados mecanismos resistentes a phishing, como passkeys/FIDO2/WebAuthn.

SMS não deve ser tratado como o método mais forte disponível quando mecanismos resistentes a phishing forem viáveis.

## 8. Recuperação de conta

O fluxo de recuperação deve ser considerado tão sensível quanto o próprio login.

Tokens de recuperação devem ser aleatórios, imprevisíveis, de curta duração e de uso único.

O banco não deve armazenar tokens reutilizáveis em texto puro quando isso puder permitir tomada de conta em caso de vazamento.

A recuperação não deve depender de perguntas de segurança facilmente descobertas.

Ao trocar senha por recuperação, sessões anteriores devem poder ser invalidadas.

Alterações de endereço de e-mail ou método de recuperação devem gerar notificação para o endereço ou dispositivo anteriormente associado quando adequado.

Nenhum suporte administrativo deve contornar silenciosamente os controles de identidade sem trilha de auditoria.

## 9. Sessões e tokens

Identificadores de sessão devem ser criptograficamente aleatórios e imprevisíveis.

Tokens de acesso devem possuir vida útil compatível com o risco.

Tokens de atualização devem possuir mecanismos apropriados de rotação, revogação ou detecção de reutilização quando a arquitetura exigir.

Logout deve invalidar a sessão ou credencial correspondente quando aplicável.

Mudanças críticas de credenciais ou privilégios devem permitir invalidação de sessões existentes.

Cookies de autenticação web devem utilizar configurações adequadas de `Secure`, `HttpOnly` e `SameSite`.

Tokens não devem ser gravados em logs nem colocados em URLs.

Sessões devem possuir proteção contra fixation, replay e roubo de credenciais conforme o modelo empregado.

## 10. Autorização

Autenticação responde quem é o usuário. Autorização responde o que ele pode fazer.

As duas verificações são independentes.

Toda operação protegida deve validar autorização na camada confiável.

O sistema deve aplicar menor privilégio e negar por padrão.

Nunca confie em campos enviados pelo cliente como `isAdmin`, `role`, `ownerId`, `userId`, `tenantId` ou equivalentes sem validação independente.

A propriedade de cada objeto deve ser conferida antes de leitura, alteração ou exclusão.

Trocar o identificador de um recurso não pode permitir acesso ao recurso de outro usuário.

## 11. Sistemas multiusuário e multitenant

Dados pertencentes a usuários, empresas ou organizações diferentes devem possuir isolamento efetivo.

Consultas ao banco devem incluir o contexto de autorização correto.

Quando PostgreSQL Row Level Security ou mecanismo equivalente for usado, as políticas devem ser revisadas especificamente para SELECT, INSERT, UPDATE e DELETE.

Chaves administrativas que ignoram RLS não devem ser expostas a aplicações cliente.

Uma conta do tenant A jamais deve conseguir acessar objetos do tenant B por alteração de IDs, filtros, parâmetros, requisições diretas ou chamadas à API.

Testes automatizados de isolamento entre tenants são obrigatórios para sistemas multitenant.

## 12. Banco de dados

O banco não deve ser diretamente acessível pela Internet sem necessidade arquitetural explícita.

Credenciais devem possuir permissões mínimas.

A aplicação não deve operar rotineiramente usando uma conta administrativa global do banco.

Consultas devem utilizar parâmetros ou mecanismos equivalentes, nunca concatenação insegura de comandos SQL.

Migrações devem ser versionadas.

Alterações destrutivas devem possuir procedimento de recuperação.

Backups devem receber proteção compatível com o banco original.

Ambientes de desenvolvimento e homologação não devem receber cópias de dados reais sensíveis sem necessidade e proteção correspondente.

Logs do banco não devem expor senhas, tokens ou conteúdo altamente sensível.

## 13. Proteção dos dados

O primeiro mecanismo de proteção de um dado sensível é evitar armazená-lo quando não for necessário.

Dados em trânsito devem utilizar canais protegidos.

Dados sensíveis em repouso devem receber proteção adequada ao modelo de ameaça.

Não deve ser criada criptografia própria.

Algoritmos, modos de operação, bibliotecas e esquemas criptográficos devem ser reconhecidos e mantidos.

Chaves criptográficas não devem ser armazenadas junto aos dados protegidos quando isso eliminar a utilidade da criptografia.

Rotação e revogação de chaves devem ser possíveis quando a criticidade justificar.

Senhas não entram nessa regra de criptografia de dados: senhas devem ser armazenadas por hashing apropriado.

## 14. Segredos e credenciais

Segredos nunca devem ser incluídos no código-fonte.

Isso inclui senhas de banco, service keys, tokens privados, chaves de API, chaves de assinatura, certificados privados e outras credenciais.

Arquivos `.env` com segredos reais não devem ser versionados.

O repositório deve possuir mecanismo de detecção de segredos sempre que possível.

Segredos de produção devem ser mantidos em sistema apropriado de secrets management ou mecanismo seguro equivalente.

Cada componente deve receber apenas os segredos necessários.

Segredos devem permitir rotação e revogação.

Um segredo vazado deve ser considerado comprometido mesmo que posteriormente seja removido do Git. O histórico do repositório também deve ser avaliado.

## 15. APIs

Todos os endpoints devem possuir classificação explícita como públicos ou protegidos.

Endpoints protegidos devem autenticar e autorizar cada operação.

A API não deve confiar em filtros realizados somente pelo frontend.

Rate limiting e controles de abuso devem ser aplicados quando apropriados.

Respostas devem retornar somente os campos necessários.

Mass assignment deve ser evitado.

Parâmetros recebidos devem ser validados por tipo, tamanho, formato, limites e contexto.

APIs externas também devem ser tratadas como fontes não confiáveis.

## 16. Validação de entrada

Toda entrada externa deve possuir validação no servidor ou camada confiável equivalente.

Validação no cliente serve para experiência do usuário, não para segurança.

Sempre que possível, use listas de valores permitidos.

Devem existir limites de tamanho, quantidade, profundidade e frequência.

Entradas destinadas a HTML, SQL, shell, caminhos de arquivos, URLs, XML, templates ou outros contextos especiais devem receber tratamento apropriado ao contexto.

Não use `eval`, execução dinâmica de código ou comandos montados a partir de entrada externa sem necessidade excepcional e controles fortes.

## 17. Upload e manipulação de arquivos

O sistema não deve confiar no nome, extensão ou MIME informado pelo cliente.

Uploads devem possuir limites de tamanho e quantidade.

Nomes de arquivo devem ser normalizados ou substituídos por identificadores controlados pela aplicação.

Arquivos não devem escapar do diretório destinado por manipulação de caminhos.

Formatos permitidos devem ser explicitamente definidos.

Arquivos executáveis ou potencialmente ativos devem ser bloqueados quando não forem necessários.

O armazenamento deve evitar execução direta de conteúdo enviado pelo usuário.

Arquivos sensíveis devem exigir autorização também no download.

## 18. Aplicações web

Aplicações web devem possuir proteção contra XSS, CSRF, clickjacking e ataques relacionados ao navegador conforme a arquitetura empregada.

Cookies de sessão devem usar atributos seguros.

Operações que alterem estado não devem depender de GET.

Aplicações autenticadas por cookie devem possuir proteção apropriada contra CSRF.

Políticas CORS não devem ser amplas por conveniência.

Headers de segurança devem ser configurados de forma compatível com a aplicação.

## 19. Aplicativos móveis

Segredos permanentes não devem ser considerados protegidos apenas porque foram incluídos no APK ou pacote equivalente.

Credenciais administrativas, service-role keys ou chaves privadas nunca devem ser distribuídas no aplicativo cliente.

Dados sensíveis locais devem utilizar armazenamento apropriado da plataforma.

Backups automáticos, clipboard, screenshots, logs, notificações e cache devem ser avaliados quando puderem expor informações sensíveis.

Comunicação em rede deve utilizar canais protegidos.

Permissões do sistema operacional devem seguir menor privilégio.

Aplicativos móveis devem ser avaliados também com base no OWASP MASVS/MASWE.

## 20. Logs e auditoria

Eventos relevantes de segurança devem gerar logs.

Devem ser registrados, quando aplicável, logins, falhas de login, alterações de credenciais, recuperação de conta, mudanças de permissões, operações administrativas, alterações críticas de dados e decisões de autorização relevantes.

Logs devem registrar informação suficiente para investigação sem registrar segredos.

Senhas, tokens de sessão, tokens de recuperação, chaves privadas e outros segredos não devem ser registrados.

Logs administrativos devem possuir proteção contra alteração e acesso indevido.

## 21. Privacidade e minimização

O projeto deve documentar quais dados coleta, por que coleta, onde armazena, por quanto tempo mantém e quem pode acessá-los.

Dados desnecessários não devem ser coletados.

Ambientes de teste devem preferir dados fictícios.

Dados pessoais utilizados para suporte ou observabilidade devem ser minimizados.

A exclusão de conta deve possuir comportamento definido para dados ativos, backups, logs e obrigações de retenção.

Requisitos legais aplicáveis devem ser tratados separadamente deste baseline técnico.

## 22. Dependências e cadeia de suprimentos

Toda dependência adicionada aumenta a superfície de ataque.

Dependências devem possuir origem conhecida e manutenção ativa.

Bibliotecas abandonadas devem ser substituídas quando representarem risco.

Dependências devem ter versões controladas por lockfiles ou mecanismo equivalente.

Atualizações críticas de segurança devem possuir processo de aplicação.

O projeto deve executar análise de vulnerabilidades de dependências quando a plataforma permitir.

Pacotes não devem ser adicionados apenas para evitar implementar funcionalidade trivial.

Artefatos de build e releases devem ter origem reproduzível ou rastreável quando apropriado.

## 23. Infraestrutura e configuração

Ambientes de desenvolvimento, homologação e produção devem ser separados.

Configurações de debug não devem chegar à produção.

Endpoints internos, consoles administrativos, dashboards, bancos e serviços de observabilidade não devem ficar publicamente expostos sem necessidade.

Contas e serviços não utilizados devem ser desabilitados.

Configurações padrão inseguras devem ser eliminadas.

Firewall, IAM, security groups e políticas de cloud devem seguir menor privilégio.

O sistema deve falhar de forma segura. Na dúvida sobre autorização, o comportamento padrão deve ser negar.

## 24. Backups

Backups fazem parte do sistema de segurança.

Devem possuir controle de acesso e proteção compatível com os dados de produção.

Procedimentos de restauração devem ser testados.

Um backup que nunca foi restaurado com sucesso não deve ser considerado estratégia comprovada de recuperação.

Políticas de retenção e descarte devem ser definidas.

## 25. Exclusão e descarte

A exclusão lógica e a exclusão física devem ser diferenciadas.

O comportamento esperado deve ser documentado.

Dados removidos não devem permanecer indefinidamente em caches, exports, arquivos temporários ou sistemas auxiliares sem necessidade.

Segredos substituídos devem ser revogados.

Mídias desativadas devem receber tratamento adequado aos dados existentes.

## 26. Operações administrativas

Interfaces administrativas devem possuir autenticação forte.

MFA deve ser obrigatório sempre que tecnicamente viável.

Privilégios administrativos devem ser separados das contas comuns quando o risco justificar.

Ações administrativas sensíveis devem ser auditáveis.

Não devem existir contas administrativas padrão com senha conhecida.

Operações extremamente sensíveis podem exigir reautenticação ou aprovação adicional.

## 27. Testes obrigatórios de autorização

O sistema deve possuir testes negativos.

Não basta testar que o usuário correto consegue acessar seus dados.

É obrigatório testar que usuário A não consegue ler, alterar ou excluir dados do usuário B.

É obrigatório testar que usuário comum não executa funções administrativas.

É obrigatório testar requisições sem autenticação.

É obrigatório testar tokens expirados, revogados ou inválidos.

É obrigatório testar alteração manual de IDs, parâmetros, roles e identificadores de tenant.

Para cada endpoint protegido deve existir evidência de teste de autorização correspondente.

## 28. Verificações automatizadas

Sempre que disponíveis no projeto, devem ser executados:

- testes unitários;
- testes de integração;
- testes de autorização;
- lint;
- build completo;
- type checking;
- análise estática de segurança;
- análise de dependências vulneráveis;
- detecção de segredos;
- verificações específicas da plataforma.

Falha em uma verificação de segurança não deve ser contornada apenas para produzir um build verde.

Regras de segurança não devem ser desabilitadas para fazer testes passarem.

Um falso positivo pode ser suprimido somente após análise e justificativa documentada.

## 29. Processo obrigatório para código produzido por IA

Toda alteração produzida por IA deve ser considerada proposta, não implementação confiável.

Quando houver Git disponível, a IA não deve trabalhar diretamente na branch principal.

Mudanças devem ser pequenas, revisáveis e isoladas sempre que possível.

Antes da alteração, a IA deve compreender arquitetura e controles de segurança existentes.

Após a alteração, deve revisar especificamente autenticação, autorização, validação de entradas, armazenamento, manipulação de arquivos, chamadas de rede, segredos, criptografia, permissões, dependências e exposição de dados.

A IA deve procurar formas de abuso da própria implementação, não apenas conferir o fluxo normal.

A IA deve executar testes, build, lint e verificações de segurança disponíveis.

Quando uma verificação não puder ser executada, isso deve ser explicitamente informado.

A IA não deve declarar uma implementação segura apenas porque os testes passaram.

## 30. Gate obrigatório antes de merge

Uma mudança sensível somente pode ser considerada pronta quando:

- sua funcionalidade foi validada;
- fluxos de erro foram testados;
- autorização foi testada negativamente;
- nenhum segredo novo foi introduzido;
- entradas externas foram avaliadas;
- dependências novas foram verificadas;
- testes passaram;
- build passou;
- lint passou;
- verificações de segurança disponíveis passaram.

Falhas conhecidas críticas ou altas impedem liberação.

Exceções exigem decisão humana explícita, justificativa registrada e plano de mitigação.

## 31. Gate obrigatório antes de produção

Antes da primeira publicação ou de mudança significativa em autenticação, autorização, banco, infraestrutura ou criptografia, o sistema deve receber revisão de segurança mais ampla.

Devem ser avaliadas configurações reais de produção, permissões, secrets, endpoints públicos, políticas de banco, backups, monitoramento, logs e processo de recuperação.

Testes realizados apenas localmente não são suficientes para validar configuração de produção.

Sistemas de risco elevado devem receber revisão independente ou teste de penetração periódico.

## 32. Critérios que impedem release

O sistema não deve ser liberado quando houver:

- senha ou segredo em texto puro;
- credencial real no repositório;
- endpoint sensível sem autorização;
- banco publicamente exposto sem justificativa;
- autorização feita apenas no cliente;
- acesso a dados de outro usuário por alteração de identificador;
- criptografia caseira em função crítica;
- vulnerabilidade crítica conhecida sem mitigação;
- dependência comprometida conhecida;
- debug expondo informações sensíveis;
- backup sem proteção adequada;
- ausência de mecanismo de recuperação quando perda de dados for inaceitável.

## 33. Evidência de segurança

Cada release relevante deve deixar evidência do que foi verificado.

O relatório deve informar versão ou commit avaliado, testes executados, build, lint, análise de dependências, análise de segredos, análise estática, testes de autorização, riscos encontrados, riscos corrigidos, verificações não executadas e riscos residuais.

Ausência de evidência significa ausência de comprovação.

## 34. Resposta a vulnerabilidades

Todo projeto que alcance produção deve possuir procedimento para vulnerabilidades descobertas posteriormente.

Deve ser possível revogar credenciais, rotacionar segredos, invalidar sessões, corrigir software, atualizar dependências e notificar responsáveis.

Incidentes relevantes devem gerar análise de causa raiz.

A correção de uma vulnerabilidade deve avaliar se o mesmo padrão inseguro aparece em outras partes do projeto.

## 35. Segurança contínua

Segurança não termina no release.

Bibliotecas envelhecem, vulnerabilidades são descobertas, configurações mudam, credenciais vazam e novas técnicas de ataque aparecem.

Sistemas em produção devem possuir revisão periódica de dependências, credenciais, permissões, configurações e vulnerabilidades.

## 36. Regra final para a IA e meta de qualidade

Ao receber uma tarefa sujeita a esta baseline, a IA deve perguntar internamente:

**Como esta alteração poderia permitir que alguém acessasse, modificasse, destruísse ou extraísse algo que não deveria?**

Depois deve tentar demonstrar, por código, configuração e testes, que os controles necessários impedem esse abuso.

A IA nunca deve assumir que código seguro anteriormente continuará seguro depois da alteração.

A IA nunca deve enfraquecer autenticação, autorização, criptografia, validação, isolamento, auditoria ou outro controle de segurança para simplificar implementação ou fazer um teste passar.

Ao finalizar qualquer mudança relevante, deve informar:

- verificações executadas;
- verificações não executadas;
- riscos residuais.

Nenhuma implementação deve ser descrita como “inviolável”, “100% segura” ou “blindada”.

Classificação aceitável:

> Implementação verificada de acordo com o baseline de segurança definido, sem vulnerabilidades críticas ou altas conhecidas nas verificações realizadas e com riscos residuais documentados.

## Referências oficiais

- OWASP ASVS: https://owasp.org/projects/asvs/
- NIST SP 800-63-4: https://csrc.nist.gov/pubs/sp/800/63/4/final
- NIST SP 800-218: https://csrc.nist.gov/pubs/sp/800/218/final
- OWASP Cheat Sheet Series: https://cheatsheetseries.owasp.org/
- OWASP API Security: https://owasp.org/API-Security/
- OWASP MASVS: https://mas.owasp.org/MASVS/


---

# Revisão v1.1 — Automação, ASVS e Security Gates

## 37. Enquadramento automático

Todo projeto que possua qualquer uma das características abaixo deve ser automaticamente enquadrado nesta baseline, independentemente de solicitação explícita:

- autenticação, login ou senha;
- conta de usuário;
- sessão ou token de autenticação;
- API autenticada;
- banco de dados contendo dados privados;
- armazenamento remoto ou privado;
- documentos privados;
- permissões entre usuários;
- funções administrativas;
- múltiplos usuários;
- múltiplas organizações ou tenants;
- informações pessoais ou sensíveis.

O responsável não precisa lembrar de pedir a aplicação da baseline. A própria descoberta dessas características deve disparar o protocolo.

## 38. Alvo obrigatório de segurança

O alvo padrão é:

**OWASP ASVS 5.0.0 — Nível 2.**

Todo sistema sujeito a esta baseline deve buscar cobertura compatível com os requisitos aplicáveis do ASVS Nível 2.

Requisitos tecnicamente inaplicáveis podem ser marcados como `NOT_APPLICABLE`, desde que exista justificativa.

### 38.1 Avaliação de Nível 3

Deve existir avaliação explícita sobre a necessidade de requisitos ASVS Nível 3 quando houver risco elevado, incluindo:

- grande quantidade de informações pessoais;
- dados especialmente sensíveis;
- funções administrativas de alto impacto;
- aplicações multitenant;
- operações financeiras ou equivalentes;
- serviços públicos na Internet;
- integrações críticas;
- gerenciamento próprio de material criptográfico;
- alto impacto operacional, jurídico, financeiro ou institucional em caso de comprometimento.

A conclusão deve ser registrada como:

- `ASVS_TARGET = L2`
- `ASVS_TARGET = L2 + requisitos L3 selecionados`
- `ASVS_TARGET = L3`

## 39. Matriz verificável do ASVS

O projeto deve manter uma matriz dos requisitos aplicáveis.

Cada requisito deve possuir, quando possível:

- identificador;
- descrição resumida;
- aplicabilidade;
- estado;
- evidência;
- teste correspondente;
- observações;
- risco residual.

Estados permitidos:

- `PASS`
- `FAIL`
- `NOT_APPLICABLE`
- `NOT_VERIFIED`
- `ACCEPTED_RISK`

`PASS` exige evidência. Opinião da IA ou funcionamento aparente não é evidência suficiente.

`NOT_APPLICABLE` exige justificativa.

`ACCEPTED_RISK` exige decisão humana explícita.

## 40. SECURITY_CHECKLIST.md obrigatório

Projetos enquadrados devem manter um `SECURITY_CHECKLIST.md` na raiz ou em local canônico claramente indicado.

A baseline define o que deve ser protegido. O checklist define o que deve ser conferido naquela implementação concreta.

O checklist deve ser adaptado à stack.

Exemplos:

- Android/Kotlin: armazenamento local, permissões, backups, logs, APK, rede, keystore e credenciais.
- Supabase: Auth, RLS, policies, service role, buckets, Edge Functions e isolamento.
- PostgreSQL: roles, privilégios, queries parametrizadas, RLS, rede e backups.
- Node: dependências, validação, middleware, endpoints, headers, secrets e command execution.
- Python: equivalentes específicos ao framework.
- Firebase: Authentication, Firestore/Realtime Database Rules, Storage Rules, Admin SDK e credenciais.
- Web: cookies, sessão, CSRF, XSS, CORS, CSP e headers.

O checklist não substitui a baseline.

## 41. SECURITY_PROFILE.yml obrigatório

Projetos enquadrados devem manter um perfil legível por máquina contendo, no mínimo:

- versão da baseline;
- versão ASVS;
- alvo ASVS;
- categorias de risco presentes;
- gates obrigatórios;
- severidades que bloqueiam release;
- política para código gerado por IA;
- diretório de evidências.

Um exemplo reutilizável existe neste repositório em `SECURITY_PROFILE.example.yml`.

## 42. Instruções obrigatórias para agentes de IA

Todo repositório enquadrado deve possuir instruções locais para agentes, por exemplo em `AGENTS.md`.

Antes de alterar código sensível, o agente deve:

1. ler a baseline;
2. ler o perfil;
3. ler o checklist;
4. ler ADRs e documentação arquitetural relevantes;
5. identificar a stack;
6. identificar dados e recursos afetados;
7. identificar autenticação e autorização;
8. identificar nova superfície de ataque;
9. trabalhar em branch separada quando houver Git;
10. testar fluxo normal e abuso.

A ausência de pedido explícito do usuário não elimina essa obrigação.

## 43. Sequência automática de segurança

Toda tarefa sujeita a esta baseline deve seguir automaticamente as fases abaixo.

### Fase A — Classificação

Determinar se a mudança envolve autenticação, autorização, dados privados, banco, arquivos, rede, APIs, criptografia, segredos, permissões, dependências ou infraestrutura.

### Fase B — Antes da implementação

Identificar:

- arquitetura;
- fronteiras de confiança;
- autenticação;
- autorização;
- segredos;
- dados sensíveis;
- entradas externas;
- comunicações externas;
- testes existentes.

### Fase C — Implementação

Aplicar:

- negação por padrão;
- menor privilégio;
- validação em camada confiável;
- parametrização;
- minimização de dados;
- bibliotecas consolidadas;
- ausência de criptografia caseira;
- ausência de novos segredos hardcoded;
- preservação dos controles existentes.

### Fase D — Revisão adversarial

Após implementar, a IA deve procurar formas de abuso da própria alteração.

Avaliar, quando aplicável:

- bypass de autenticação;
- bypass de autorização;
- IDOR/BOLA;
- escalada de privilégio;
- alteração de role;
- acesso entre usuários;
- acesso entre tenants;
- SQL/command injection;
- XSS;
- CSRF;
- SSRF;
- path traversal;
- upload malicioso;
- mass assignment;
- race conditions;
- replay;
- exposição de token;
- exposição de segredo;
- manipulação de parâmetros;
- enumeração de contas;
- brute force;
- abuso de recursos;
- rate limiting insuficiente;
- permissões excessivas;
- vazamento em logs;
- falhas criptográficas;
- dependências vulneráveis.

### Fase E — Verificação técnica

Executar todas as verificações disponíveis:

- testes;
- integração;
- autorização;
- build;
- lint;
- type checking;
- SAST;
- dependency scanning;
- secret scanning;
- verificações específicas da plataforma.

### Fase F — Relatório

Toda tarefa relevante deve terminar com:

- Verificações executadas.
- Verificações não executadas.
- Problemas encontrados.
- Problemas corrigidos.
- Riscos residuais.
- Situação do gate.

Estados:

- `PASS`
- `PASS_WITH_RESIDUAL_RISK`
- `BLOCKED`
- `NOT_FULLY_VERIFIED`

## 44. Gate automatizado de segurança

Sempre que tecnicamente possível, as verificações não devem depender apenas da memória humana ou da IA.

O CI deve possuir um job, preferencialmente chamado `security-gate`, adaptado à stack.

Ele deve executar, conforme aplicável:

- testes;
- build;
- lint;
- type checking;
- testes de autorização;
- secret scanning;
- análise de dependências;
- SAST;
- scanners de container, infraestrutura, mobile ou cloud.

O merge deve ser bloqueado quando um gate obrigatório falhar.

Um controle não deve ser artificialmente enfraquecido para permitir merge.

## 45. Branch principal protegida

Quando Git e pull requests estiverem disponíveis, mudanças produzidas por IA não devem ser feitas diretamente na branch principal.

Fluxo padrão:

`nova branch → implementação → revisão adversarial → testes → security gate → revisão → merge`

Quando a plataforma permitir, a branch principal deve exigir checks obrigatórios e impedir push acidental ou bypass indevido.

## 46. Alterações críticas

As seguintes áreas exigem verificação reforçada:

- login;
- criação de conta;
- recuperação de senha;
- MFA;
- sessão;
- tokens;
- autorização;
- roles;
- permissões;
- RLS;
- migrations que afetem acesso;
- funções administrativas;
- criptografia;
- segredos;
- upload/download privado;
- webhooks;
- integrações externas;
- produção;
- infraestrutura;
- backups.

Alterações críticas não devem ser aprovadas apenas com testes de caminho feliz.

## 47. Testes negativos obrigatórios

Toda funcionalidade de autorização deve demonstrar não apenas o que permite, mas o que impede.

Exemplos obrigatórios quando aplicáveis:

- usuário A acessa dado A;
- usuário A não acessa dado B;
- administrador executa operação administrativa;
- usuário comum não executa a mesma operação;
- token válido funciona;
- token expirado, alterado ou revogado falha;
- requisição autenticada funciona;
- requisição sem autenticação falha;
- identificador de outro usuário ou tenant falha.

## 48. Relatório de segurança por release

Releases relevantes devem gerar ou atualizar evidência em `security-reports/` ou local equivalente.

Registrar:

- versão;
- commit;
- data;
- alvo ASVS;
- testes realizados;
- scanners executados;
- vulnerabilidades detectadas;
- correções;
- verificações não executadas;
- riscos residuais;
- decisão final.

## 49. Bootstrap automático de segurança

Projetos novos devem preferencialmente nascer a partir de um template seguro contendo desde o primeiro commit:

- `SECURITY_BASELINE.md`;
- `SECURITY_CHECKLIST.md`;
- `SECURITY_PROFILE.yml`;
- instruções de agente;
- `security-reports/`;
- configuração de CI compatível.

O protocolo detalhado está em `SECURITY_BOOTSTRAP.md`.

## 50. Projetos existentes

Ao identificar projeto existente com autenticação ou dados privados que não possua a estrutura, a IA deve recomendar e, quando autorizada a modificar o projeto, inicializar a baseline.

O processo deve ser incremental.

Não marque controles desconhecidos como aprovados.

Corrija riscos críticos e altos primeiro.

## 51. Regra de descoberta automática

O agente deve presumir que a baseline é aplicável quando encontrar elementos equivalentes a:

- `login`
- `auth`
- `password`
- `session`
- `token`
- `JWT`
- `OAuth`
- Firebase Auth
- Supabase Auth
- tabela de usuários
- banco remoto
- API privada
- RLS
- roles
- permissions
- admin
- private storage

A detecção desses sinais deve disparar a conferência dos controles.

## 52. Regra de parada

A IA não deve classificar uma alteração como concluída quando encontrar:

- segredo exposto;
- credencial comprometida;
- autenticação contornável;
- autorização contornável;
- acesso indevido entre usuários;
- vulnerabilidade crítica conhecida;
- vulnerabilidade alta sem mitigação;
- proteção deliberadamente desativada;
- teste de segurança removido apenas para permitir aprovação.

Nesses casos:

`SECURITY_GATE = BLOCKED`

## 53. Ausência de ferramentas

Não possuir scanner ou ambiente disponível não permite afirmar que a categoria foi aprovada.

A saída deve distinguir claramente:

- verificado e aprovado;
- não verificado;
- parcialmente verificado.

Verificações relevantes indisponíveis devem aparecer em riscos residuais.

## 54. Revisão humana

Automação e IA não eliminam revisão humana.

Sistemas de risco elevado, mudanças arquiteturais críticas ou exposição pública relevante devem considerar revisão humana especializada independente e, quando apropriado, teste de penetração.

Checklist, IA, SAST, SCA, DAST e testes automatizados são camadas complementares.

Nenhuma camada isolada prova segurança completa.

## 55. Defesa em profundidade

Um único erro não deve ser suficiente para comprometer o sistema.

Quando adequado, controles independentes devem existir em múltiplas camadas.

Exemplo:

- o frontend não apresenta uma ação proibida;
- a API rejeita a ação;
- o banco também impede acesso indevido.

A falha de uma camada não deve eliminar as demais.

## 56. Atualização, versão estável e regra permanente

Esta baseline deve acompanhar versões estáveis dos referenciais adotados.

Versões preview, draft, bleeding edge ou equivalentes devem ser avaliadas separadamente antes de substituir uma referência estável em produção.

Em outubro de 2026, a referência adotada por esta baseline é OWASP ASVS 5.0.0; NIST SP 800-63-4 é final; e NIST SP 800-218 versão 1.1 permanece a versão final do SSDF enquanto revisões posteriores ainda em draft não substituem automaticamente a referência.

Sempre que um projeto combinar software com identidade, autenticação, armazenamento privado ou controle de acesso, segurança deve ser tratada como parte do funcionamento do produto.

Ela não é opcional.

Ela não depende de o responsável lembrar de solicitá-la.

Ela não depende de uma conversa específica com uma IA.

Ela deve existir no repositório, no processo de desenvolvimento, nos testes e no processo de release.

## Artefatos complementares desta versão

- `SECURITY_CHECKLIST.md`
- `SECURITY_PROFILE.example.yml`
- `AGENTS_SECURITY_TEMPLATE.md`
- `SECURITY_BOOTSTRAP.md`
- `SECURITY_REPORT_TEMPLATE.md`
