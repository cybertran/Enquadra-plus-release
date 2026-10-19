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
50:D7:B4:DD:CA:B4:B9:37:62:C1:1A:3E:BB:1C:EF:44:4B:45:F3:EE:8F:F4:79:B7:3F:07:F2:51:15:C9:E1:18
```

Esse fingerprint é público e serve para verificar a identidade de uma build. Ele não permite reconstruir a chave privada.

## Princípios

O projeto segue fail-closed quando aplicável, mínimo privilégio, HTTPS obrigatório para fontes remotas, validação de entradas e arquivos, isolamento da chave de assinatura, revisão de dependências e validação de código gerado por IA.

Vulnerabilidades críticas ou altas conhecidas e sem mitigação bloqueiam releases.
