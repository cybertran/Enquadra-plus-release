# Dossiê Canônico — Atualização Local e Segura das Bases Jurídico-Normativas do Enquadra+

**Status:** CANÔNICO  
**Data da decisão:** 13/10/2026  
**Escopo:** bloco 6 do roteiro funcional  
**Aplica-se a:** CTB, MBFT, catálogo CONTRAN/SENATRAN, relações derivadas e demais datasets normativos oficiais do Enquadra+  
**Princípio central:** nenhuma atualização externa pode modificar diretamente uma base validada.

## 1. Finalidade

Este dossiê define a arquitetura obrigatória para atualização das bases jurídico-normativas do Enquadra+ sem depender de servidor próprio, computador pessoal, máquina virtual ou serviço de backend operado pelo projeto.

O próprio aplicativo Android deverá ser capaz, quando houver conexão com a internet, de consultar diretamente fontes primárias oficiais, detectar mudanças, baixar os documentos necessários, construir uma nova versão candidata da base, validá-la integralmente e somente então promovê-la a versão ativa.

A consulta cotidiana do usuário continua sendo local-first e deve permanecer funcional sem internet. A internet é necessária apenas para verificar e obter atualizações.

A confiabilidade tem precedência sobre a atualidade. Se o aplicativo não conseguir demonstrar que uma atualização é segura, a atualização deve ser recusada e a última base validada deve continuar ativa.

## 2. Decisão arquitetural

O bloco 6 **não será baseado em um servidor do Enquadra+** e **não dependerá de IA para interpretar ou aprovar alterações normativas**.

O fluxo canônico é:

```
fonte oficial
    ↓
verificação de identidade e transporte
    ↓
download imutável da fonte
    ↓
detecção de versão/hash
    ↓
parser determinístico compatível
    ↓
base candidata isolada
    ↓
validação estrutural
    ↓
validação semântica
    ↓
comparação diferencial com a base ativa
    ↓
validação cruzada entre datasets
    ↓
promoção atômica
    ↓
nova base ativa
```

Qualquer falha interrompe o fluxo antes da promoção.

## 3. Regra de ouro

> **Nenhum dado baixado da internet pode ser escrito diretamente sobre a base ativa.**

A versão atualmente validada é tratada como um estado conhecido e bom — *known good state*. Toda nova versão é construída em ambiente separado.

A base ativa só muda por troca atômica depois que a candidata passar por todas as validações exigidas para aquele dataset.

Atualização parcial de tabelas ativas durante parsing, “merge em produção”, substituição linha a linha da base vigente e migração destrutiva disparada por conteúdo externo são proibidos.

## 4. Modelo A/B com snapshots

O Enquadra+ deverá manter, conceitualmente, três estados independentes:

- **ativa:** versão usada nas consultas do aplicativo;
- **anterior validada:** última versão comprovadamente válida antes da ativa;
- **candidata:** versão em construção ou aguardando validação.

A candidata nunca é consultada pelas telas normais do aplicativo.

Depois de uma promoção bem-sucedida:

```
anterior validada ← antiga ativa
ativa             ← candidata aprovada
candidata          ← vazia
```

A versão anterior deve permanecer disponível para rollback.

O armazenamento concreto pode usar arquivos SQLite separados, bancos Room independentes por geração ou outra estratégia que ofereça o mesmo isolamento. A escolha técnica não pode enfraquecer a separação lógica entre ativa e candidata.

## 5. Falha fechada — fail-closed

O atualizador deve ser deliberadamente conservador.

São exemplos de eventos que devem **bloquear** uma atualização:

- documento oficial indisponível ou resposta incompleta;
- domínio, protocolo ou redirecionamento não autorizado;
- tipo de conteúdo inesperado;
- hash impossível de calcular ou manifesto inconsistente;
- parser desconhecido para o formato encontrado;
- quantidade de registros fora dos limites esperados;
- duplicidade de códigos ou identificadores;
- perda inesperada de grande quantidade de registros;
- campos obrigatórios ausentes;
- referência a fonte oficial ausente;
- relação quebrada entre datasets;
- código MBFT referenciado que desapareceu sem justificativa oficial;
- CTB com artigos inesperadamente ausentes;
- alteração estrutural do documento que torne ambígua a extração;
- erro de migração;
- erro de validação do SQLite/Room;
- divergência entre contagens declaradas e calculadas;
- diferença que não possa ser classificada com segurança por regra determinística;
- qualquer exceção durante parsing, validação ou promoção.

Em qualquer um desses casos, o aplicativo deve conservar a base ativa sem modificação.

## 6. A ausência de atualização é um estado válido

O sistema não deve tratar “não consegui atualizar” como falha operacional do aplicativo.

O usuário deve continuar podendo consultar normalmente a última versão validada.

Estados esperados incluem:

- atualizado;
- nova versão disponível e aprovada;
- nova versão disponível, mas bloqueada;
- fonte temporariamente indisponível;
- formato oficial alterado e ainda não suportado;
- atualização adiada por falta de conexão;
- rollback disponível.

A prioridade é nunca trocar uma base correta por uma base duvidosa.

## 7. Sem servidor próprio

O aplicativo não dependerá de infraestrutura mantida pelo autor do projeto.

Não deve ser requisito operacional:

- computador pessoal ligado;
- VM permanente;
- servidor HTTP próprio;
- banco remoto do Enquadra+;
- job agendado em máquina do desenvolvedor;
- serviço privado de transformação de documentos.

O app consulta diretamente as fontes oficiais aprovadas.

Isso reduz dependências operacionais e permite que o sistema continue funcionando futuramente mesmo se a infraestrutura particular do desenvolvedor deixar de existir.

## 8. Fontes confiáveis

Somente fontes primárias oficiais podem produzir ou alterar dados normativos.

Cada conector de fonte deverá possuir uma política explícita de confiança contendo, no mínimo:

- domínios autorizados;
- protocolo HTTPS;
- caminhos ou padrões esperados;
- política de redirecionamento;
- tipo de documento esperado;
- parser compatível;
- regras mínimas de integridade;
- dataset que aquela fonte pode atualizar.

Metadados HTTP como ETag e Last-Modified podem ser usados para otimização, mas **não são prova suficiente de identidade ou integridade**. A decisão final deve depender do conteúdo efetivamente recebido e de seu hash.

Redirecionamentos não devem ampliar silenciosamente a lista de origens confiáveis.

## 9. Manifesto de proveniência

Toda versão candidata deve possuir um manifesto local de proveniência.

O manifesto deve registrar, quando aplicável:

- dataset;
- versão lógica;
- schema version;
- parser version;
- data/hora de obtenção;
- data/hora de geração;
- URL solicitada;
- URL final;
- órgão responsável;
- identificador do ato/documento;
- MIME type;
- tamanho do conteúdo;
- SHA-256 da fonte bruta;
- SHA-256 canônico dos registros estruturados;
- quantidade de registros;
- lista de fontes utilizadas;
- versão das dependências normativas;
- validações executadas;
- resultado de cada validação;
- versão-base usada para comparação;
- resumo do diff;
- motivo de bloqueio, quando houver.

Nenhum dataset sem proveniência completa pode ser promovido.

## 10. Imutabilidade da fonte baixada

A fonte oficial usada para gerar uma candidata deve ser tratada como artefato imutável durante o ciclo de validação.

O parser não deve reconsultar a internet durante uma mesma geração e misturar respostas de momentos diferentes.

Uma geração deve usar um conjunto fechado de entradas com hashes conhecidos.

Se uma fonte precisar ser baixada novamente, deve ser iniciada uma nova geração candidata.

## 11. Parsers determinísticos e versionados

Cada formato suportado deve possuir parser explicitamente versionado.

Exemplo conceitual:

```
ctb-html-parser-v3
mbft-pdf-parser-v4
contran-index-parser-v2
senatran-index-parser-v2
```

Dado o mesmo conteúdo de entrada, a mesma versão de parser deve produzir exatamente a mesma saída canônica.

O parser deve rejeitar estruturas que não reconheça.

Ele não deve “adivinhar” novas colunas, títulos ou relações com heurísticas permissivas quando uma mudança estrutural puder afetar o significado jurídico.

Se o órgão alterar o formato de publicação de maneira incompatível, o resultado correto é:

> **Formato oficial alterado. Atualização não aplicada.**

Nessa situação, pode ser necessária uma nova versão do APK com parser atualizado. Isso é aceitável e preferível a corromper a base.

## 12. Papel da IA

IA não integra a cadeia de confiança da atualização.

Uma IA poderá futuramente ser usada como ferramenta auxiliar de desenvolvimento, auditoria ou triagem, mas:

- não pode decidir que uma base está correta;
- não pode promover candidata;
- não pode preencher silenciosamente campo ausente;
- não pode inventar relação entre dispositivos;
- não pode substituir fonte oficial;
- não pode resolver divergência normativa sem prova determinística;
- não pode transformar interpretação probabilística em dado oficial.

Se uma funcionalidade futura usar IA para indicar possíveis alterações, a indicação deverá ser tratada apenas como alerta e nunca como evidência suficiente para modificar o banco.

## 13. Camadas obrigatórias de validação

Uma atualização somente pode ser promovida depois de passar pelas camadas aplicáveis abaixo.

### 13.1 Transporte e origem

Validar:

- HTTPS;
- domínio autorizado;
- redirecionamento permitido;
- resposta HTTP aceitável;
- limites de tamanho;
- tipo de conteúdo;
- ausência de resposta HTML inesperada no lugar de PDF/JSON;
- hash da resposta.

### 13.2 Sintaxe

Validar:

- JSON/HTML/PDF processável pelo parser esperado;
- encoding;
- estrutura mínima;
- campos obrigatórios;
- tipos;
- valores enumerados;
- ausência de registros malformados.

### 13.3 Estrutura

Validar:

- quantidade de registros;
- identificadores únicos;
- chaves primárias;
- chaves estrangeiras;
- FTS reconstruível;
- schema compatível;
- migrações válidas;
- integridade SQLite;
- ausência de referências órfãs.

### 13.4 Semântica

Validar invariantes específicos do domínio.

Exemplos:

- código de enquadramento com formato aceito;
- artigo legal não vazio;
- natureza dentro do conjunto permitido;
- pontos compatíveis com os valores legais possíveis;
- tipos de relação válidos;
- número/ano de ato coerentes;
- CTB sem duplicidade de artigo;
- MBFT sem duas fichas ativas com a mesma identidade;
- relação derivada apontando apenas para códigos existentes quando operacional.

### 13.5 Diferença contra a base ativa

Nenhuma candidata deve ser avaliada isoladamente.

O sistema deve calcular um diff completo contra a versão ativa:

- adicionados;
- alterados;
- removidos;
- renumerados;
- supersededidos;
- referências novas;
- referências quebradas;
- campos que passaram a nulo;
- campos que mudaram de categoria;
- variação percentual e absoluta de contagem.

Mudanças anormais devem bloquear promoção.

### 13.6 Validação cruzada

Datasets dependentes devem ser verificados conjuntamente.

Exemplos:

- relações exigem versão exata do MBFT;
- ficha MBFT que referencia artigo deve encontrar o dispositivo esperado no CTB quando a relação for estruturalmente verificável;
- atos normativos citados como origem devem existir no catálogo quando esse catálogo já cobrir a fonte;
- dataset derivado deve registrar a versão exata do dataset-fonte.

## 14. Política de anomalias

O sistema deve possuir limites conservadores, mas limites numéricos isolados não são suficientes.

Exemplos de eventos suspeitos:

- 405 fichas MBFT se tornarem 247;
- dezenas de códigos existentes desaparecerem sem ato oficial correspondente;
- centenas de fichas mudarem simultaneamente de artigo;
- CTB perder parte substancial dos artigos;
- catálogo normativo diminuir abruptamente;
- parser deixar campos antes obrigatórios vazios;
- relações crescerem ou diminuírem de maneira incompatível com o diff do MBFT.

Esses casos devem ser classificados como anomalia e bloquear a promoção.

A política deve preferir falso negativo de atualização a falso positivo de validade.

## 15. Classes de mudança

Quando possível, diferenças devem ser classificadas deterministicamente em categorias como:

- inclusão;
- alteração textual;
- revogação;
- supersessão;
- renumeração;
- mudança de vigência;
- alteração de metadado;
- alteração estrutural;
- remoção não explicada;
- alteração incompatível.

Classes não reconhecidas devem ser bloqueadas.

## 16. Promoção atômica

A promoção deve ser indivisível do ponto de vista das telas do aplicativo.

O usuário nunca deve observar metade da base antiga e metade da nova.

A sequência deve ser:

1. finalizar candidata;
2. fechar escrita;
3. executar validação final;
4. persistir manifesto aprovado;
5. marcar snapshot atual como anterior;
6. trocar ponteiro/generation id para candidata;
7. reabrir repositórios/índices;
8. executar verificação pós-promoção;
9. somente então descartar temporários.

Se qualquer etapa crítica falhar antes da troca, a ativa permanece intacta.

Se a verificação pós-promoção falhar, o rollback deve ser automático.

## 17. Rollback

Rollback é requisito funcional, não ferramenta de desenvolvimento.

O sistema deve conservar ao menos a versão ativa anterior validada.

A restauração deve:

- não depender de internet;
- não repetir parsing;
- não reconstruir a partir da fonte;
- usar snapshot já validado;
- ser atômica;
- registrar o motivo do rollback.

O usuário poderá futuramente receber uma opção explícita de “Restaurar versão anterior” em uma tela de diagnóstico/atualização, desde que isso não exponha uma versão incompatível com o schema atual.

## 18. Atualização independente por dataset

Não existirá uma única atualização monolítica obrigatória.

Os principais datasets possuem ciclos próprios:

- CTB;
- MBFT;
- catálogo CONTRAN;
- catálogo SENATRAN;
- relações derivadas;
- datasets específicos de ferramentas quando houver fonte própria.

Uma atualização de CTB não deve reimportar MBFT sem necessidade.

Uma atualização de MBFT deve invalidar ou reconstruir os datasets que dependam daquela versão, como relações.

A promoção conjunta somente é permitida quando a consistência exigir atomicidade entre dois ou mais datasets.

## 19. CTB

O CTB deverá ser atualizado a partir do texto compilado oficial adotado pelo projeto.

Regras mínimas:

- comparar hash da fonte;
- reconstruir artigos e Anexo I em candidata;
- preservar identificação estável de artigos;
- detectar inclusão, alteração e remoção;
- rejeitar duplicidades;
- rejeitar redução estrutural incompatível;
- validar contagem mínima e invariantes históricos configurados;
- reconstruir FTS na candidata;
- produzir diff artigo a artigo.

Mudança do HTML do site que impeça identificação segura dos artigos deve bloquear a atualização.

## 20. MBFT

O MBFT exige o nível mais conservador de atualização.

Nenhuma ficha existente deve ser modificada diretamente.

A candidata deve ser reconstruída integralmente a partir do conjunto oficial compatível com o parser instalado.

Devem ser comparados, no mínimo:

- códigos;
- tipificação;
- dispositivo legal;
- natureza;
- pontuação;
- responsabilidade;
- competência;
- constatação;
- penalidades;
- medidas administrativas;
- quando autuar;
- quando não autuar;
- procedimentos;
- observações oficiais;
- referências entre códigos;
- situação de cada ficha;
- proveniência.

Referência a código desconhecido não deve ser remapeada por semelhança.

Código desaparecido não deve ser considerado automaticamente revogado.

Alteração de layout que torne a leitura ambígua bloqueia a atualização.

## 21. Catálogo CONTRAN/SENATRAN

Os índices oficiais devem ser comparados por identidade de ato e conteúdo canônico.

O sistema pode detectar:

- novo ato;
- alteração de metadado;
- alteração de situação/observação;
- novo link oficial;
- desaparecimento do índice.

O desaparecimento de um ato previamente conhecido da página corrente não significa revogação por si só.

Revogação ou mudança de situação deve ser derivada de informação oficial identificável, nunca inferida apenas por ausência.

## 22. Relações entre enquadramentos

Relações derivadas nunca são fonte primária.

Quando o MBFT mudar, as relações operacionais devem ser recalculadas a partir da candidata MBFT e validadas antes da promoção conjunta quando exigido.

As mesmas regras atuais permanecem:

- somente fichas `VERIFIED` originam automaticamente alternativa, concomitância ou pré-requisito;
- evidência oficial explícita deve ser preservada;
- referências não resolvidas não recebem substituição heurística;
- relações curadas permanecem separadas da evidência oficial.

## 23. Atualização de datasets de ferramentas

Datasets estruturados de ferramentas, como equipamentos obrigatórios, não devem ser sobrescritos automaticamente apenas porque a resolução-fonte recebeu nova publicação.

Se a fonte oficial mudar:

1. detectar hash diferente;
2. identificar se a estrutura é compatível;
3. reconstruir candidata;
4. validar todas as células/invariantes já formalizados;
5. comparar célula por célula com a versão ativa;
6. bloquear se houver alteração não classificada.

Uma fonte complexa que não possa ser convertida com prova determinística pelo parser instalado deve permanecer na última versão validada.

## 24. Geração conhecida e boa — known good generation

Cada dataset ativo deve registrar uma geração validada.

Exemplo conceitual:

```
dataset: mbft
generation: 2026-10-13-mbft-<hash>
schemaVersion: 6
parserVersion: mbft-pdf-parser-v4
status: ACTIVE_VERIFIED
previousGeneration: ...
```

Estados mínimos recomendados:

- `CANDIDATE_DOWNLOADING`;
- `CANDIDATE_PARSING`;
- `CANDIDATE_VALIDATING`;
- `CANDIDATE_BLOCKED`;
- `CANDIDATE_APPROVED`;
- `ACTIVE_VERIFIED`;
- `PREVIOUS_VERIFIED`;
- `ROLLED_BACK`.

## 25. Atualização automática versus ativação automática

O app pode verificar e baixar atualizações automaticamente quando houver condições adequadas.

A **ativação automática**, porém, somente é permitida quando:

- todas as validações obrigatórias passam;
- a classe de mudança é conhecida;
- nenhum limiar de anomalia é atingido;
- o parser reconhece integralmente o formato;
- todas as dependências entre datasets estão satisfeitas;
- não existe aviso de segurança ou consistência pendente.

Se qualquer uma dessas condições não for demonstrável, a candidata fica bloqueada e a ativa permanece em uso.

## 26. Experiência do usuário

A interface deve explicar estado e não exigir conhecimento técnico para operar.

Exemplo de estado saudável:

```
Banco jurídico
CTB       Atualizado
MBFT      Atualizado
CONTRAN   Verificado hoje
SENATRAN  Verificado hoje

Última atualização validada: 13/10/2026
```

Exemplo de atualização aprovada:

```
Nova versão encontrada

CTB
2 artigos alterados
1 artigo incluído
0 removidos sem justificativa

Validação concluída
Atualização segura para instalação
```

Exemplo de bloqueio:

```
Nova versão encontrada

A estrutura da fonte oficial mudou e não pode ser validada
com segurança por esta versão do Enquadra+.

A atualização não foi aplicada.
A base validada instalada continua em uso.
```

O aplicativo não deve assustar o usuário com falhas internas nem ocultar que a base permaneceu antiga por segurança.

## 27. Execução em segundo plano

Uma implementação Android poderá usar mecanismo apropriado de trabalho persistente, como WorkManager, para verificações periódicas, respeitando conectividade, bateria e políticas da plataforma.

Essa escolha é de implementação e não altera as regras de confiança.

Nenhum job em segundo plano pode promover uma base que não tenha passado pelo mesmo pipeline de validação exigido para uma atualização iniciada manualmente.

## 28. Segurança de rede e arquivos

O atualizador é uma nova superfície de ataque e deve ser tratado como entrada não confiável.

Requisitos mínimos:

- HTTPS obrigatório;
- allowlist de origens;
- limites de download;
- timeout;
- armazenamento temporário privado do app;
- nomes de arquivos gerados internamente, nunca confiados à resposta;
- nenhuma extração de arquivo pode escapar do diretório temporário;
- proteção contra ZIP path traversal se formatos compactados vierem a ser aceitos;
- parser deve tratar todo conteúdo externo como não confiável;
- nenhuma execução de código recebido;
- nenhuma WebView necessária para parsing;
- limpeza de temporários após falha;
- logs sem dados sensíveis;
- ausência de credenciais privadas para consultar fontes públicas.

O app não deve incorporar segredo de servidor porque a arquitetura não depende de servidor próprio.

## 29. Integridade e autenticidade

SHA-256 protege contra alteração acidental e permite identificar versões, mas não prova sozinho autoria da fonte.

A confiança é construída pela combinação de:

- HTTPS;
- origem oficial allowlisted;
- URL e redirecionamento controlados;
- conteúdo recebido;
- hash registrado;
- parser conhecido;
- invariantes;
- diff;
- proveniência.

Se uma fonte oficial disponibilizar futuramente assinatura digital verificável, ela deverá ser aproveitada como camada adicional, sem substituir as demais validações.

## 30. Persistência de dados do usuário

Dados pessoais do usuário, favoritos, histórico, preferências e outros registros locais não podem compartilhar uma estratégia destrutiva com o banco normativo.

A promoção ou rollback de uma base jurídica deve preservar os dados do usuário.

Se continuarem no mesmo arquivo Room, migrações e isolamento devem garantir essa propriedade. Preferencialmente, a evolução arquitetural deve considerar separação entre dados normativos versionáveis e dados pessoais persistentes quando isso reduzir risco.

## 31. Observabilidade local

O aplicativo deve manter histórico mínimo das verificações:

- quando verificou;
- quais fontes;
- se havia alteração;
- versão candidata;
- resultado;
- motivo de bloqueio;
- promoção;
- rollback.

Esse histórico deve ser local e voltado à auditabilidade.

Não é necessário serviço remoto de telemetria para que o mecanismo funcione.

## 32. Testabilidade

O atualizador só poderá ser considerado confiável se puder ser testado com fixtures imutáveis.

Cada parser deverá possuir testes para:

- fonte válida conhecida;
- fonte idêntica;
- inclusão legítima;
- alteração legítima;
- remoção suspeita;
- campo ausente;
- documento truncado;
- HTML de erro no lugar da fonte;
- mudança de layout;
- código duplicado;
- referência quebrada;
- hash diferente com conteúdo semanticamente igual, quando aplicável;
- conteúdo malicioso ou excessivamente grande;
- falha durante promoção;
- rollback.

Devem existir testes de fronteira e testes de regressão para bugs encontrados em auditorias anteriores.

## 33. Reprodutibilidade

Sempre que possível, uma geração deve ser reproduzível offline a partir das fontes brutas armazenadas para teste.

A saída canônica deve permitir comparar byte a byte ou hash a hash duas execuções do mesmo parser sobre a mesma entrada.

Diferença não explicada entre duas execuções do mesmo parser é falha de determinismo e bloqueia promoção.

## 34. Política de compatibilidade

O app deve distinguir três situações:

### Dados mudaram, formato conhecido

Pode ser atualizado sem novo APK, se todas as validações passarem.

### Formato mudou, parser ainda consegue provar compatibilidade

Pode ser atualizado sem APK somente se a compatibilidade estiver coberta por regras e testes já instalados.

### Formato mudou de modo não reconhecido

Atualização bloqueada. Pode ser necessária nova versão do APK.

Essa limitação é intencional.

O objetivo não é prometer que uma versão antiga do app entenderá qualquer documento futuro. O objetivo é garantir que ela **não destrua uma base correta quando não entender o futuro**.

## 35. Política de retenção

A implementação deverá definir limites de armazenamento, mas nunca apagar a única geração anterior validada antes de existir uma nova ativa validada.

Arquivos brutos temporários de candidatas bloqueadas podem ser eliminados depois de registrar hash e diagnóstico suficiente, exceto quando o usuário optar por exportar diagnóstico.

Snapshots antigos além do mínimo de rollback podem ser limpos conforme política de espaço.

## 36. Ordem inicial de implementação

O bloco 6 deve ser desenvolvido em etapas pequenas e verificáveis.

### Etapa A — infraestrutura de gerações

Criar modelo de geração, manifesto, estado candidata/ativa/anterior e mecanismo de promoção/rollback, ainda usando fixtures locais.

### Etapa B — validador genérico

Implementar validações de origem, hash, schema, contagem, integridade, diff e política fail-closed.

### Etapa C — primeiro atualizador real

Escolher a fonte mais simples e estável para validar a arquitetura ponta a ponta. O catálogo de atos normativos tende a ser candidato melhor do que MBFT para o primeiro conector real, pois possui estrutura de índice mais previsível.

### Etapa D — CTB

Adicionar atualização direta do texto compilado, com diff artigo a artigo.

### Etapa E — MBFT

Somente depois da infraestrutura estar comprovada. MBFT exige parser mais conservador e validação mais forte.

### Etapa F — datasets derivados

Recalcular relações e demais datasets dependentes somente após a promoção compatível de suas fontes.

### Etapa G — interface de atualização

Expor versão instalada, última verificação, mudanças, bloqueios e rollback de forma simples.

## 37. Critérios mínimos para concluir o bloco 6

O bloco não será considerado concluído apenas porque o app “consegue baixar uma atualização”.

Para conclusão, deve existir evidência de que:

- nenhuma atualização escreve diretamente na ativa;
- candidata é isolada;
- parser é versionado;
- fonte é allowlisted e rastreada;
- manifesto contém hashes e proveniência;
- validações falham fechadas;
- diff é obrigatório;
- promoção é atômica;
- rollback funciona offline;
- formatos desconhecidos são recusados;
- datasets dependentes são validados;
- testes cobrem corrupção, truncamento e mudanças estruturais;
- a última base validada permanece utilizável diante de qualquer falha simulada;
- nenhuma IA é necessária para aprovar a atualização.

## 38. Regras proibitivas

É proibido no bloco de atualização:

- sobrescrever base ativa enquanto faz parsing;
- aceitar fonte sem proveniência;
- aceitar domínio não aprovado por redirecionamento;
- corrigir automaticamente código desconhecido por “parecido”;
- deduzir revogação apenas por desaparecimento;
- preencher dado oficial por IA;
- reduzir validações para permitir que atualização passe;
- apagar snapshot anterior antes da promoção comprovada;
- misturar dados de fontes obtidas em gerações diferentes sem manifestar dependência;
- ativar candidata parcialmente validada;
- tratar erro de parser como “sem mudanças”.

## 39. Precedência e caráter canônico

Este documento é a especificação canônica do mecanismo de atualização local das bases jurídico-normativas.

Para qualquer trabalho relacionado ao bloco 6, atualização remota, download de fonte normativa, criação de candidata, promoção, rollback, parser remoto, versionamento de dataset ou sincronização de bases, este dossiê deve ser consultado antes da implementação.

Em caso de conflito entre uma implementação futura e este documento, a implementação deve ser considerada incorreta até que a decisão arquitetural seja formalmente alterada neste dossiê e refletida nos documentos canônicos relacionados.

Mudanças relevantes neste mecanismo devem atualizar este dossiê antes ou junto da implementação.

## 40. Decisão final

O Enquadra+ continuará **local-first**, mas passará a ser capaz de se manter atualizado sem infraestrutura própria.

A arquitetura desejada é:

> **consulta direta às fontes oficiais + base candidata isolada + validação determinística + comparação diferencial + promoção atômica + rollback.**

O sistema deve ser desenhado para preservar uma base correta mesmo diante de:

- internet instável;
- documento truncado;
- mudança de layout;
- erro do órgão na publicação;
- erro de parser;
- mudança normativa inesperada;
- conteúdo malformado;
- interrupção do processo;
- versão futura que o aplicativo ainda não saiba interpretar.

A regra final é simples:

> **Se o Enquadra+ não puder provar que a candidata é segura, ele não atualiza.**
