# Diagnóstico das atualizações jurídicas — 11/11/2026

## Evidências observadas

Na tela **Atualizações jurídicas**, em 11/11/2026, o usuário obteve:

- **CTB:** tentativa inconclusiva com `Connection reset`; base ativa `2026-10-17-planalto-f1982bdedd22` foi preservada.
- **CONTRAN/SENATRAN:** candidata recusada com **6 atos incluídos, 3.204 alterados, 0 removidos, 0 referências quebradas**; base ativa `2026-10-17-norms-3d52f7eb192e` preservada.
- **MBFT:** nenhuma emenda aplicável pendente foi detectada na consulta suportada.

A base normativa versionada na fonte canônica contém **3.204 registros** em oito páginas-fonte oficiais (1.012 resoluções, 249 deliberações, 348 portarias de 2026, 334 de 2025, 276 de 2024, 358 de 2023, 326 de 2022 e 301 de 2021).

## CTB: causa ainda não demonstrada no dispositivo

Endpoint oficial fixo: `https://www.planalto.gov.br/ccivil_03/leis/l9503compilado.htm`.

O cliente usa `HttpsURLConnection`, TLS obrigatório, validação de host/path e redirecionamentos restritos; já existem até três tentativas, timeout de conexão de 20 s e leitura de 60 s. `Connection reset` indica que a sessão de rede foi interrompida, mas **não comprova** bloqueio, defeito TLS, queda da operadora ou erro no Android.

Uma consulta externa efetuada em 11/11/2026 recebeu do próprio site do Planalto uma página **“Acesso Negado! Esta requisição foi bloqueada”**. Esse resultado é evidência de restrições para parte dos acessos automatizados, **não** reprodução da falha do celular. A distinção deve ser verificada em rede móvel, Wi-Fi e, quando disponível, com logs mínimos sem dados pessoais.

Não trocar o domínio por mirror, HTTP ou outro host não aprovado; não ignorar TLS/SSL nem aplicar documento sem validação.

## CONTRAN/SENATRAN: suspeita de mudança de metadados

O `NormativeUpdateCodec.diff` compara `toMap()` completo de cada ato, exceto `lastCheckedAt`. Assim, variações em `officialUrl`, `status`, `numberLabel`, `publicationDate` e `subject` contam como atos alterados. Isso é prudente porque links oficiais e estados de vigência têm relevância jurídica; entretanto, sem identificar os campos não é possível declarar que os 3.204 itens sofreram mudanças jurídicas.

Em amostras de índices oficiais de 2025, 2021 e deliberações do CONTRAN consultadas nesta investigação, muitos números, assuntos e datas coincidiram com o snapshot de 17/10/2026. A extração HTML usada nesta inspeção não preservou com fidelidade todos os `href` e não permite concluir a contagem exata dos campos alterados no parser Android.

### Mudanças instrumentais desta branch

1. `NormativeUpdateCodec.changedFieldCounts` conta, por nome do campo, diferenças entre registros com o mesmo ID, ignorando `lastCheckedAt`; não altera `diff`, validação ou promoção.
2. A mensagem **técnica** de candidata normativa bloqueada indica os campos mais frequentes com o número de atos afetados. O limite de mudança em massa permanece o mesmo: `maxOf(200, 10% da base ativa)`.
3. `OfficialSourceErrorDiagnostics` distingue reset de socket, timeout, DNS e falha TLS na consulta CTB; sem retries adicionais, bypass ou outro endpoint.
4. Testes unitários verificam diagnósticos, registros idênticos, alteração só de URL, campos distintos e manutenção do bloqueio massivo.

### Critério para a próxima correção funcional

Somente após obter uma execução completa com os diagnósticos por campo e comparar amostras do **HTML original preservando os links** com o snapshot ativo poderemos identificar normalizações seguras. Não classificar `status`, `officialUrl` ou `subject` como “cosméticos” sem evidência e revisão normativa.

## Validação e limites

Esta etapa é **diagnóstica**: não gera atualização jurídica liberada nem resolve de imediato a disponibilidade do Planalto. O código continua fail-closed e preserva base/rollback. O novo diagnóstico requer uma build Android gerada **após esta alteração** e teste no aparelho; um APK candidato gerado anteriormente não o contém. Nenhum arquivo de assinatura, senha ou dado privado deve entrar em repositórios/CI.
