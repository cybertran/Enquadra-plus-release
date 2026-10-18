# Implementação do atualizador normativo seguro — 13/10/2026

## Status

Este documento registra a implementação inicial do bloco 6 conforme o dossiê canônico [SAFE_NORMATIVE_UPDATE_DOSSIER.md](../SAFE_NORMATIVE_UPDATE_DOSSIER.md).

O bloco 6 está **implementado e validado pelo CI**. A infraestrutura comum cobre CTB, catálogo CONTRAN/SENATRAN e MBFT, preservando o comportamento fail-closed definido no dossiê canônico.

No MBFT, a implementação não tenta interpretar genericamente qualquer PDF futuro. O app consulta o índice oficial, identifica atos que alteram a Resolução CONTRAN nº 985/2022 e somente pode promover uma alteração quando existe uma transformação determinística previamente auditada, ligada à identidade e ao SHA-256 exato da fonte oficial. Emendas desconhecidas ou conteúdo cujo hash mudou são bloqueados e a última geração validada permanece ativa.

## Arquitetura implementada

O aplicativo passou a consultar diretamente fontes oficiais aprovadas, sem servidor próprio do Enquadra+.

As gerações ficam no armazenamento privado do aplicativo. O conteúdo ativo não é modificado durante download, parsing ou validação. Uma mudança remota gera uma candidata separada, acompanhada por manifesto imutável de proveniência com parser, hashes, URL solicitada/final, tipo e tamanho do conteúdo, base de comparação, validações executadas e diff.

A base Room só é alterada depois da aprovação da candidata. A promoção usa uma transação única e o snapshot anterior validado permanece disponível para rollback offline.

Os assets embarcados de CTB, catálogo normativo, MBFT e relações passaram a ser apenas bootstrap: se já existir uma versão instalada, a inicialização do APK não a substitui por uma cópia embarcada.

## Fontes e rede

O CTB aceita somente HTTPS no domínio oficial do Planalto e no caminho exato do texto compilado.

O catálogo normativo aceita somente HTTPS em `gov.br/transportes/pt-br/assuntos/transito/`. Redirecionamentos são manuais, limitados e precisam continuar dentro da allowlist. HTTP claro foi desabilitado no manifesto Android.

Downloads possuem timeout, limite de tamanho, tipo MIME esperado e SHA-256 da resposta bruta. A decodificação textual rejeita sequências inválidas em vez de inserir caracteres de substituição silenciosamente.

## Validação e comparação

O CTB exige estrutura mínima, IDs únicos, artigos sentinela, Anexo I, hashes e versão coerentes. A transição bloqueia remoções não explicadas, redução da base e mudanças em massa acima dos limites conservadores.

O catálogo CONTRAN/SENATRAN exige fontes oficiais, atos sentinela, IDs únicos, hashes, anos válidos e contagens mínimas. A transição bloqueia desaparecimento de atos já conhecidos, perda de páginas-fonte e mudanças em massa.

Os limites são guardrails de anomalia, não substitutos de prova. Qualquer falha de parser, origem, integridade ou transição mantém a base ativa intacta.

Além disso, cada versão de parser precisa ser **calibrada contra uma geração conhecida e boa** antes de poder aprovar alterações. A calibração só é registrada quando o parser reconstrói a base ativa sem diferença material. Se a primeira execução de uma versão nova do parser já encontrar mudanças, a candidata é bloqueada; o sistema não pode usar uma mudança desconhecida como seu próprio teste de correção.

## Operação no aplicativo

A área **Normas → Atualizações jurídicas** permite verificar as fontes, consultar o estado de cada dataset, aplicar uma candidata aprovada e restaurar a geração anterior.

O WorkManager executa verificação periódica quando existe rede e a bateria não está baixa. Nesta fase, trabalho em segundo plano pode preparar uma candidata, mas não a promove automaticamente. A ativação permanece explícita para maximizar segurança durante a consolidação do mecanismo.

## MBFT

O MBFT passou a integrar o atualizador seguro por meio de um **parser/transformador determinístico com registro fechado de emendas suportadas**.

A base vigente de 13/10/2026 continua sendo a geração conhecida e boa. O app comprova que sua derivação de relações reproduz exatamente o dataset auditado antes de considerar o parser calibrado.

O fluxo do MBFT é:

1. consultar o índice oficial do CONTRAN;
2. identificar atos posteriores que alterem a Resolução nº 985/2022;
3. bloquear imediatamente qualquer ato não reconhecido pelo registro instalado;
4. para uma emenda conhecida, exigir transformação auditada e data de vigência atingida;
5. baixar o PDF oficial exato da emenda;
6. conferir origem, MIME, tamanho e SHA-256 bruto;
7. aplicar apenas as operações determinísticas registradas;
8. validar a nova base e calcular o diff contra a ativa;
9. reconstruir deterministicamente as relações;
10. bloquear se houver novas referências quebradas;
11. promover MBFT e relações na mesma transação Room;
12. preservar a geração anterior para rollback offline.

A Resolução CONTRAN nº 1.031/2026 é a primeira transformação registrada. O PDF oficial auditado é `resolucao10312026.pdf`, SHA-256 `9F6D8D8C6E7B7996FCE21326A229C9B262D45178907FFF7BD9DD159CA5BA9B5A`. O Anexo III contido nesse PDF substitui as fichas relacionadas a álcool e outras substâncias. A alteração do MBFT somente entra em vigor em **17/10/2026**; antes dessa data o app identifica a emenda, mas mantém obrigatoriamente a base atual.

A transformação preparada para 17/10/2026 remove o código legado `757-90`, atualiza `516-92` e inclui `516-91`, `757-91` e `757-92`, resultando em 407 fichas. As quatro fichas do novo Anexo III estão registradas como `VERIFIED` porque seus campos foram auditados contra as páginas oficiais correspondentes.

Não há OCR, IA nem adaptação heurística nessa cadeia. Se o CONTRAN publicar nova emenda, alterar o arquivo já auditado ou mudar a estrutura de modo ainda não suportado, o resultado correto é **CANDIDATE_BLOCKED** até que uma nova transformação determinística seja auditada e entregue em versão do aplicativo.

## Testes e CI

Foram adicionados testes JVM para JSON canônico, política de origem, bloqueio de hosts/redirecionamentos indevidos, parsing básico do CTB, parsing das tabelas CONTRAN/SENATRAN, transições do MBFT, hash drift, vigência da Resolução nº 1.031/2026, reconstrução determinística das relações e bloqueio de regressões.

O CI executa `tools/audit/validate_safe_update.py`, que verifica guardrails arquiteturais essenciais: HTTPS/allowlist, tráfego claro desabilitado, snapshots/manifests, promoção transacional, assets usados apenas como bootstrap, registro fechado de emendas MBFT, hash oficial da Resolução nº 1.031/2026, conteúdo esperado das quatro fichas e reconstrução das relações.

A validação completa passou no GitHub Actions run `38494975276`: 391 entradas CTB, 2.943 atos normativos em 7 páginas-fonte, 1.173 relações da base vigente, auditoria das ferramentas aprovada e prova estática do atualizador com candidata MBFT da Resolução nº 1.031/2026 em 407 fichas e 1.184 relações derivadas. `:app:testDebugUnitTest` e `:app:lintDebug` passaram; o APK debug de 14 MB foi compilado e validado com SHA-256 `F8D694A9F42D7D02247EB6F09FD073167F8CC69536558F4F941F3704E5F4F7E8`.

## Riscos residuais deliberados

Uma alteração futura no formato HTML do Planalto, nos índices oficiais ou uma nova emenda ao MBFT que ainda não esteja no registro auditado pode ser bloqueada pelo parser instalado. Esse resultado é intencional: fonte não comprovadamente compatível exige nova transformação/parser em versão posterior do APK, sem afetar a última base validada.

O sistema não promete interpretar automaticamente qualquer documento governamental futuro. Ele promete algo mais importante para este projeto: **não promover aquilo que não consiga provar**.

A implementação mantém o Room atual como base de consulta e usa snapshots JSON imutáveis como gerações A/B. A candidata não é um segundo banco Room completo; o isolamento é obtido fora do banco, e a promoção é transacional. No caso do MBFT, a promoção inclui também as relações derivadas na mesma transação, impedindo uma base de infrações nova de coexistir com relações da versão anterior.
