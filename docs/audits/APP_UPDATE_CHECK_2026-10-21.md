# Verificação de atualização do aplicativo — revisão de segurança (2026-10-21)

## Escopo

A v0.2.0-alpha.10 adiciona uma verificação manual da versão oficial publicada do Enquadra+. Este fluxo é independente do atualizador jurídico-normativo e não altera CTB, MBFT ou catálogo CONTRAN/SENATRAN.

## Fluxo

1. O usuário abre **Versão** e toca em **Verificar atualização**.
2. O aplicativo consulta um endpoint HTTPS fixo no repositório público de distribuição.
3. O manifesto é tratado como entrada externa não confiável e validado antes de qualquer ação.
4. O `versionCode` publicado é comparado ao `versionCode` instalado.
5. Quando há versão mais nova, o aplicativo apresenta a versão e um botão **Baixar atualização**.
6. O botão abre o APK oficial no GitHub. Não existe download silencioso, instalação automática ou permissão para instalar pacotes solicitada pelo Enquadra+.

## Controles

- HTTPS obrigatório; cleartext permanece desabilitado no Android.
- Endpoint do manifesto é fixo em `raw.githubusercontent.com/cybertran/Enquadra-plus-release`.
- Redirecionamento HTTP não é seguido pela consulta do manifesto.
- Timeouts de conexão e leitura são explícitos.
- Manifesto limitado a 32 KiB.
- Schema conhecido e `versionCode` positivo.
- `versionName` limitado e validado.
- URLs de release e APK devem usar `https://github.com`, sem userinfo, porta, query ou fragmento.
- Paths devem corresponder exatamente ao repositório `cybertran/Enquadra-plus-release`, à tag da versão e ao arquivo `Enquadra.apk`.
- SHA-256 do APK deve possuir formato válido.
- Fingerprint do certificado no manifesto deve corresponder ao certificado oficial documentado.
- O manifesto não concede execução de código e não controla hosts arbitrários.
- Nenhum segredo, token ou credencial é necessário ou incorporado ao APK.
- A instalação continua protegida pelo fluxo normal do Android e pela identidade de assinatura do pacote.

## Limites e risco residual

O aplicativo não baixa o APK internamente e, portanto, não calcula localmente o SHA-256 do arquivo baixado pelo navegador. O hash do manifesto é metadado de transparência e publicação; a identidade criptográfica efetiva de uma atualização instalada continua sendo imposta pelo Android ao atualizar o mesmo pacote assinado.

Um comprometimento da conta/repositório público poderia causar informação falsa de disponibilidade, mas as URLs aceitas continuam restritas ao repositório oficial e uma build com assinatura incompatível não substitui legitimamente o pacote instalado. A chave privada oficial permanece fora do GitHub.

## Validação automatizada

Foram adicionados testes unitários para o codec do manifesto cobrindo manifesto oficial válido e rejeição de HTTP, repositório divergente, certificado divergente e inconsistência entre versão e URL. Build, lint, testes completos, auditoria de dependências e Security Policy Gate permanecem como gates do CI antes do merge.

## Evolução v0.2.0-alpha.15 — mitigação de cache compartilhado

Após a publicação da alpha.14, o cliente ocasionalmente consultou uma cópia anterior do manifesto no cache HTTP e declarou incorretamente que não existia atualização. O cliente já usava `useCaches=false`, que não impede resposta antiga de CDN/proxy remoto.

A verificação da alpha.15 usa um parâmetro de consulta `check=<timestamp>` diferente em cada tentativa no endpoint HTTPS oficial fixo, com cabeçalhos `Cache-Control: no-cache, no-store, max-age=0` e `Pragma: no-cache`. O hostname e o caminho continuam pinados; redirecionamentos permanecem proibidos. O manifesto recebido continua sujeito aos mesmos limites de tamanho, schema, versão, URLs e identidade de certificado.

Uma versão publicada maior que a instalada oferece atualização; igual confirma que não há versão nova identificada; menor é **inconclusiva** e não autoriza a mensagem de versão atualizada. A interface informa a versão oficial efetivamente consultada para facilitar o diagnóstico.

Testes unitários cobrem consulta única, endpoint permitido, nonce, comparação de versões e estado inconclusivo. Essa mitigação reduz muito a chance de falso negativo causado por cache compartilhado, mas não garante ausência de atraso na propagação da informação após uma publicação. A assinatura e o fluxo de promoção do manifesto permanecem inalterados.
