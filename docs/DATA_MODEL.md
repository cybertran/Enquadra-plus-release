# Modelo de dados e rastreabilidade

A base do Enquadra+ não deve ser uma cópia de banco de terceiros. Ela será produzida a partir das fontes primárias oficiais.

## Persistência local

A v0.1 usa Room sobre SQLite. A base é local-first: depois que os dados estiverem instalados no aparelho, a pesquisa não depende de conexão com a internet.

As tabelas iniciais são:

- `infractions`: ficha principal da infração;
- `infraction_notes`: quando autuar, quando não autuar, procedimentos e observações oficiais;
- `official_sources`: documentos e fontes oficiais;
- `infraction_source_cross_ref`: relação entre fichas e suas fontes;
- `infraction_relations`: relações tipadas entre códigos de enquadramento, com origem, campo-fonte e evidência quando a regra é explícita no MBFT;
- `ctb_articles`: artigos do CTB compilado, com capítulo, seção, texto e proveniência;
- `ctb_article_search`: índice FTS local para pesquisa por artigo, contexto e conteúdo;
- `normative_acts`: catálogo local de Resoluções CONTRAN e Portarias SENATRAN, com número, ano, assunto, situação/observação e proveniência;
- `normative_act_search`: índice FTS local para pesquisa por órgão, tipo, número, ano, assunto e situação.

## Registro de infração

Cada infração deverá conter, quando aplicável:

- código de enquadramento;
- artigo e dispositivo legal;
- descrição resumida;
- tipificação do enquadramento;
- classificação/natureza;
- pontuação;
- responsabilidade;
- competência;
- forma de constatação;
- penalidade;
- medida administrativa;
- indicação oficial sobre possível crime de trânsito;
- quando autuar;
- quando não autuar;
- definições e procedimentos;
- observações oficiais do AIT;
- enquadramentos relacionados;
- referências das fontes utilizadas.

## Proveniência

Cada informação importante deve poder apontar para uma ou mais fontes. Uma referência de fonte contém órgão, documento, seção ou ficha, endereço oficial quando disponível, vigência e data em que a informação foi verificada.

## Primeiro registro de validação

A base inicial contém a ficha oficial de código **682-32**, prevista no art. 231, IV do CTB e documentada no MBFT. Ela foi escolhida para validar a estrutura porque reúne campos de enquadramento, gravidade, pontuação, competência, constatação, medida administrativa, regras de autuação e observação do AIT.

## CTB local

O CTB é empacotado como dataset versionado gerado a partir do texto compilado oficial da Lei nº 9.503/1997. As unidades de consulta são os artigos e o Anexo I de conceitos e definições. O asset registra versão, datas de geração e verificação, URL oficial, hash SHA-256 da fonte e quantidade de registros. O aplicativo importa o conteúdo para Room e constrói um índice FTS local, preservando capítulo, seção e subseção quando disponíveis.

## Catálogo normativo CONTRAN/SENATRAN

O catálogo normativo é gerado a partir dos índices oficiais do Ministério dos Transportes. São incluídas as Resoluções CONTRAN publicadas no índice oficial e todas as Portarias SENATRAN dos índices anuais de 2021 a 2026, sem exclusão automática por heurística de relevância. Cada registro preserva órgão, tipo, número, ano, datas informadas, assunto, situação ou observação oficial, URL do documento quando disponível, página-fonte e data de verificação.

O asset é versionado pelo hash SHA-256 canônico dos registros estruturados e registra separadamente o SHA-256 bruto de cada página-fonte. O texto integral de PDF não é copiado para este bloco; a consulta local cobre os metadados normativos oficiais e abre o documento oficial quando disponível.

## Relações e combinações entre enquadramentos

As relações operacionais são mantidas em dataset próprio, vinculado à versão exata do MBFT. O pipeline deriva automaticamente apenas regras de alta confiança presentes em fichas `VERIFIED`:

- `ALTERNATIVE`: o campo oficial “Quando não autuar” remete explicitamente a outro código;
- `CONCOMITANT`: o procedimento oficial determina autuação concomitante, lavratura de outro AIT ou equivalente;
- `PREREQUISITE`: o procedimento oficial determina que a autuação seja precedida por outra;
- `RELATED`: relação curada pelo projeto a partir das fontes oficiais quando não há uma das três regras explícitas acima.

Cada regra oficial conserva o campo de origem e o trecho do MBFT que menciona explicitamente o código relacionado. Relações curadas nunca são apresentadas como texto oficial. Se uma relação curada também puder ser derivada de regra oficial explícita, a versão curada é descartada para evitar duplicidade. Referências explícitas a códigos ausentes do catálogo são relatadas para revisão e não são mapeadas automaticamente.

## Conteúdo derivado

Campos calculados, relações entre infrações, índices de busca e resultados de ferramentas podem ser derivados dos dados oficiais. Eles devem ser gerados por regras próprias do Enquadra+ e nunca pela reprodução de lógica proprietária de terceiros.

## Atualização futura

A atualização das bases locais é governada pelo dossiê canônico [SAFE_NORMATIVE_UPDATE_DOSSIER.md](SAFE_NORMATIVE_UPDATE_DOSSIER.md).

O próprio aplicativo poderá consultar diretamente fontes oficiais e gerar novas versões sem exigir novo APK quando o formato continuar compatível com os parsers instalados. A base ativa nunca será sobrescrita durante parsing ou validação: uma geração candidata isolada deve ser construída, comparada com a ativa, validada integralmente e promovida de forma atômica, preservando uma geração anterior validada para rollback.
