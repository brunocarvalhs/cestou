const utils = require('../utils');
const path = require('path');

module.exports = async function stepResourceLocalization(danger) {
  const { git, message } = danger;
  const modifiedFiles = git.modified_files || [];

  const modifiedStrings = modifiedFiles.filter(f => f.endsWith('strings.xml'));

  for (const file of modifiedStrings) {
    if (file.includes('/res/values/')) { // Base strings.xml
      const modulePath = file.split('/res/values/')[0];
      // Search for other strings.xml in the same module
      const otherLanguages = modifiedFiles.filter(f =>
        f.startsWith(modulePath) &&
        f.endsWith('strings.xml') &&
        !f.includes('/res/values/')
      );

      if (otherLanguages.length === 0) {
        message(`Localização: O arquivo \`strings.xml\` base foi alterado em \`${utils.getModuleName(file) || file}\`. Não esqueça de solicitar as traduções para os outros idiomas suportados.`);
      }
    }
  }
};
