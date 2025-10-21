const fs = require('fs');

function getLibsFromFile(fileName, danger) {
  const { message, fail } = danger;
  try {
    const fileContent = fs.readFileSync(fileName, 'utf-8');
    return fileContent.split('\n').map(lib => lib.trim()).filter(lib => lib.length > 0);
  } catch (error) {
    if (error.code === 'ENOENT') {
      message(`Arquivo ${fileName} não encontrado.`);
    } else {
      fail(`Erro ao ler ${fileName}: ${error.message}`);
    }
    return [];
  }
}

async function checkForBlockedLibs(dangerInstance, files) {
  const blockedLibs = getLibsFromFile('.github/danger/excludes/blockedLibs.txt', dangerInstance);
  const gradleFiles = files.filter(f => f.endsWith('build.gradle.kts') || f.includes('libs.versions.toml'));
  for (const file of gradleFiles) {
    const content = await dangerInstance.git.diffForFile(file);
    if (content?.diff) {
      const addedLines = content.diff.split('\n').filter(l => l.startsWith('+'));
      blockedLibs.forEach(lib => {
        const regex = new RegExp(`\\b${lib.replace(/[-\\/\\^$*+?.()|[\\]{}]/g, '\\$&')}\\b`);
        if (addedLines.some(line => regex.test(line))) {
          dangerInstance.fail(`Biblioteca bloqueada ${lib} adicionada em ${file}.`);
        }
      });
    }
  }
}

async function checkForDeprecatedLibs(dangerInstance, files) {
  const deprecatedLibs = getLibsFromFile('.github/danger/excludes/deprecatedLibs.txt', dangerInstance);
  const gradleFiles = files.filter(f => f.endsWith('build.gradle.kts') || f.includes('libs.versions.toml'));
  for (const file of gradleFiles) {
    const content = await dangerInstance.git.diffForFile(file);
    if (content?.diff) {
      const addedLines = content.diff.split('\n').filter(l => l.startsWith('+'));
      deprecatedLibs.forEach(lib => {
        if (addedLines.some(line => line.includes(lib) || line.includes(lib.split(':')[0]))) {
          dangerInstance.message(`Biblioteca depreciada ${lib} adicionada em ${file}.`);
        }
      });
    }
  }
}

module.exports = async function stepLibsChecks(dangerInstance) {
  const createdFiles = dangerInstance.git.created_files || [];
  const modifiedFiles = dangerInstance.git.modified_files || [];
  const deletedFiles = dangerInstance.git.deleted_files || [];
  const allFiles = [...createdFiles, ...modifiedFiles, ...deletedFiles];
  const androidFiles = allFiles.filter(f => !['.github/', 'docs/', 'scripts/'].some(p => f.startsWith(p)));

  await checkForBlockedLibs(dangerInstance, androidFiles);
  await checkForDeprecatedLibs(dangerInstance, androidFiles);
};
