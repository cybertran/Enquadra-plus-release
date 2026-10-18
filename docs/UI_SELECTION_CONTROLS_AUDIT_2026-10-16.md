# Auditoria visual — seletores, filtros e opções

**Data:** 16/10/2026  
**Escopo:** controles de seleção, filtros, metadados apresentados como chips e listas de opções do Enquadra+.

## Diagnóstico

A interface usa muitos controles com contorno persistente e formato retangular semelhante entre si. Quando esse padrão se repete em grande quantidade, principalmente em `FlowRow`, a tela ganha aparência de formulário ou grade de caixas e perde hierarquia visual.

O problema não é apenas o raio dos cantos. A mesma linguagem visual está sendo usada para quatro coisas diferentes:

- seleção;
- filtro;
- informação estática;
- ação.

Esses quatro papéis devem ter aparências diferentes.

## Diretriz visual canônica

- **Seleções pequenas e exclusivas:** usar seletor tonal/segmentado ou pills arredondadas, com estado selecionado preenchido e opções não selecionadas leves, sem borda forte permanente.
- **Filtros múltiplos curtos:** usar chips compactos de seleção tonal, preferencialmente em uma linha rolável quando houver muitas opções. O estado selecionado deve ser indicado por preenchimento e, quando útil, check discreto.
- **Listas longas de escolha única:** evitar dezenas de chips. Preferir seletor pesquisável, menu/bottom sheet ou lista de seleção com indicador de rádio/check.
- **Checklists textuais:** usar linhas de lista com indicador de seleção, fundo tonal apenas quando selecionadas e hierarquia tipográfica; não transformar cada item simples em um grande cartão.
- **Metadados estáticos:** usar badges/tags não clicáveis, sem aparência de botão.
- **Ações reais:** reservar Button, TextButton, IconButton e componentes equivalentes para ações. Não usar chip apenas para dar aparência de ação secundária.
- **Forma:** preferir cantos mais arredondados, fundos tonais suaves, ausência de contorno pesado e maior uso de espaço em branco.
- **Consistência:** o mesmo papel deve usar o mesmo componente em todo o app.
- **Acessibilidade:** preservar alvo de toque adequado, contraste, estados selecionado/desabilitado e leitura clara pelo TalkBack.

## Avaliação por tela

### Normas

**Situação atual:** filtros `Todas`, `CTB`, `MBFT`, `Resoluções`, `Deliberações`, `Senatran` e `Sinalização` usam chips contornados em `FlowRow`.

**Diretriz:** substituir por pills tonais compactas, preferencialmente em faixa horizontal rolável. O selecionado recebe preenchimento tonal; os demais ficam sem borda forte. Evitar aparência de grade de botões.

### Equipamentos obrigatórios

**Situação atual:** todos os tipos de veículo aparecem simultaneamente como chips contornados, inclusive rótulos muito longos.

**Diretriz:** esta é a maior prioridade. Como há muitas opções e alguns nomes são extensos, usar seletor de veículo pesquisável ou bottom sheet/lista de escolha única. A seleção atual pode aparecer em um campo compacto no topo. Não manter uma parede de chips.

Os requisitos aplicáveis devem continuar como checklist, mas preferencialmente em linhas selecionáveis com indicador de check, usando fundo tonal apenas no item marcado, em vez de grandes cartões equivalentes para todos os itens.

### Infrações — modos e filtros

**Situação atual:** `Todas`, `Favoritas`, `Recentes` e `Filtros` usam chips semelhantes. Natureza, pontos e “Pode configurar crime” repetem o mesmo padrão.

**Diretriz:** `Todas/Favoritas/Recentes` são seleção única e devem usar controle segmentado leve ou pills tonais. `Filtros` deve ser ação própria, com ícone ou botão tonal discreto. Natureza e pontos permanecem filtros múltiplos, mas com chips tonais compactos, sem contorno pesado.

### Resultados de infrações

**Situação atual:** código, artigo, favorita e status podem aparecer como `AssistChip` clicável mesmo quando funcionam apenas como informação.

**Diretriz:** transformar código, artigo e status em badges estáticos. O card inteiro continua sendo o alvo de abertura. Favorito pode ser representado por ícone/estado visual, não por botão extra dentro do card.

### Ficha de infração

**Situação atual:** código, natureza e pontos são `AssistChip`; favorito, anotação e fontes também usam linguagem semelhante.

**Diretriz:** código, natureza e pontos viram tags estáticas. Favoritar deve ser IconButton ou ação textual discreta. “Minha anotação” deve parecer expansão/ação secundária, não filtro. “Ver fonte” deve ser ação textual ou botão tonal pequeno.

### Engate

**Situação atual:** as duas opções de PBT usam chips contornados; opções booleanas e checklist usam cartões completos.

**Diretriz:** PBT é escolha binária e deve usar seletor segmentado/tonal de duas opções. Estados booleanos como “Engate original de fábrica” e “Veículo já circulava...” devem usar linha com switch/check ou controle equivalente. O checklist deve seguir a mesma linguagem de lista selecionável usada no restante do app.

### Licenciamento e prazos

**Situação atual:** `Nacional` e UFs auditadas usam chips; final de placa de 0 a 9 também usa chips.

**Diretriz:** enquanto houver poucas UFs auditadas, usar pills tonais em rolagem horizontal. Se a cobertura crescer para muitas UFs, migrar para seletor pesquisável. Para final da placa, usar seletor numérico compacto — círculos/pills pequenas ou grid numérico leve — sem grandes retângulos contornados.

### Sinalização

**Situação atual:** `Todos` e volumes usam chips contornados.

**Diretriz:** usar pills tonais compactas em faixa horizontal rolável. O volume selecionado deve ser identificado pelo preenchimento, não por borda dominante.

### Fiscalização de alcoolemia

**Situação atual:** sinais psicomotores são apresentados como cartões clicáveis de largura total.

**Diretriz:** como é checklist, usar linhas de seleção com check visível e fundo tonal apenas quando selecionadas. Manter agrupamento por categoria, evitando uma sequência de cartões pesados.

### CNH e habilitação

**Situação atual:** cenários são cartões de largura total clicáveis.

**Diretriz:** usar lista de escolha única com indicador visual de seleção (rádio/check) e fundo tonal suave. O cartão grande deve ficar reservado ao resultado, não a cada opção.

## Componentes que podem permanecer como cards

Cards continuam adequados para:

- resultados ou resumos;
- blocos explicativos;
- informações jurídicas com mais de uma linha;
- resultado de ferramenta;
- navegação de alto nível em Home/Ferramentas.

A diretriz não é “remover cards”, e sim evitar que **toda escolha simples pareça um card ou botão contornado**.

## Estado de implementação — 16/10/2026

A diretriz desta auditoria foi implementada no código do aplicativo com componentes reutilizáveis de seleção e estado.

Aplicações concluídas:

- Normas: filtros migrados para pills tonais horizontais.
- Equipamentos obrigatórios: tipos de veículo migrados para seletor pesquisável em bottom sheet; requisitos migrados para linhas de checklist.
- Infrações: modos de lista migrados para pills; filtros múltiplos para pills tonais; código, artigo e status passam a badges estáticos.
- Ficha de infração: metadados passam a tags estáticas; favorito mantém ação própria; anotação e fontes deixam de parecer filtros.
- Engate: escolha de PBT passa a seletor tonal; estados booleanos e checklist usam linhas de seleção.
- Licenciamento: UF e final da placa passam a seletores horizontais compactos.
- Sinalização: volumes passam a pills horizontais.
- Fiscalização de alcoolemia: sinais psicomotores passam a linhas de checklist.
- CNH/habilitação: cenários passam a lista de escolha única com estado visual claro.

Os componentes compartilhados ficam em `SelectionControls.kt`, evitando que cada tela volte a criar padrões visuais divergentes.

Validação de código: Android CI completo verde no run `38938786320`.

## Fora do escopo desta anotação

Esta auditoria não altera agora:

- campos de busca e entrada de texto;
- cores da marca;
- bottom navigation;
- cards de conteúdo;
- tipografia geral;
- regras funcionais.

Uma revisão futura pode avaliar a migração de alguns `OutlinedTextField` usados como busca para componentes de SearchBar do Material 3, mas isso não é requisito desta decisão.
