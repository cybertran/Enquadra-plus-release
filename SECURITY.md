# Política de Segurança

## Versões suportadas

Enquanto o repositório público estiver em bootstrap, somente a branch principal e as releases explicitamente marcadas como oficiais são consideradas suportadas.

## Reportando uma vulnerabilidade

Prefira o recurso de **Private vulnerability reporting** do GitHub quando ele estiver habilitado neste repositório.

Não publique em issue pública detalhes exploráveis, chaves, tokens, dados privados, provas de conceito perigosas ou informações que permitam comprometer usuários.

Se o canal privado ainda não estiver disponível, entre em contato com o mantenedor pelo perfil GitHub `@cybertran` sem divulgar o detalhe sensível publicamente.

## Material de assinatura

A chave privada oficial de assinatura do Enquadra+ e sua senha nunca são armazenadas neste repositório.

Fingerprint SHA-256 do certificado oficial:

```text
BE:58:EA:ED:F7:AD:76:B6:5F:75:FC:08:6A:DC:71:89:E4:91:A7:25:67:B5:8E:E7:BB:E3:EA:DA:3A:2C:93:A9
```

Esse fingerprint é público e serve para verificar a identidade de uma build. Ele não permite reconstruir a chave privada.

## Princípios

O projeto segue fail-closed quando aplicável, mínimo privilégio, HTTPS obrigatório para fontes remotas, validação de entradas e arquivos, isolamento da chave de assinatura, revisão de dependências e validação de código gerado por IA.

Vulnerabilidades críticas ou altas conhecidas e sem mitigação bloqueiam releases.
