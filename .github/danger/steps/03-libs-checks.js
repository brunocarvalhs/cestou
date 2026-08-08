const fs = require('fs');
const config = require('../config');
const utils = require('../utils');

function getLibsFromFile(fileName, danger) {
  const { message, fail } = danger;
  try {
    if (!fs.existsSync(fileName)) return [];
    const fileContent = fs.readFileSync(fileName, 'utf-8');
    return fileContent.split('\n').map(lib => lib.trim()).filter(lib => lib.length > 0);
  } catch (error) {
    fail(`Erro ao ler arquivo de configuração de libs (${fileName}): ${error.message}`);
    return [];
  }
}

async function checkDependencies(danger, files) {
  const blockedLibs = getLibsFromFile(config.paths.blockedLibs, danger);
  const deprecatedLibs = getLibsFromFile(config.paths.deprecatedLibs, danger);

  const gradleFiles = files.filter(f => f.endsWith('.gradle.kts') || f.endsWith('.toml'));

  for (const file of gradleFiles) {
    const addedLines = await utils.getAddedLines(danger, file);

    for (const line of addedLines) {
      // Check Blocked
      blockedLibs.forEach(lib => {
        if (line.includes(lib)) {
          danger.fail(`Biblioteca bloqueada adicionada em ${file}: ${lib}`);
        }
      });

      // Check Deprecated
      deprecatedLibs.forEach(lib => {
        if (line.includes(lib)) {
          danger.warn(`Biblioteca depreciada adicionada em ${file}: ${lib}`);
        }
      });

      // Check Dynamic Versions
      if (/\+ |latest\.release|\[.*,/.test(line)) {
        danger.warn(`Evite usar versões dinâmicas em ${file}: \`${line.trim()}\``);
      }
    }
  }
}

module.exports = async function stepLibsChecks(dangerInstance) {
  const { git } = dangerInstance;
  const allFiles = [...git.created_files, ...git.modified_files, ...git.deleted_files];
  const androidFiles = allFiles.filter(utils.isAndroidFile);

  await checkDependencies(dangerInstance, androidFiles);
};
