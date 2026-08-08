const utils = require('../utils');

module.exports = async function stepHiltDiCheck(danger) {
  const { git, warn } = danger;
  const createdKotlinFiles = git.created_files.filter(f => f.endsWith('.kt'));

  for (const file of createdKotlinFiles) {
    const content = await utils.getFileContent(danger, file);

    if (content.includes('@Inject constructor')) {
      const hasHiltAnnotation = content.includes('@HiltViewModel') ||
                                content.includes('@AndroidEntryPoint') ||
                                content.includes('@Module') ||
                                content.includes('@Singleton'); // Or other scopes

      if (!hasHiltAnnotation) {
        warn(`Hilt DI: O arquivo \`${file}\` usa \`@Inject\`, mas nenhuma anotação do Hilt (como \`@AndroidEntryPoint\` ou \`@HiltViewModel\`) foi detectada.`);
      }
    }
  }
};
