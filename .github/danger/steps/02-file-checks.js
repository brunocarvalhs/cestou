const config = require('../config');
const utils = require('../utils');

function checkLibsVersionsFile(files, danger) {
  const { message } = danger;
  if (files.some(f => f.includes('libs.versions'))) {
    message('O arquivo de versões de dependências foi alterado.');
  }
}

function checkModifiedFiles(files, danger) {
  const { message } = danger;
  const kotlinFiles = files.filter(f => f.endsWith('.kt'));
  const xmlFiles = files.filter(f => f.endsWith('.xml'));

  if (kotlinFiles.length > 0) message(`Arquivos Kotlin modificados: ${kotlinFiles.length}`);
  if (xmlFiles.length > 0) message(`Arquivos XML modificados: ${xmlFiles.length}`);
}

function checkForUnitTests(created, modified, deleted, danger) {
  const { message, warn } = danger;
  const testFiles = [...created, ...modified, ...deleted].filter(utils.isTestFile);

  if (testFiles.length === 0) {
    warn('Nenhum arquivo de teste foi criado ou modificado neste PR.');
  } else {
    message(`Arquivos de teste identificados: ${testFiles.length}`);
  }
}

function checkAndroidCoreFiles(files, danger) {
  const { message } = danger;
  const criticalChanged = files.some(f => config.criticalAndroidPatterns.some(p => f.includes(p)));

  if (criticalChanged) {
    message('Arquivos principais do Android (manifest, gradle, etc) foram modificados.');
  }
}

module.exports = async function stepFileChecks(dangerInstance) {
  const { git } = dangerInstance;
  const createdFiles = git.created_files || [];
  const modifiedFiles = git.modified_files || [];
  const deletedFiles = git.deleted_files || [];
  const allFiles = [...createdFiles, ...modifiedFiles, ...deletedFiles];

  const androidFiles = allFiles.filter(utils.isAndroidFile);

  if (androidFiles.length === 0) {
    dangerInstance.message('PR não impacta arquivos do código Android.');
    return;
  }

  checkLibsVersionsFile(androidFiles, dangerInstance);
  checkModifiedFiles(androidFiles, dangerInstance);
  checkForUnitTests(createdFiles, modifiedFiles, deletedFiles, dangerInstance);
  checkAndroidCoreFiles(androidFiles, dangerInstance);
};
