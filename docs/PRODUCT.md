# Escopo funcional — Enquadra+ v0.1

## Objetivo

O Enquadra+ será uma ferramenta Android gratuita e de uso universal para consulta rápida e apoio à fiscalização de trânsito. Toda informação normativa deve ser obtida de fontes oficiais, armazenada com referência de origem e apresentada por uma interface própria.

## Modelo de acesso

O Enquadra+ é concebido para uso gratuito e universal. O acesso às funcionalidades principais não deve depender de assinatura, período de teste, paywall, compra, plano comercial ou login obrigatório.

Conta, autenticação ou sincronização só poderão ser consideradas futuramente se houver uma necessidade técnica independente e claramente justificada, sem transformar o acesso ao conteúdo operacional principal em serviço pago ou restrito.

## Roteiro funcional canônico

A ordem funcional e as regras de fonte que orientam o desenvolvimento estão registradas em [FUNCTIONAL_ROADMAP.md](FUNCTIONAL_ROADMAP.md). Esse documento deve ser consultado antes da escolha de um novo bloco de implementação.

O mecanismo de atualização local das bases jurídico-normativas possui especificação canônica própria em [SAFE_NORMATIVE_UPDATE_DOSSIER.md](SAFE_NORMATIVE_UPDATE_DOSSIER.md). Qualquer funcionalidade de consulta remota, geração candidata, promoção ou rollback deve obedecer a esse dossiê.

A atualização do próprio aplicativo é independente desse mecanismo. Em **Versão**, o usuário pode solicitar uma verificação manual da última release oficial assinada. Uma versão nova é apenas informada e oferecida para download no GitHub; não há download em segundo plano, instalação silenciosa nem aplicação automática.

## Diretriz canônica de UX operacional

A interface deve separar claramente três camadas:

- **Uso diário:** cada tela operacional mostra somente controles, dados e resultados necessários para concluir a tarefa, sem blocos permanentes sobre metodologia, auditoria, confiabilidade, cobertura da base ou funcionamento interno do sistema.
- **Fonte específica:** quando a origem jurídica de uma informação precisar ser consultada, a tela oferece uma ação discreta como **“Ver fonte”**, sem transformar a fonte em um bloco visual permanente.
- **Transparência do sistema:** versões instaladas, estado de atualização, proveniência, hashes, geração candidata, diff, rollback, política fail-closed e demais diagnósticos ficam concentrados em **Configurações > Base jurídica e atualizações** e, quando técnicos, atrás de **“Detalhes técnicos”**. A tela **Normas** é destinada à consulta do conteúdo jurídico e documental, não à manutenção das bases.

Exceções são admitidas quando uma observação jurídica for necessária para interpretar corretamente o resultado operacional. A interface continua própria, em Material Design 3, sem reproduzir identidade visual, organização editorial ou trade dress de aplicativos de terceiros.

A tela inicial deve permanecer estritamente operacional e assumir a forma de um painel administrativo enxuto. A Home prioriza **consulta rápida de infrações**, **consultas recentes**, **favoritos** e um **resumo compacto da base jurídica**. Ao acionar a **Consulta rápida**, a tela **Infrações** deve abrir com o campo de pesquisa já focado e o teclado disponível para digitação imediata; a navegação normal para Infrações não deve forçar esse comportamento. Esse resumo pode indicar apenas estados simples como base instalada, conteúdo atualizado, atualização disponível ou necessidade real de atenção. Uma falha transitória ao consultar uma fonte remota, quando a base local continua íntegra e disponível, não deve deixar a Home em alerta vermelho permanente; nesse caso, a Home mostra a base jurídica instalada e, quando identificável pela versão ativa, a data da base em uso. Versões completas, hashes, diffs, rollback e demais diagnósticos continuam fora da Home. Versão do aplicativo, capacidades técnicas, contagens internas da base e informações de desenvolvimento não devem ocupar blocos permanentes na Home. A navegação secundária deve usar um menu lateral acessível pelo botão de menu no topo, com **Configurações**, **Sobre**, **Versão** e **Desenvolvedor**. A ação de verificar e aplicar atualizações jurídicas deve permanecer acessível em **Configurações > Base jurídica e atualizações**, e o resumo da Home pode encaminhar diretamente para essa área.

A tela **Sobre** pode concentrar identidade do aplicativo e informações institucionais de baixa frequência: marca, nome, subtítulo, versão/build, finalidade, projeto, informações técnicas essenciais, privacidade e funcionamento local-first. Ela também pode oferecer uma ação discreta **“Enviar feedback”**, que abre o formulário público do projeto no Google Forms somente por decisão do usuário. Quando o formulário é aberto pelo app, o Enquadra+ pode pré-preencher um identificador aleatório da instalação e a versão/build para correlação técnica e diagnóstico. Esse identificador é um UUID próprio, armazenado em `noBackupFilesDir`, não usa `ANDROID_ID`, IMEI, Advertising ID, telefone, conta Google ou outro identificador pessoal/plataforma, e é renovado após desinstalação/reinstalação. O formulário permanece utilizável sem identificação pessoal obrigatória e o e-mail é opcional. Os dados técnicos ficam em um campo próprio, visível e editável no Google Forms; não são tratados como campo oculto nem como prova de identidade. A tela **Desenvolvedor** concentra autoria, contato técnico, repositório e acessos de diagnóstico. Esses conteúdos devem permanecer fora da Home operacional.

A identidade visual do aplicativo adota como marca principal a arte **“E+”**, com “E” branco e “+” dourado sobre fundo azul-marinho. O arquivo fornecido para a identidade oficial deve ser usado no launcher Android e na apresentação institucional da tela Sobre, sem a borda branca externa da arte original, preservando legibilidade em tamanhos reduzidos e compatibilidade com ícones adaptativos.


### Arquitetura canônica da informação operacional

A navegação deve preservar a natureza de cada função, evitando reunir conteúdos distintos apenas porque tratam de trânsito:

- **Início:** painel operacional com consulta rápida de infrações, até três consultas recentes, acesso às infrações favoritas, atalho compacto para Normas e resumo simples da base jurídica. **Infrações**, **Normas** e **Ferramentas** continuam como destinos de nível superior na navegação inferior; a Home não replica o antigo conjunto de grandes cards de navegação.
- **Infrações:** busca e ficha operacional. A ficha concentra os campos do enquadramento, hipóteses de autuar/não autuar, assistente contextual de observação do AIT e links discretos para fontes/normas relacionadas. Evidências de auditoria e detalhes de proveniência não ocupam a leitura principal.
- **Normas:** CTB, fonte oficial do MBFT, Resoluções e Deliberações CONTRAN, atos SENATRAN, texto integral pesquisável e os nove volumes oficiais de sinalização. A consulta estruturada das fichas MBFT permanece em Infrações.
- **Ferramentas:** organizada por natureza. **Fiscalização guiada** reúne alcoolemia, CNH, equipamentos, engate e licenciamento; **Consultas rápidas** reúne crimes de trânsito e dicionário; **Guias operacionais** permanece como apoio próprio separado das ferramentas de cálculo/checklist.
- **Fiscalização de alcoolemia:** medição por etilômetro e sinais de alteração da capacidade psicomotora pertencem ao mesmo fluxo operacional, não a ferramentas independentes.
- **Configurações > Base jurídica e atualizações:** concentra verificação, aplicação, versões, diff, rollback e diagnósticos das bases.

Sinalização pode possuir atalho contextual em outros pontos no futuro, mas sua localização canônica é a base documental em **Normas**.

### Diretriz canônica para seletores, filtros e opções

A interface não deve usar o mesmo padrão de retângulo contornado para seleção, filtro, metadado e ação. Seleções e filtros devem priorizar componentes tonais leves e arredondados; listas longas de escolha única devem usar seletor pesquisável ou lista de seleção; checklists devem usar linhas com indicador de estado; metadados devem ser badges não clicáveis; e botões devem ser reservados a ações reais. Contornos fortes e grades extensas de chips devem ser evitados.

A aplicação por tela está registrada em [UI_SELECTION_CONTROLS_AUDIT_2026-10-16.md](UI_SELECTION_CONTROLS_AUDIT_2026-10-16.md). Esta diretriz é visual/estrutural e não altera regras jurídicas ou funcionais.

## Funcionalidades previstas

### Consulta de infrações

- Pesquisa de infrações, com resultados cujo título começa pelo termo buscado priorizados antes de ocorrências do termo no meio do título quando a busca é textual. Quando a consulta é numérica (código ou artigo), correspondências exatas de código de enquadramento têm prioridade, seguidas das correspondências exatas de artigo, correspondências parciais de identificadores e, por fim, a tipificação e os demais campos indexados. A busca por artigo distingue artigos com sufixo (ex.: 180 e 180-A). A consulta deve ser tolerante a palavras de ligação comuns do português, como `em`, `de`, `do`, `da`, `a`, `o` e `e`, para que expressões como `transitar em` não eliminem resultados válidos. Durante a digitação, a consulta deve usar debounce curto para evitar uma ida ao banco a cada tecla e um último token alfabético ainda incompleto de um único caractere não pode zerar prematuramente os resultados.
- Busca por código de enquadramento.
- Busca por artigo e palavra-chave.
- Enquadramento e tipificação.
- Natureza e pontuação.
- Responsabilidade pela infração.
- Competência para autuação.
- Forma de constatação.
- Penalidades.
- Medidas administrativas.
- Amparo legal.
- Hipóteses oficiais de autuar e não autuar.
- Observações oficiais ou exemplos constantes dos documentos oficiais.
- Relação entre enquadramentos construída a partir das fontes oficiais.
- Regulamentações e atos relacionados por evidência oficial auditável: referência explícita no MBFT/fonte ou citação direta do artigo do CTB no texto integral do ato.

### Produtividade operacional

- Exibição do valor monetário da multa quando determinável diretamente pelo CTB/MBFT.
- Filtros por natureza, pontuação, competência, forma de constatação e possibilidade de crime.
- Favoritos.
- Histórico de fichas consultadas.
- Anotações pessoais vinculadas ao código da infração, armazenadas separadamente da base jurídico-normativa.
- Assistente de observação do AIT que organiza somente fatos informados pelo usuário, exibindo separadamente os exemplos oficiais da ficha e sem inventar fatos, enquadramento ou conclusão jurídica.
- Guias operacionais próprios para acesso rápido a fluxos de fiscalização, com redação independente e referência às bases oficiais.

### Base normativa

- CTB.
- MBFT.
- Resoluções CONTRAN.
- Deliberações CONTRAN.
- Normas e atos da Senatran.
- Visualização interna de PDFs oficiais quando o ato disponibilizar documento direto em origem allowlisted, com opção de salvar uma cópia e compartilhar o arquivo validado por URI temporária.
- Pesquisa integrada no conteúdo normativo, incluindo FTS do texto integral offline dos atos extraíveis das fontes oficiais.
- Controle de versão e data de verificação das fontes.

### Ferramentas

- Fiscalização e cálculo de alcoolemia conforme parâmetros oficiais.
- Conjunto de sinais de alteração da capacidade psicomotora.
- Consulta de regras relativas a engate.
- CNH e habilitação.
- Equipamentos obrigatórios.
- Dicionário construído a partir de definições oficiais e redação própria.
- Crimes de trânsito previstos na legislação.
- Licenciamento, calendários e prazos oficiais, incluindo calendários estaduais apenas quando o exercício e a UF tiverem sido conferidos em fonte oficial e com a cobertura exibida ao usuário.
- Placas e sinalização: acesso aos nove volumes oficiais do Manual Brasileiro de Sinalização e catálogo pesquisável gerado por volume, página, código e seção; os 57 sinais de regulamentação permanecem como conjunto auditado do Volume I.
- Alertas locais opt-in quando uma nova candidata normativa tiver passado pelas validações determinísticas e estiver disponível para decisão do usuário; a notificação nunca promove a atualização automaticamente.

## Fora do escopo

O aplicativo não terá camada comercial de acesso, assinatura, período gratuito seguido de cobrança, paywall ou bloqueio das funcionalidades principais por conta/plano.

O aplicativo não copiará análises jurídicas, orientações jurídicas, comentários, explicações autorais, análises de jurisprudência, modelos autorais de terceiros, guias ou fluxogramas proprietários, identidade visual, banco extraído de outro aplicativo ou organização editorial proprietária.

A matriz pública de comparação funcional que orienta o fechamento da paridade operacional está em [QAP_FUNCTIONAL_PARITY_AUDIT.md](QAP_FUNCTIONAL_PARITY_AUDIT.md).

Jurisprudência pública poderá futuramente ser referenciada como fonte primária, mas qualquer resumo ou análise será produzido de forma independente e identificado como conteúdo próprio do Enquadra+.
