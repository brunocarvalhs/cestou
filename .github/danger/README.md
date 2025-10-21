Estrutura de steps do Danger

Colocamos checagens menores em módulos separados para facilitar manutenção.

Formato:

- Cada arquivo em `steps/` exporta uma função assíncrona que recebe o objeto `danger`.
- Os arquivos são carregados pelo `dangerfile.js` principal em ordem alfabética (prefixe com números para controlar ordem).

Como adicionar um step:

1. Criar `.github/danger/steps/NN-descricao-do-step.js`
2. Exportar `module.exports = async function(danger) { /* ... */ }`
3. Comitar. O `dangerfile.js` principal vai importar e executar automaticamente.

Exemplo de responsabilidades por arquivo:

- `01-pr-metadata.js` - Valida título e descrição do PR
- `02-file-checks.js` - Verifica arquivos modificados, testes, compose, etc
- `03-libs-checks.js` - Verifica libs bloqueadas e depreciadas

Onde colocar listas de libs:

- Arquivos com listas de bibliotecas ficam em `.github/danger/libs/` (por exemplo `blockedLibs.txt` e `deprecatedLibs.txt`).

Sugestão de novo step de qualidade:

- `04-manifest-permissions.js` - Verifica mudanças em permissões no `AndroidManifest.xml` e alerta sobre permissões sensíveis (por exemplo: CAMERA, RECORD_AUDIO, LOCATION). Isso ajuda reviewers a avaliar impactos de privacidade e rationale.
