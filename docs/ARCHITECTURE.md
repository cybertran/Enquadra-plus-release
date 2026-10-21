# Arquitetura

## Princípios

O projeto adota Kotlin, Jetpack Compose, Material Design 3 e arquitetura em camadas. A interface deve permanecer simples e a camada de domínio não deve depender de detalhes de banco, rede ou apresentação.

## Estrutura inicial

```
app/
  ui/             Interface Compose e estado de tela
  domain/
    model/        Modelos de domínio
    repository/   Contratos de acesso aos dados
  data/
    local/        Room, entidades, DAO e carga inicial
    repository/   Implementação offline dos repositórios
```

A v0.1 começa em um único módulo Android para reduzir complexidade. A divisão em módulos Gradle só deve ocorrer quando houver ganho concreto de build, testes ou isolamento.

## Navegação

O aplicativo tem quatro destinos de nível superior: Início, Infrações, Normas e Ferramentas. Em telefones a navegação prioriza a barra inferior. Em telas maiores, a evolução prevista é utilizar os componentes adaptáveis do Material 3 para rail ou navegação equivalente.

Destinos secundários que não fazem parte do fluxo operacional principal usam um drawer lateral acionado pelo botão de menu no topo. O drawer concentra Configurações, Sobre, Versão e Desenvolvedor. Configurações é a entrada principal para a área de atualização das bases jurídico-normativas. Informações técnicas ou de versão não devem competir visualmente com os atalhos da Home.

Navigation 3 fica disponível para os fluxos internos mais complexos. A primeira ficha funcional usa estado de tela simples enquanto a estrutura de navegação definitiva é consolidada.

## Dados

A estratégia é local-first. A consulta continua disponível sem internet depois que a base estiver instalada ou atualizada.

A persistência usa SQLite por meio do Room. Infrações, CTB e catálogo normativo utilizam índices FTS locais separados. A pesquisa de infrações cobre código, artigo, tipificação e campos operacionais; a pesquisa do CTB cobre rótulo do artigo, capítulo/seção e texto compilado; a pesquisa normativa cobre órgão, tipo, número, ano, assunto e situação/observação oficial.

A carga inicial é transacional por dataset. MBFT, relações entre enquadramentos, CTB e catálogo normativo possuem importadores independentes e metadados de versão próprios, compartilhando a mesma base Room. O dataset de relações é importado após o MBFT e exige correspondência exata com a versão MBFT instalada.

Preferências simples, como tema, filtros e opções de visualização, poderão usar DataStore.

## Atualizações

A atualização normativa deverá ser independente do APK sempre que o formato oficial continuar compatível com parsers já instalados. O próprio aplicativo fará consulta direta às fontes oficiais aprovadas, sem depender de servidor próprio do Enquadra+.

Nenhuma fonte remota pode alterar diretamente a base ativa. Toda atualização deve produzir uma geração candidata isolada, com versão, parser, hashes, proveniência, diff contra a base ativa e validações determinísticas. A promoção deve ser atômica, manter uma geração anterior validada para rollback e falhar fechada diante de qualquer inconsistência ou formato não reconhecido.

A especificação canônica completa está em [SAFE_NORMATIVE_UPDATE_DOSSIER.md](SAFE_NORMATIVE_UPDATE_DOSSIER.md) e é obrigatória para qualquer implementação relacionada ao bloco de atualização.

### Atualização do aplicativo

A atualização do APK é um fluxo separado da atualização jurídico-normativa. A tela **Versão** pode consultar manualmente um manifesto de release mantido no repositório público de distribuição. O manifesto só representa APKs oficiais já publicados e assinados; código presente na branch principal ou um candidato de CI não deve ser anunciado como atualização disponível.

A consulta compara o `versionCode` instalado com o `versionCode` publicado. O manifesto remoto é entrada não confiável: a implementação exige HTTPS, endpoint fixo, limite de tamanho, schema conhecido, URLs restritas ao repositório oficial, nome de APK fixo e fingerprint do certificado oficial esperado. O aplicativo não instala nem aplica APKs automaticamente. Quando houver nova versão, a ação do usuário abre o download oficial no GitHub e a instalação permanece sob confirmação do Android.

## Estado de UI

Cada tela com lógica própria terá ViewModel responsável pelo estado observável. A UI Compose apenas renderiza o estado e envia ações do usuário.

## Fontes

Nenhum registro normativo deve existir sem referência de origem. O modelo de dados registra órgão, documento, seção, URL oficial, vigência e data da última verificação.
