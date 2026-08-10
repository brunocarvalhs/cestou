const config = require('../config');
const utils = require('../utils');
const path = require('path');

module.exports = async function stepNamingConventions(danger) {
  const { git, warn, fail } = danger;
  const createdFiles = git.created_files || [];

  for (const file of createdFiles) {
    const fileName = path.basename(file);
    const moduleName = utils.getModuleName(file);

    // 1. ViewModel suffix check
    if (file.endsWith('.kt')) {
        const content = await utils.getFileContent(danger, file);
        if (content.includes(': ViewModel()') || content.includes(': BaseViewModel()')) {
            if (!fileName.endsWith(`${config.naming.suffixes.ViewModel}.kt`)) {
                warn(`Convenção de Nome: O arquivo \`${fileName}\` parece ser um ViewModel mas não possui o sufixo \`ViewModel\`.`);
            }
        }
    }

    // 2. Resource naming check
    if (file.includes('/res/layout/') || file.includes('/res/drawable/') || file.includes('/res/values/')) {
        if (moduleName && config.naming.resourcePrefixes[moduleName]) {
            const expectedPrefix = config.naming.resourcePrefixes[moduleName];
            // Ignore some standard files
            if (fileName !== 'strings.xml' && fileName !== 'colors.xml' && fileName !== 'themes.xml' && fileName !== 'styles.xml') {
                if (!fileName.startsWith(expectedPrefix)) {
                    warn(`Convenção de Nome: O recurso \`${fileName}\` no módulo \`${moduleName}\` deve começar com o prefixo \`${expectedPrefix}\`.`);
                }
            }
        }
    }
  }
};
