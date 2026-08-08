const config = require('../config');
const utils = require('../utils');

module.exports = async function stepManifestPermissions(danger) {
  const { git, message, warn } = danger;
  const manifestFiles = [...git.created_files, ...git.modified_files].filter(f => f.endsWith('AndroidManifest.xml'));

  for (const file of manifestFiles) {
    const addedLines = await utils.getAddedLines(danger, file);
    const addedPermissions = [];

    for (const line of addedLines) {
      config.sensitivePermissions.forEach(perm => {
        if (line.includes(perm)) {
          addedPermissions.push(perm);
        }
      });
    }

    if (addedPermissions.length > 0) {
      warn(`Permissões sensíveis adicionadas em ${file}: ${addedPermissions.join(', ')}. Por favor, garanta que o uso está documentado.`);
    }
  }
};
