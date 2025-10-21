const { danger, message, fail } = require('danger');
const { getLibsFromFile } = require('./utils');

const blockedLibs = getLibsFromFile('.github/danger/blockedLibs.txt', { fail, message });
const deprecatedLibs = getLibsFromFile('.github/danger/deprecatedLibs.txt', { fail, message });

async function checkForBlockedLibs(files) {
  const gradleFiles = files.filter(
    f => f.endsWith('build.gradle.kts') || f.includes('libs.versions.toml')
  );

  for (const file of gradleFiles) {
    const content = await danger.git.diffForFile(file);
    if (content?.diff) {
      const addedLines = content.diff
        .split('\n')
        .filter(l => l.startsWith('+'));

      blockedLibs.forEach(lib => {
        const regex = new RegExp(`\\b${lib.replace(/[-\/\\^$*+?.()|[\]{}]/g, '\\$&')}\\b`);
        if (addedLines.some(line => regex.test(line))) {
          fail(`Biblioteca bloqueada ${lib} adicionada em ${file}.`);
        }
      });
    }
  }
}

async function checkForDeprecatedLibs(files) {
  const gradleFiles = files.filter(
    f => f.endsWith('build.gradle.kts') || f.includes('libs.versions.toml')
  );

  for (const file of gradleFiles) {
    const content = await danger.git.diffForFile(file);
    if (content?.diff) {
      const addedLines = content.diff
        .split('\n')
        .filter(l => l.startsWith('+'));

      deprecatedLibs.forEach(lib => {
        const libName = lib.split(':')[0];
        if (addedLines.some(line => line.includes(lib) || line.includes(libName))) {
          message(`Biblioteca depreciada ${lib} adicionada em ${file}.`);
        }
      });
    }
  }
}

function checkLibsVersionsFile(files) {
  if (files.includes('libs.versions.gradle')) {
    message("O arquivo `libs.versions.gradle` foi alterado.");
  }
}

module.exports = {
  checkForBlockedLibs,
  checkForDeprecatedLibs,
  checkLibsVersionsFile,
};
