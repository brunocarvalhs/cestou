const config = require('../config');
const utils = require('../utils');

module.exports = async function stepModuleArchitecture(danger) {
  const { git, fail, warn } = danger;
  const gradleFiles = git.modified_files.filter(f => f.endsWith('build.gradle.kts'));

  for (const file of gradleFiles) {
    const currentModuleName = utils.getModuleName(file);
    if (!currentModuleName) continue;

    const currentModuleType = utils.getModuleType(currentModuleName);
    const allowedTypes = config.architecture.rules[currentModuleType];

    if (!allowedTypes) continue;

    const addedLines = await utils.getAddedLines(danger, file);
    for (const line of addedLines) {
      // Look for project dependencies: implementation(project(":type:name"))
      const projectDepMatch = line.match(/project\(":(.+):(.+)"\)/);
      if (projectDepMatch) {
        const depType = projectDepMatch[1];
        const depName = projectDepMatch[2];
        const depFullName = `${depType}:${depName}`;

        if (!allowedTypes.includes(depType)) {
          fail(`Violação de Arquitetura: Módulo \`${currentModuleName}\` (\`${currentModuleType}\`) não pode depender de \`${depFullName}\` (\`${depType}\`).`);
        }
      }
    }
  }
};
