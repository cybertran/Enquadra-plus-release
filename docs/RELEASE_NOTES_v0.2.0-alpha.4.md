# Enquadra+ v0.2.0-alpha.4

Quarta prévia da v0.2, consolidando a base funcional do Enquadra+ até a etapa v0.19 e a baseline de segurança do projeto.

## Destaques

- Pesquisa operacional de infrações com código, artigo, descrição, palavra-chave e relevância.
- Fichas MBFT locais e versionadas, com relações e combinações auditadas entre enquadramentos.
- CTB local pesquisável e catálogo normativo oficial com Resoluções, Deliberações CONTRAN e atos SENATRAN.
- Pesquisa offline no texto integral das normas quando o conteúdo oficial está disponível.
- Catálogo pesquisável dos 9 volumes oficiais dos manuais brasileiros de sinalização.
- Valores de multas, filtros operacionais, favoritos, histórico e anotações pessoais.
- Ferramentas para alcoolemia, sinais psicomotores, CNH, equipamentos obrigatórios, engate, crimes de trânsito, licenciamento e prazos.
- Assistente de observações de AIT, guias operacionais e calendários estaduais auditados.
- Abertura, salvamento e compartilhamento controlado de PDFs oficiais.
- Atualizador normativo local com validação, proveniência, fail-closed, promoção atômica e rollback.
- Baseline de segurança v1.1 integrada, com Security Policy Gate e revisão de segredos/políticas.
- Pipeline Android validando datasets, guardrails, testes unitários, lint, build e integridade do APK.

## Fontes e limites

O Enquadra+ continua baseado em fontes públicas e oficiais. Conteúdo autoral de terceiros, incluindo análises jurídicas, comentários proprietários e análises de jurisprudência de outros aplicativos, não é incorporado.

Alguns atos normativos legados permanecem apenas com metadados quando o formato oficial não permitiu extração confiável do texto integral. A interface deve continuar tratando incerteza de forma explícita e falhar fechada quando a fonte necessária não puder ser validada.

## APK

O APK anexado a esta Release é um **debug APK**, destinado a testes da pré-release. Ele é compilado diretamente a partir do commit-alvo desta Release e validado pelo pipeline controlado antes do upload.
