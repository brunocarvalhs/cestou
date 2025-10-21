const { danger, message, fail } = require('danger');
const { isAndroidFile, hasAndroidChanges } = require('./rules/utils');
const { checkPRDescription, checkPRTitle, checkPRSize, checkWIPStatus } = require('./rules/pr-checks');
const { checkForBlockedLibs, checkForDeprecatedLibs, checkLibsVersionsFile } = require('./rules/dependency-checks');
const { checkModifiedFiles, checkForUnitTests, checkForComposeFiles, checkAndroidCoreFiles, checkScreenshotsForUIChanges } = require('./rules/file-checks');

// Executa verificações
async function runPRChecks() {
  // PR Checks
  checkPRDescription();
  checkPRTitle();
  checkPRSize();
  checkWIPStatus();

  const createdFiles = danger.git.created_files || [];
  const modifiedFiles = danger.git.modified_files || [];
  const deletedFiles = danger.git.deleted_files || [];
  const allFiles = [...createdFiles, ...modifiedFiles, ...deletedFiles];

  // Filtra arquivos Android relevantes
  const androidFiles = allFiles.filter(isAndroidFile);

  const androidChanges = hasAndroidChanges(androidFiles);

  // Checagens sempre executadas
  checkLibsVersionsFile(androidFiles);
  checkModifiedFiles(androidFiles);
  await checkForBlockedLibs(androidFiles);
  await checkForDeprecatedLibs(androidFiles);

  // Checagens condicionais só se houver impacto no Android
  if (androidChanges) {
    checkForUnitTests(
        createdFiles.filter(isAndroidFile), 
        modifiedFiles.filter(isAndroidFile), 
        deletedFiles.filter(isAndroidFile)
    );
    checkForComposeFiles(androidFiles);
    checkAndroidCoreFiles(androidFiles);
    checkScreenshotsForUIChanges(androidFiles);
  } else {
    message("PR não impacta arquivos críticos do Android, warnings de testes e core foram ignorados.");
  }
}

// Rodar Danger
runPRChecks();
