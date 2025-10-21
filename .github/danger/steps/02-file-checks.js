const fs = require('fs');

function isAndroidFile(file) {
  const ignoredPrefixes = ['.github/', 'docs/', 'scripts/'];
  return !ignoredPrefixes.some(prefix => file.startsWith(prefix));
}

function checkLibsVersionsFile(files, danger) {
  const { message } = danger;
  if (files.includes('libs.versions.gradle')) {
    message('O arquivo `libs.versions.gradle` foi alterado.');
  }
}

function checkModifiedFiles(files, danger) {
  const { message } = danger;
  const kotlinFiles = files.filter(f => f.endsWith('.kt'));
  const xmlFiles = files.filter(f => f.endsWith('.xml'));

  if (kotlinFiles.length > 0) message(`Arquivos Kotlin modificados: ${kotlinFiles.join(', ')}`);
  if (xmlFiles.length > 0) message(`Arquivos XML modificados: ${xmlFiles.join(', ')}`);
}

function checkForUnitTests(created, modified, deleted, danger) {
  const { message } = danger;
  const testPattern = /Test/;
  const testModified = modified.filter(f => testPattern.test(f));
  const testCreated = created.filter(f => testPattern.test(f));
  const testDeleted = deleted.filter(f => testPattern.test(f));

  if (testModified.length === 0 && testCreated.length === 0 && testDeleted.length === 0) {
    message('Nenhum arquivo de teste foi criado, modificado ou deletado.');
  }
}

function checkForComposeFiles(files, danger) {
  const { message } = danger;
  const composeFiles = files.filter(f => f.includes('Composable') || f.includes('Compose'));
  if (composeFiles.length > 0) {
    message(`Arquivos Jetpack Compose modificados: ${composeFiles.join(', ')}`);
  }
}

function checkAndroidCoreFiles(files, danger) {
  const { message } = danger;
  const criticalPatterns = [
    'AndroidManifest.xml',
    'build.gradle',
    'settings.gradle',
    'proguard-rules.pro',
    '/src/',
    '/res/',
    '/assets/',
    '/libs/',
    '.gradle',
  ];
  const criticalChanged = files.some(f => criticalPatterns.some(p => f.includes(p)));

  if (criticalChanged) {
    message('Arquivos principais do Android foram modificados.');
  } else {
    message('Nenhum arquivo principal do Android foi modificado.');
  }
}

module.exports = async function stepFileChecks(dangerInstance) {
  const createdFiles = dangerInstance.git.created_files || [];
  const modifiedFiles = dangerInstance.git.modified_files || [];
  const deletedFiles = dangerInstance.git.deleted_files || [];
  const allFiles = [...createdFiles, ...modifiedFiles, ...deletedFiles];

  const androidFiles = allFiles.filter(isAndroidFile);
  const androidChanges = androidFiles.some(f => [
    'AndroidManifest.xml', 'build.gradle', 'settings.gradle', 'proguard-rules.pro', '/src/', '/res/', '/assets/', '/libs/', '.gradle'
  ].some(p => f.includes(p)));

  checkLibsVersionsFile(androidFiles, dangerInstance);
  checkModifiedFiles(androidFiles, dangerInstance);

  if (androidChanges) {
    checkForUnitTests(createdFiles.filter(isAndroidFile), modifiedFiles.filter(isAndroidFile), deletedFiles.filter(isAndroidFile), dangerInstance);
    checkForComposeFiles(androidFiles, dangerInstance);
    checkAndroidCoreFiles(androidFiles, dangerInstance);
  } else {
    const { message } = dangerInstance;
    message('PR não impacta arquivos críticos do Android, warnings de testes e core foram ignorados.');
  }
};
