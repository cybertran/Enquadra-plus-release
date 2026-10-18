# Primeira importação pública

A primeira importação do código do Enquadra+ foi feita a partir do snapshot de desenvolvimento correspondente à versão v0.2.0-alpha.8, em uma branch dedicada e sem copiar o histórico Git privado.

## Sanitização aplicada

Não foram importados workflows e configurações do runner privado, documentação específica de infraestrutura AWS, diretórios de staging de pipelines, arquivos de controle de releases internos, chaves de assinatura, keystores, senhas, tokens ou credenciais.

Os workflows públicos permanecem separados e usam runners hospedados pelo GitHub com permissões mínimas. Referências do aplicativo ao repositório anterior foram ajustadas para este repositório público.

O `gradle.properties` foi desvinculado do perfil de memória do runner privado. A política de extração de dados também passou a excluir o banco local do usuário de transferências entre dispositivos, alinhando esse comportamento à exclusão já aplicada ao backup em nuvem.

## Validação

A importação deve ser aceita somente após os gates públicos de segurança, varredura de segredos, revisão de dependências, CodeQL e CI Android concluírem com sucesso.
