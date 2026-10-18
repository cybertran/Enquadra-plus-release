# Fontes oficiais

A regra do projeto é simples: a base normativa do Enquadra+ deve ser construída a partir de fontes primárias oficiais.

## CTB

Documento-base: Lei nº 9.503, de 23 de setembro de 1997 — Código de Trânsito Brasileiro.

Texto compilado oficial:
https://www.planalto.gov.br/ccivil_03/leis/l9503compilado.htm

O dataset local do CTB é gerado durante o desenvolvimento a partir desse texto compilado, incluindo os artigos e o Anexo I de conceitos e definições, com hash SHA-256 da resposta oficial, data de verificação, quantidade de registros e validação de duplicidades. O APK não depende de conexão para pesquisar o conteúdo após a importação do asset.

## Resoluções e Deliberações CONTRAN e Portarias SENATRAN

Índice oficial de Resoluções CONTRAN:
https://www.gov.br/transportes/pt-br/assuntos/transito/conteudo-Senatran/resolucoes-contran

Índice oficial de Deliberações CONTRAN:
https://www.gov.br/transportes/pt-br/assuntos/transito/conteudo-contran/deliberacoes-contran

Índice oficial de Portarias SENATRAN:
https://www.gov.br/transportes/pt-br/assuntos/transito/conteudo-Senatran/portarias-senatran

O catálogo local usa os índices correntes de Resoluções e Deliberações CONTRAN e os índices anuais de Portarias SENATRAN de 2021 a 2026. O pipeline registra o SHA-256 de cada página consultada, a data de verificação e o hash canônico do conjunto estruturado. A situação ou observação exibida no aplicativo é a publicada pelo próprio órgão; o Enquadra+ não infere vigência quando a fonte não a declara.

## Manuais Brasileiros de Sinalização

Página oficial da SENATRAN:
https://www.gov.br/transportes/pt-br/assuntos/transito/senatran/manuais-brasileiros-de-sinalizacao-de-transito

A coleção vigente reúne nove volumes: regulamentação, advertência, indicação, sinalização horizontal, semafórica, dispositivos auxiliares, temporária, cicloviária e cruzamentos rodoferroviários. O módulo de sinalização usa os PDFs oficiais dessa coleção. Para os Volumes II e III, a geração v0.18 foi alinhada aos arquivos publicados na área atual de manuais da SENATRAN; o Volume III utilizado é a 2ª edição de 2022, em substituição ao arquivo legado de 2014 que permanecia no diretório histórico de publicações.

No Volume I, o catálogo pesquisável registra os 57 sinais de regulamentação pelo código e nome publicados no próprio manual. O Enquadra+ não redesenha a placa: ao abrir um sinal, renderiza a página correspondente do PDF oficial.

O visualizador interno aceita apenas HTTPS, domínio gov.br allowlisted, caminho oficial de trânsito e arquivo PDF direto. Redirecionamentos são revalidados, o download possui limite de tamanho e o arquivo precisa começar pelo cabeçalho `%PDF-` antes de ser entregue ao PdfRenderer.

## Texto integral normativo e vínculos por infração

A v0.18 gera um pacote offline derivado exclusivamente dos documentos apontados pelo catálogo oficial de Resoluções, Deliberações e Portarias. O pipeline aceita somente URLs HTTPS em `gov.br` / `www.gov.br` sob a área oficial de trânsito, revalida cada redirecionamento antes de segui-lo, impõe limite de tamanho e registra SHA-256 do documento efetivamente extraído. A validação da v0.18 exige cobertura de texto de 100% dos documentos PDF do catálogo; formatos legados não-PDF permanecem contabilizados separadamente no relatório de extração.

O texto extraído é dividido em chunks comprimidos e importado para um FTS próprio do Room. O pacote é derivado do `normative-catalog`: quando o catálogo é promovido para outra versão, o texto integral antigo é invalidado automaticamente até existir geração compatível.

A auditoria de regulamentações percorre todas as fichas MBFT instaladas. São aceitos somente dois tipos de vínculo:
- referência normativa explícita localizada no MBFT ou em sua fonte;
- citação direta do artigo correspondente do CTB no texto integral de um ato oficial, com trecho de evidência armazenado.

Não há vínculo por similaridade temática, palavra-chave jurídica genérica ou inferência editorial. Todas as fichas MBFT são auditadas; uma ficha sem evidência oficial adicional permanece explicitamente sem vínculo adicional, em vez de receber associação forçada por quota.

## Índice completo de sinalização

Os nove PDFs oficiais dos Manuais Brasileiros de Sinalização são processados por pipeline próprio. O catálogo preserva os 57 sinais de regulamentação já auditados do Volume I e, nos demais volumes, registra entradas dos marcadores internos e códigos encontrados no texto oficial, sempre com volume, página, URL e hash da fonte.

## MBFT

Documento-base: Manual Brasileiro de Fiscalização de Trânsito, aprovado pela Resolução CONTRAN nº 985, de 15 de dezembro de 2022.

Página oficial de resoluções do CONTRAN:
https://www.gov.br/transportes/pt-br/assuntos/transito/conteudo-Senatran/resolucoes-contran

A página oficial consultada em 09/10/2026 informa que a Resolução 985/2022 permanece em vigor com alterações posteriores. Por isso cada registro do aplicativo guarda uma data de verificação e a origem utilizada.

## Ficha de validação inicial

Código de enquadramento: **682-32**

Amparo legal: **art. 231, IV, do CTB**

Fonte: ficha oficial do MBFT para o código 682-32.

Dados usados na primeira versão:

- gravidade: grave;
- penalidade: multa;
- medida administrativa: retenção do veículo para regularização;
- infrator: condutor;
- competência: órgão ou entidade de trânsito municipal e rodoviário;
- pontuação: 5;
- constatação: mediante abordagem;
- regras oficiais de quando autuar e quando não autuar;
- definições/procedimentos presentes na ficha;
- exemplo oficial do campo de observações do AIT.

## Relações entre enquadramentos

As relações operacionais são derivadas das fichas locais do MBFT já conferidas contra as fontes oficiais. Somente fichas `VERIFIED` podem originar automaticamente regras de alternativa, concomitância ou pré-requisito. O dataset derivado registra a versão do MBFT que lhe deu origem e falha na validação caso os dois pacotes não coincidam.

Relações gerais curadas pelo Enquadra+ permanecem identificadas como conteúdo derivado do projeto e não são apresentadas como redação oficial.

## Controle de atualização

Antes de importar ou revisar um lote de fichas, deve-se conferir se a Resolução 985/2022 ou o trecho correspondente do MBFT foi alterado por resolução posterior. O número da fonte, a seção e a data de verificação devem acompanhar o dado importado.

Para atualizações executadas pelo próprio aplicativo, somente fontes primárias oficiais allowlisted podem alimentar uma geração candidata. Ausência de um ato em um índice não será interpretada automaticamente como revogação, e redirecionamento para origem não aprovada deve bloquear o processo.

A política canônica de consulta, proveniência, validação, promoção e rollback está em [SAFE_NORMATIVE_UPDATE_DOSSIER.md](SAFE_NORMATIVE_UPDATE_DOSSIER.md).
