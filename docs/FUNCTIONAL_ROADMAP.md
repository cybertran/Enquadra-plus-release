# Roteiro funcional canônico — Enquadra+

Este documento é o guia funcional de referência do projeto. Antes de escolher um novo bloco de desenvolvimento, deve-se consultar este roteiro e o estado atual do repositório. Uma prioridade nova que altere esta ordem deve ser registrada aqui antes ou junto da implementação.

## Regra de fontes

O Enquadra+ usa fontes públicas e oficiais como base de verdade. Conteúdo autoral de terceiros, inclusive análises, orientações jurídicas, modelos próprios e análises de jurisprudência, não deve ser copiado. Quando houver espaço para lógica ou conteúdo editorial próprio, ele deve ser produzido de forma independente e claramente separado do conteúdo oficial.

## Roteiro funcional

| Funcionalidade | Natureza da fonte | Diretriz |
| --- | --- | --- |
| Pesquisa de infrações | 🟢 Dados públicos | Pode implementar |
| Busca por código de enquadramento | 🟢 Dados públicos | Pode implementar |
| Busca por artigo / palavra-chave | 🟢 Dados públicos | Pode implementar |
| CTB atualizado | 🟢 Oficial | Pode disponibilizar/estruturar |
| MBFT atualizado | 🟢 Oficial | Pode disponibilizar/estruturar |
| Resoluções CONTRAN/SENATRAN | 🟢 Oficiais | Pode disponibilizar/estruturar |
| Enquadramento da infração | 🟢 MBFT/CTB | Pode implementar |
| Tipificação | 🟢 MBFT/CTB | Pode implementar |
| Combinação de enquadramentos | 🟡 Misto | Pode criar lógica própria baseada no MBFT/CTB |
| Competência para autuação | 🟢 MBFT | Pode implementar |
| Forma de constatação | 🟢 MBFT | Pode implementar |
| Pontuação | 🟢 CTB/MBFT | Pode implementar |
| Responsabilidade pela infração | 🟢 CTB/MBFT | Pode implementar |
| Natureza da infração | 🟢 CTB/MBFT | Pode implementar |
| Penalidades | 🟢 CTB/MBFT | Pode implementar |
| Medidas administrativas | 🟢 CTB/MBFT | Pode implementar |
| Amparo legal | 🟢 Oficial | Pode implementar |
| Hipóteses de autuar/não autuar | 🟢 MBFT | Pode implementar |
| Observações obrigatórias/recomendadas para AIT | 🟡 Misto | Usar as previstas oficialmente; não copiar recomendações próprias de terceiros |
| Modelos de observações para AIT | 🟡 Misto | Exemplos do MBFT podem ser usados; modelos autorais de terceiros, não |
| Análises e orientações jurídicas | 🔴 Conteúdo autoral de terceiros | Não copiar; somente conteúdo próprio e independente |
| Calculadora/fiscalização de alcoolemia | 🟢 Regras públicas | Pode implementar |
| Conjuntos de sinais de alteração psicomotora | 🟢 Oficial | Pode implementar |
| Consulta sobre engate | 🟡 Misto | Pode criar com normas e dados obtidos independentemente |
| Ferramentas sobre CNH/habilitação | 🟢 Regras públicas | Pode implementar |
| Equipamentos obrigatórios | 🟢 Normas públicas | Pode implementar |
| Dicionário de termos | 🟡 Misto | Criar conteúdo próprio usando definições legais/oficiais |
| Crimes de trânsito | 🟢 CTB | Pode implementar |
| Análises de jurisprudência ligadas aos crimes | 🔴 Conteúdo autoral de terceiros | Decisões são públicas; análises de terceiros não devem ser copiadas |
| Licenciamento, calendários e prazos | 🟢/🟡 | Pode implementar com dados oficiais dos órgãos |
| Atualizações do banco jurídico/normativo | 🟡 Serviço/curadoria | Pode criar sistema próprio de atualização |
| Valor monetário da multa | 🟢 CTB/MBFT | Calcular pelo art. 258 e multiplicadores expressos na ficha; valores variáveis devem permanecer identificados como variáveis |
| Filtros operacionais de infrações | 🟢 Dados já estruturados | Filtrar por natureza, pontos, competência, constatação e possibilidade de crime sem alterar a base oficial |
| Favoritos, histórico e anotações | 🟡 Dados locais do usuário | Armazenar separadamente da base normativa para sobreviver a atualização e rollback; não apresentar anotações como conteúdo oficial |
| Placas e sinalização | 🟢 Oficial | Usar os Manuais Brasileiros de Sinalização da SENATRAN; imagens devem vir do documento oficial, sem redesenho aproximado |
| Deliberações CONTRAN | 🟢 Oficial | Integrar ao catálogo normativo com a mesma proveniência e atualização segura das Resoluções |
| Regulamentações vinculadas à ficha | 🟢/🟡 | Resolver somente referências normativas explícitas no MBFT/fonte, sem associação jurídica por similaridade |
| PDFs oficiais | 🟢 Oficial | Visualizar, salvar e compartilhar apenas documentos HTTPS de origem oficial allowlisted, com validação de redirecionamento, tamanho e cabeçalho |
| Guias operacionais | 🟡 Misto | Produzir fluxos próprios a partir de CTB/MBFT e normas oficiais; não copiar guias ou fluxogramas de terceiros |
| Assistente de observação do AIT | 🟡 Misto | Compor texto somente a partir de fatos informados pelo usuário e referências oficiais; não inventar fatos nem conclusão jurídica |
| Calendários estaduais de licenciamento | 🟢 Oficial | Incluir somente UFs/exercícios conferidos em fonte oficial do órgão competente, com cobertura explícita e sem inferência de datas |
| Alertas de atualização | 🟡 Serviço local | Notificar de forma opt-in apenas quando houver candidata normativa validada; nunca aplicar automaticamente por causa da notificação |
| Atualização do aplicativo | 🟡 Distribuição oficial | Verificar manualmente a última release oficial assinada, comparar `versionCode` e oferecer download no GitHub; manter separado das atualizações jurídicas e nunca instalar silenciosamente |

## Modelo de acesso

O Enquadra+ é gratuito e de uso universal. Assinatura, período de teste, paywall, plano comercial e login obrigatório para acesso às funções principais não fazem parte do produto e não devem ser tratados como lacunas de paridade com aplicativos comerciais.

## Ordem operacional atual

A ordem de trabalho deve considerar o que já existe no aplicativo e evitar reconstruir funcionalidades concluídas.

1. **Atualizado em 15/10/2026:** a base vigente possui 409 fichas, sendo 407 `VERIFIED` e 2 deliberadamente em transição normativa pela Resolução CONTRAN nº 1.031/2026; os códigos toxicológicos supersededidos permanecem removidos.
2. **Concluído em 13/10/2026:** CTB estruturado como base local pesquisável, com artigos e Anexo I importados do texto compilado oficial do Planalto, índice FTS, proveniência e consulta offline.
3. **Concluído em 13/10/2026:** Resoluções CONTRAN e Portarias SENATRAN estruturadas como catálogo normativo local pesquisável e rastreável, com situação/observação oficial, links de origem, versionamento por hash e consulta offline dos metadados.
4. **Concluído em 13/10/2026:** relações e combinações entre enquadramentos estruturadas em dataset versionado, com regras oficiais de alternativa, concomitância e pré-requisito derivadas apenas de fichas MBFT `VERIFIED`, além de relações curadas suplementares claramente separadas do texto oficial.
5. **Concluído em 13/10/2026:** auditoria e consolidação das ferramentas operacionais existentes. Alcoolemia foi completada com o fluxo da Resolução CONTRAN nº 1.031/2026; engate e CNH foram corrigidos; equipamentos obrigatórios passaram a seguir a matriz auditada da Resolução CONTRAN nº 993/2023 com 53 requisitos, 16 tipos de veículo, 848 células explícitas, 462 marcações aplicáveis e 49 notas; o dicionário foi provado termo a termo em 37 entradas; sinais psicomotores, crimes de trânsito e licenciamento/prazos foram reauditados sem divergência material remanescente.
6. **Concluído em 13/10/2026:** sistema local e autônomo de atualização segura das bases jurídico-normativas, conforme [SAFE_NORMATIVE_UPDATE_DOSSIER.md](SAFE_NORMATIVE_UPDATE_DOSSIER.md). CTB, catálogo CONTRAN/SENATRAN e MBFT usam gerações isoladas, proveniência, validação fail-closed, diff, promoção atômica e rollback. O MBFT usa registro fechado de emendas auditadas: alterações desconhecidas são bloqueadas, e a Resolução CONTRAN nº 1.031/2026 já possui transformação determinística preparada para sua vigência em 17/10/2026, condicionada ao SHA-256 exato do PDF oficial e à reconstrução válida das relações. Registro técnico: [audits/SAFE_UPDATE_IMPLEMENTATION_2026-10-13.md](audits/SAFE_UPDATE_IMPLEMENTATION_2026-10-13.md).
7. **Concluído em 15/10/2026:** bloco de produtividade operacional v0.16, com valor monetário da multa calculado a partir do art. 258 do CTB e dos multiplicadores expressos no MBFT, filtros operacionais e dados pessoais de uso (favoritos, histórico e anotações) persistidos em banco separado da base normativa.
8. **Concluído em 15/10/2026:** bloco v0.17 de sinalização e documentos oficiais: nove volumes do Manual Brasileiro de Sinalização acessíveis pelo app, catálogo pesquisável dos 57 sinais de regulamentação com visual oficial, Deliberações CONTRAN no catálogo normativo, atos explicitamente citados vinculados às fichas e visualizador interno de PDFs oficiais com política de origem restrita.
9. **Concluído em 15/10/2026:** bloco v0.18 de fechamento do núcleo de paridade operacional: todas as 409 fichas foram auditadas para vínculos normativos com evidência oficial; o pacote offline passou a indexar 3.141 de 3.192 atos (98,4% do catálogo) e 100% dos 3.017 documentos PDF, com FTS próprio; e os nove volumes oficiais de sinalização passaram a ter catálogo pesquisável com 1.038 entradas. A auditoria comparativa posterior identificou lacunas complementares de produtividade, documentadas em [QAP_FUNCTIONAL_PARITY_AUDIT.md](QAP_FUNCTIONAL_PARITY_AUDIT.md).
10. **Em implementação em 15/10/2026:** bloco v0.19 de paridade operacional complementar: guias operacionais próprios, calendários estaduais 2026 somente para UFs auditadas em fonte oficial, assistente de observação do AIT baseado em fatos do usuário, alertas locais opt-in para candidatas normativas validadas e salvar/compartilhar PDFs oficiais. Análise jurídica autoral, conteúdo editorial proprietário e camada comercial continuam fora do escopo.
11. **Implementado em 21/10/2026 para v0.2.0-alpha.10:** verificação manual de atualização do próprio aplicativo na tela Versão, por manifesto público de release oficial assinada, com comparação por `versionCode`, validação restritiva do manifesto e download iniciado somente por decisão do usuário. O fluxo permanece independente das atualizações jurídico-normativas.

## Estado funcional já existente

O repositório já possui pesquisa de infrações, busca FTS por código/artigo/palavra-chave e estrutura de fichas com tipificação, natureza, pontuação, responsabilidade, competência, constatação, penalidades, medidas administrativas, amparo legal, hipóteses de autuar/não autuar, procedimentos e observações oficiais. As relações entre códigos agora distinguem alternativa, concomitância, pré-requisito e relação curada, preservando a evidência oficial quando aplicável.

Também já existem ferramentas funcionais para alcoolemia, sinais de alteração psicomotora, CNH/habilitação, equipamentos obrigatórios, engate, crimes de trânsito, licenciamento/prazos e dicionário.

A tela de normas oferece acesso a fontes oficiais de CTB, MBFT, CONTRAN e SENATRAN. O CTB funciona como base local de texto pesquisável, incluindo artigos e Anexo I. Resoluções e Deliberações CONTRAN e Portarias SENATRAN também possuem catálogo local pesquisável, com metadados oficiais e rastreabilidade até o documento e o índice de origem.

## Manutenção deste roteiro

Ao concluir um bloco, registrar a conclusão neste documento ou na documentação diretamente vinculada a ele antes de iniciar o próximo. Se uma nova decisão mudar a prioridade, escopo ou fonte admitida, atualizar este roteiro para que o repositório continue sendo a referência canônica do projeto.
