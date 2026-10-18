# Auditoria formal das ferramentas — 13/10/2026

## Escopo e método

Esta auditoria foi executada sobre o bloco empilhado em `feat/infraction-relations-v0.5`. O objetivo é validar as ferramentas operacionais já existentes contra as fontes oficiais, corrigir divergências e deixar evidência reexecutável.

Fontes primárias usadas:
- CTB compilado do Planalto, já empacotado e versionado em `app/src/main/assets/ctb/articles.json`;
- Resolução CONTRAN nº 993/2023, Anexos I, II e III;
- Resolução CONTRAN nº 1.031/2026;
- Resolução CONTRAN nº 937/2022;
- Resolução CONTRAN nº 1.020/2025 e CTB para CNH/habilitação.

A prova possui duas camadas: revisão humana termo/célula e validação determinística por `tools/audit/validate_tooling_audit.py`.

## Dicionário — prova termo a termo

O conjunto possui exatamente 37 entradas. Cada entrada abaixo foi conferida contra o cabeçalho oficial correspondente no Anexo I do CTB. `CONFORME` significa que o resumo preserva os elementos definidores relevantes; `AJUSTADO` significa que a auditoria encontrou perda semântica e o texto foi corrigido.

| # | Termo no app | Cabeçalho oficial | Resultado | Prova semântica |
| ---: | --- | --- | --- | --- |
| 1 | Acostamento | ACOSTAMENTO | AJUSTADO | Restauradas a diferenciação da pista, a finalidade de emergência e a condição para pedestres/bicicletas. |
| 2 | Agente da autoridade de trânsito | AGENTE DA AUTORIDADE DE TRÂNSITO | AJUSTADO | Restauradas as categorias de agente e a exigência de designação legal quando aplicável. |
| 3 | Automóvel | AUTOMÓVEL | CONFORME | Resumo preserva finalidade e limite de passageiros, excluído o condutor. |
| 4 | Autoridade de trânsito | AUTORIDADE DE TRÂNSITO | AJUSTADO | Restaurada a vinculação ao órgão ou entidade executivo integrante do SNT. |
| 5 | Bicicleta | BICICLETA | CONFORME | Resumo preserva propulsão humana, duas rodas e distinção legal. |
| 6 | Bicicletário | BICICLETÁRIO | AJUSTADO | Restaurada a informação de que o local pode estar na via ou fora dela. |
| 7 | Caminhão | CAMINHÃO | CONFORME | Resumo preserva transporte de carga, PBT superior a 3.500 kg e limite pela CMT. |
| 8 | Caminhão-trator | CAMINHÃO-TRATOR | CONFORME | Resumo preserva a finalidade de tracionar ou arrastar outro veículo. |
| 9 | Caminhonete | CAMINHONETE | CONFORME | Resumo preserva transporte de carga e PBT até 3.500 kg. |
| 10 | Camioneta | CAMIONETA | CONFORME | Resumo preserva transporte misto no mesmo compartimento. |
| 11 | Calçada | CALÇADA | AJUSTADO | Restauradas segregação/nível, exclusão do tráfego veicular e demais usos permitidos. |
| 12 | Ciclo | CICLO | CONFORME | Resumo preserva ao menos duas rodas e propulsão humana. |
| 13 | Ciclofaixa | CICLOFAIXA | AJUSTADO | Restauradas as referências à pista de rolamento e à sinalização específica. |
| 14 | Ciclomotor | CICLOMOTOR | CONFORME | Resumo preserva número de rodas, limites de cilindrada/potência e velocidade. |
| 15 | Ciclovia | CICLOVIA | CONFORME | Resumo preserva pista própria e separação física. |
| 16 | Circulação | CIRCULAÇÃO | AJUSTADO | Restaurada a abrangência de vias públicas ou privadas abertas ao público e o 'conduzidos ou não'. |
| 17 | Conversão | CONVERSÃO | CONFORME | Resumo preserva mudança angular à esquerda ou direita. |
| 18 | Cruzamento | CRUZAMENTO | CONFORME | Resumo preserva interseção de duas vias em nível. |
| 19 | Dispositivo de segurança | DISPOSITIVO DE SEGURANÇA | AJUSTADO | Restauradas a função específica e as consequências de perigo previstas na definição. |
| 20 | Estacionamento | ESTACIONAMENTO | CONFORME | Resumo preserva a imobilização além do tempo de embarque/desembarque. |
| 21 | Estrada | ESTRADA | CONFORME | Resumo coincide semanticamente com via rural não pavimentada. |
| 22 | Etilômetro | ETILÔMETRO | CONFORME | Resumo preserva a medição do teor alcoólico no ar alveolar. |
| 23 | Faixa de trânsito | FAIXAS DE TRÂNSITO | AJUSTADO | Restaurados subdivisão da pista, presença ou ausência de marcas longitudinais e circulação de veículos automotores. |
| 24 | Motocicleta | MOTOCICLETA | AJUSTADO | Restaurada a possibilidade de side-car. |
| 25 | Motoneta | MOTONETA | CONFORME | Resumo preserva duas rodas e posição sentada do condutor. |
| 26 | Ônibus | ÔNIBUS | AJUSTADO | Restaurada a regra de capacidade superior a vinte mesmo com adaptações que reduzam ocupação. |
| 27 | Parada | PARADA | AJUSTADO | Restaurada expressamente a finalidade da imobilização, além do tempo estritamente necessário. |
| 28 | Rodovia | RODOVIA | CONFORME | Resumo coincide semanticamente com via rural pavimentada. |
| 29 | Trânsito | TRÂNSITO | CONFORME | Resumo coincide semanticamente com movimentação e imobilização nas vias terrestres. |
| 30 | Triciclo | TRICICLO | AJUSTADO | Restauradas as posições sentada ou montada do condutor. |
| 31 | Ultrapassagem | ULTRAPASSAGEM | AJUSTADO | Restaurados veículo em menor velocidade e mesma faixa como elementos definidores. |
| 32 | Veículo articulado | VEÍCULO ARTICULADO | AJUSTADO | A redação foi aproximada da definição legal, que exige combinação acoplada com um dos veículos automotor. |
| 33 | Veículo de grande porte | VEÍCULO DE GRANDE PORTE | CONFORME | Resumo preserva os limiares de carga e passageiros. |
| 34 | Veículo especial | VEÍCULO ESPECIAL | AJUSTADO | Restauradas as classes de veículo abrangidas e a necessidade de arranjos específicos para a função especial. |
| 35 | Via | VIA | AJUSTADO | Restaurada expressamente a circulação de veículos, pessoas e animais sobre a superfície. |
| 36 | Via arterial | VIA ARTERIAL | AJUSTADO | Restaurados acessos a lotes e vias secundárias/locais, além da conexão entre regiões. |
| 37 | Via de trânsito rápido | VIA DE TRÂNSITO RÁPIDO | CONFORME | Resumo preserva acessos especiais e as três ausências definidoras. |

Invariantes executáveis:
- exatamente 37 termos no código;
- exatamente 37 registros de prova;
- todos os 37 cabeçalhos oficiais existem no texto local do Anexo I do CTB;
- 18 termos foram ajustados nesta auditoria e 19 permaneceram conformes;
- nenhuma definição é promovida sem correspondência no Anexo I oficial.

## Resolução CONTRAN nº 993/2023 — prova das 53 páginas

O PDF oficial possui 53 páginas. Todas foram inspecionadas. Para o Anexo I, a matriz foi normalizada de forma explícita: 53 linhas de equipamento × 16 tipos de veículo = 848 células. Cada célula possui `X` (aplicável), `X:<notas>` (aplicável com notas) ou `-` (não marcada no Anexo I). Não há inferência por união de categorias.

Resultado estrutural do Anexo I:
- 16 tipos de veículo, preservando as quatro tabelas oficiais;
- 53 linhas de requisitos;
- 848 células explícitas;
- 462 células marcadas como aplicáveis;
- notas 1 a 49 registradas e referenciadas;
- micro-ônibus leve e pesado permanecem separados pela faixa de PBT;
- as exceções por data, categoria e aplicação são exibidas junto do requisito.

O Anexo II é um índice de resoluções e requisitos técnicos gerais. Suas células de aplicabilidade foram conferidas, mas não são convertidas automaticamente em novos itens de equipamento, evitando transformar um índice normativo em obrigação autônoma. O Anexo III foi conferido e vinculado à nota 30 do Anexo I para proteção térmica do sistema de exaustão.

### Cobertura página a página

| Página | Seção | Resultado |
| ---: | --- | --- |
| 1 | ANEXO / I | CELL_BY_CELL_VERIFIED |
| 2 | ANEXO / I | CELL_BY_CELL_VERIFIED |
| 3 | ANEXO / I | CELL_BY_CELL_VERIFIED |
| 4 | ANEXO / I | CELL_BY_CELL_VERIFIED |
| 5 | ANEXO / I | CELL_BY_CELL_VERIFIED |
| 6 | ANEXO / I | CELL_BY_CELL_VERIFIED |
| 7 | ANEXO / I | CELL_BY_CELL_VERIFIED |
| 8 | ANEXO / I | CELL_BY_CELL_VERIFIED |
| 9 | ANEXO / I | CELL_BY_CELL_VERIFIED |
| 10 | ANEXO / I | CELL_BY_CELL_VERIFIED |
| 11 | ANEXO / I | CELL_BY_CELL_VERIFIED |
| 12 | ANEXO / I | CELL_BY_CELL_VERIFIED |
| 13 | ANEXO / I / ANEXO / II | CELL_BY_CELL_VERIFIED |
| 14 | ANEXO / II | CELL_BY_CELL_VERIFIED |
| 15 | ANEXO / II | CELL_BY_CELL_VERIFIED |
| 16 | ANEXO / II | CELL_BY_CELL_VERIFIED |
| 17 | ANEXO / II | CELL_BY_CELL_VERIFIED |
| 18 | ANEXO / II | CELL_BY_CELL_VERIFIED |
| 19 | ANEXO / II | CELL_BY_CELL_VERIFIED |
| 20 | ANEXO / II | CELL_BY_CELL_VERIFIED |
| 21 | ANEXO / II | CELL_BY_CELL_VERIFIED |
| 22 | ANEXO / II | CELL_BY_CELL_VERIFIED |
| 23 | ANEXO / II | CELL_BY_CELL_VERIFIED |
| 24 | ANEXO / II | CELL_BY_CELL_VERIFIED |
| 25 | ANEXO / II | CELL_BY_CELL_VERIFIED |
| 26 | ANEXO / II | CELL_BY_CELL_VERIFIED |
| 27 | ANEXO / II | CELL_BY_CELL_VERIFIED |
| 28 | ANEXO / II | CELL_BY_CELL_VERIFIED |
| 29 | ANEXO / II | CELL_BY_CELL_VERIFIED |
| 30 | ANEXO / II | CELL_BY_CELL_VERIFIED |
| 31 | ANEXO / II | CELL_BY_CELL_VERIFIED |
| 32 | ANEXO / II | CELL_BY_CELL_VERIFIED |
| 33 | ANEXO / II | CELL_BY_CELL_VERIFIED |
| 34 | ANEXO / II | CELL_BY_CELL_VERIFIED |
| 35 | ANEXO / II | CELL_BY_CELL_VERIFIED |
| 36 | ANEXO / II | CELL_BY_CELL_VERIFIED |
| 37 | ANEXO / II | CELL_BY_CELL_VERIFIED |
| 38 | ANEXO / II | CELL_BY_CELL_VERIFIED |
| 39 | ANEXO / II | CELL_BY_CELL_VERIFIED |
| 40 | ANEXO / II | CELL_BY_CELL_VERIFIED |
| 41 | ANEXO / II | CELL_BY_CELL_VERIFIED |
| 42 | ANEXO / II | CELL_BY_CELL_VERIFIED |
| 43 | ANEXO / II | CELL_BY_CELL_VERIFIED |
| 44 | ANEXO / II | CELL_BY_CELL_VERIFIED |
| 45 | ANEXO / II | CELL_BY_CELL_VERIFIED |
| 46 | ANEXO / II | CELL_BY_CELL_VERIFIED |
| 47 | ANEXO / II | CELL_BY_CELL_VERIFIED |
| 48 | ANEXO / II | CELL_BY_CELL_VERIFIED |
| 49 | ANEXO / II | CELL_BY_CELL_VERIFIED |
| 50 | ANEXO / II | CELL_BY_CELL_VERIFIED |
| 51 | ANEXO / II / ANEXO / III | CELL_BY_CELL_VERIFIED |
| 52 | ANEXO / III | CELL_BY_CELL_VERIFIED |
| 53 | ANEXO / III | CELL_BY_CELL_VERIFIED |

A representação auditável está em `app/src/main/assets/tools/equipment_res993.json`. A validação recusa o dataset se faltar página, veículo, linha, célula ou nota, ou se houver marcador fora da gramática admitida.

## Correções funcionais aplicadas

### Alcoolemia

O cálculo do etilômetro foi preservado e a exibição do erro máximo deixou de truncar artificialmente o valor intermediário. A tela agora cobre também triagem/pré-teste, requisitos do etilômetro, reteste por comprometimento técnico/operacional, recusa, distinção 165/165-A, dados essenciais do AIT, hipótese do art. 306 e medidas relativas ao veículo e à CNH.

### Sinais psicomotores

Reauditado sem correção funcional adicional. O app mantém a exigência de ao menos dois sinais que, em conjunto, comprovem alteração, sem transformar a contagem isolada em conclusão automática.

### Engate

Incluído o requisito do art. 5º da Resolução 937/2022: a nota de venda deve identificar o veículo em que o engate foi instalado. O resultado final foi renomeado para checklist normativo, evitando sugerir mera conferência documental.

### CNH

Corrigida a regra de motor-casa da categoria B. O CTB usa condição alternativa: peso de até 6.000 kg **ou** lotação de até oito lugares, excluído o motorista.

### Equipamentos obrigatórios

Removida a redução para apenas três grupos genéricos. A consulta agora segue os 16 tipos/colunas das quatro tabelas do Anexo I e exibe as notas condicionais por célula.

### Crimes de trânsito e licenciamento/prazos

Reauditados sem divergência material dentro do escopo declarado das telas.

## Segunda auditoria e resíduos encontrados

Após as correções iniciais, toda a funcionalidade foi auditada novamente. Essa segunda passagem encontrou três resíduos materiais, todos corrigidos antes do encerramento:

- **CNH categorias D e E:** a tela não exibia explicitamente o limite de no máximo uma infração gravíssima nos últimos 12 meses. A condição foi adicionada às duas categorias conforme o art. 53 da Resolução CONTRAN nº 1.020/2025.
- **Dicionário — agente da autoridade de trânsito:** a primeira redação corrigida ainda usava a expressão genérica “demais agentes legalmente designados”, mais ampla que a definição legal. O texto foi restringido a policial militar e agentes do art. 25-A, quando designados mediante convênio.
- **Resolução 993 — Tabela 3:** a revisão visual célula a célula revelou que “Protetores das rodas traseiras” também possui marcação para **Reboque e Semirreboque**. A célula foi corrigida de `-` para `X`, elevando o total auditado do Anexo I para **462 células aplicáveis**.
- **Proveniência da matriz 993:** removido o campo `sourcePage` de cada requisito, pois valores 1/2/3 poderiam ser interpretados incorretamente como páginas do PDF. A rastreabilidade passa a depender exclusivamente da tabela oficial, do tipo de veículo e do `pageAudit` integral das 53 páginas; o validador rejeita a reintrodução desse campo ambíguo.

Depois dessas correções, foi repetida a inspeção das 53 páginas: páginas 1–13 para Anexo I e notas, páginas 13–51 para todas as células do índice do Anexo II e páginas 51–53 para as configurações e ilustrações do Anexo III. Não foi encontrada outra divergência visual material.

Uma terceira passagem comparou programaticamente as 37 entradas do dicionário com o texto integral local do Anexo I do CTB. Sete resumos que eram semanticamente aceitáveis foram deliberadamente tornados mais estritos para preservar todos os elementos definidores relevantes: Bicicletário, Ciclofaixa, Faixa de trânsito, Parada, Veículo articulado, Veículo especial e Via. Com isso, o resultado final é **18 termos ajustados e 19 mantidos**, sem cabeçalho legal sem correspondência.

## Validação automatizada

O Android CI executa `python3 tools/audit/validate_tooling_audit.py` antes dos testes e build. O script valida:
- as 53 páginas declaradas na prova;
- as 848 células do Anexo I;
- 16 tipos, 53 requisitos e 49 notas;
- consistência entre quantidade declarada e calculada de células aplicáveis;
- a prova termo a termo das 37 definições;
- as correções específicas de CNH, engate e alcoolemia.

### Resultado final do CI

Run `38479720104`, sobre o commit funcional `2ea61b946ab915dbd1a0d9b7b45eb49065f24c14`: **success**.

- CTB: 391 entradas validadas;
- catálogo normativo: 2.943 atos / 7 páginas-fonte oficiais;
- relações: 1.173 registros / 42 referências não resolvidas preservadas para revisão;
- prova das ferramentas: 53 páginas, 848 células do Anexo I e 37 termos do dicionário;
- `:app:testDebugUnitTest`: executado, porém `NO-SOURCE` — o projeto não possui testes unitários Kotlin nesse source set;
- `:app:lintDebug`: sucesso;
- `:app:assembleDebug`: sucesso;
- APK debug: 13 MB, SHA-256 `b57adc44ded07f56a12cd5c6d7c528decaaa517eb5810163dba9db330aca0f3d`.

### Segurança

A revisão do diff não encontrou arquivos ou padrões indicativos de segredo/credencial. Nenhum arquivo de dependência ou `AndroidManifest.xml` foi alterado, não houve nova permissão, código de rede, autenticação, criptografia ou armazenamento sensível. A nova matriz da Resolução 993 é um asset local empacotado; a URL oficial é usada somente para abertura pelo manipulador de URI já existente.
