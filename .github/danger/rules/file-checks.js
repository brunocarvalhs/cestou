const { message, warn } = require('danger');

function checkModifiedFiles(files) {
  const kotlinFiles = files.filter(f => f.endsWith('.kt'));
  const xmlFiles = files.filter(f => f.endsWith('.xml'));

  if (kotlinFiles.length > 0) message(`Arquivos Kotlin modificados: ${kotlinFiles.join(', ')}`);
  if (xmlFiles.length > 0) message(`Arquivos XML modificados: ${xmlFiles.join(', ')}`);
}

function checkForUnitTests(created, modified, deleted) {
  const testPattern = /Test/;
  const testModified = modified.filter(f => testPattern.test(f));
  const testCreated = created.filter(f => testPattern.test(f));
  const testDeleted = deleted.filter(f => testPattern.test(f));

  if (testModified.length === 0 && testCreated.length === 0 && testDeleted.length === 0) {
    message("Nenhum arquivo de teste foi criado, modificado ou deletado.");
  }
}

function checkForComposeFiles(files) {
  const composeFiles = files.filter(f => f.includes('Composable') || f.includes('Compose'));
  if (composeFiles.length > 0) {
    message(`Arquivos Jetpack Compose modificados: ${composeFiles.join(', ')}`);
  }
}

function checkAndroidCoreFiles(files) {
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
    message("Arquivos principais do Android foram modificados.");
  } else {
    message("Nenhum arquivo principal do Android foi modificado.");
  }
}

function checkScreenshotsForUIChanges(files) {
    const prBody = danger.github.pr.body || '';
    const hasImages = prBody.includes('<img src=') || prBody.match(/!\[.*\]\(.*\)/);

    const hasUIChanges = files.some(file =>
        (file.endsWith('.xml') && file.includes('res/layout')) ||
        (file.endsWith('.kt') && (file.includes('Composable') || file.includes('Compose')))
    );

    if (hasUIChanges && !hasImages) {
        warn("Este PR parece modificar a UI. Considere adicionar screenshots ou GIFs para facilitar a revisão visual.");
    }
}

module.exports = {
  checkModifiedFiles,
  checkForUnitTests,
  checkForComposeFiles,
  checkAndroidCoreFiles,
  checkScreenshotsForUIChanges,
};
