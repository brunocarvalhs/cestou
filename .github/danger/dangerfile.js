const { danger, message, warn, fail } = require('danger');
const fs = require('fs');

// Verifica o tamanho dos arquivos
function checkFileSizes(files, maxSizeKB = 500) {
  files.forEach(file => {
    try {
      // Ignora arquivos que não existem no sistema de arquivos local (por exemplo, submódulos do git)
      if (!fs.existsSync(file)) return;
      const stats = fs.statSync(file);
      const fileSizeInKB = stats.size / 1024;
      if (fileSizeInKB > maxSizeKB) {
        warn(`O arquivo ${file} tem ${fileSizeInKB.toFixed(2)}KB, que é maior que o limite de ${maxSizeKB}KB.`);
      }
    } catch (error) {
      // Ignora arquivos que não existem mais (por exemplo, deletados)
      if (error.code !== 'ENOENT') {
        fail(`Erro ao verificar o tamanho do arquivo ${file}: ${error.message}`);
      }
    }
  });
}

// Alerta sobre TODOs no código
async function checkForTodos(files) {
  for (const file of files) {
    const content = await danger.git.diffForFile(file);
    if (content?.diff) {
      const addedLines = content.diff.split('\\n').filter(l => l.startsWith('+'));
      addedLines.forEach(line => {
        if (line.match(/TODO/i)) {
          warn(`Encontrado um "TODO" no arquivo ${file}. Considere criar uma issue para isso.`);
        }
      });
    }
  }
}

// Verifica strings hardcoded em arquivos de layout XML
async function checkForHardcodedStrings(files) {
  const layoutFiles = files.filter(f => f.match(/res\\/layout\\/.*\\.xml$/));
  for (const file of layoutFiles) {
    const content = await danger.git.diffForFile(file);
    if (content?.diff) {
      const addedLines = content.diff.split('\\n').filter(l => l.startsWith('+'));
      addedLines.forEach(line => {
        if (line.includes('android:text="')) {
          if (!line.includes('@string/') && !line.includes('@={')) {
            warn(`String hardcoded encontrada em ${file}: ${line.trim()}. Extraia para um resource de string.`);
          }
        }
      });
    }
  }
}

// Lê bibliotecas bloqueadas e depreciadas
function getLibsFromFile(fileName) {
  try {
    const fileContent = fs.readFileSync(fileName, 'utf-8');
    return fileContent.split('\\n').map(lib => lib.trim()).filter(lib => lib.length > 0);
  } catch (error) {
    if (error.code === 'ENOENT') {
      message(`Arquivo ${fileName} não encontrado.`);
    } else {
      fail(`Erro ao ler ${fileName}: ${error.message}`);
    }
    return [];
  }
}

const blockedLibs = getLibsFromFile('.github/danger/blockedLibs.txt');
const deprecatedLibs = getLibsFromFile('.github/danger/deprecatedLibs.txt');

// Filtra arquivos que impactam o Android
function isAndroidFile(file) {
  const ignoredPrefixes = ['.github/', 'docs/', 'scripts/'];
  return !ignoredPrefixes.some(prefix => file.startsWith(prefix));
}

// Verifica se algum arquivo crítico foi alterado
function hasAndroidChanges(files) {
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
  return files.some(file => criticalPatterns.some(pattern => file.includes(pattern)));
}

// Checagens de PR
function checkPRDescription() {
  const prDescription = danger.github.pr.body || '';
  if (prDescription.length < 10) {
    fail("A descrição do PR deve ter pelo menos 10 caracteres.");
  }
}

function checkPRTitle() {
  const prTitle = danger.github.pr.title;
  const pattern = /^(feat|fix|docs|style|refactor|perf|test|chore|build|ci|revert|BREAKING CHANGE): .+/;
  if (!pattern.test(prTitle)) {
    fail("O título do PR deve seguir o Conventional Commit.");
  }
}

// Verifica libs.versions.gradle
function checkLibsVersionsFile(files) {
  if (files.includes('libs.versions.gradle')) {
    message("O arquivo `libs.versions.gradle` foi alterado.");
  }
}

// Verifica arquivos Kotlin e XML
function checkModifiedFiles(files) {
  const kotlinFiles = files.filter(f => f.endsWith('.kt'));
  const xmlFiles = files.filter(f => f.endsWith('.xml'));

  if (kotlinFiles.length > 0) message(`Arquivos Kotlin modificados: ${kotlinFiles.join(', ')}`);
  if (xmlFiles.length > 0) message(`Arquivos XML modificados: ${xmlFiles.join(', ')}`);
}

// Verifica arquivos de teste
function checkForUnitTests(created, modified, deleted) {
  const testPattern = /Test/;
  const testModified = modified.filter(f => testPattern.test(f));
  const testCreated = created.filter(f => testPattern.test(f));
  const testDeleted = deleted.filter(f => testPattern.test(f));

  if (testModified.length === 0 && testCreated.length === 0 && testDeleted.length === 0) {
    message("Nenhum arquivo de teste foi criado, modificado ou deletado.");
  }
}

// Arquivos Jetpack Compose
function checkForComposeFiles(files) {
  const composeFiles = files.filter(f => f.includes('Composable') || f.includes('Compose'));
  if (composeFiles.length > 0) {
    message(`Arquivos Jetpack Compose modificados: ${composeFiles.join(', ')}`);
  }
}

// Arquivos críticos Android
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

// Verifica libs bloqueadas
async function checkForBlockedLibs(files) {
  const gradleFiles = files.filter(f => f.endsWith('build.gradle.kts') || f.includes('libs.versions.toml'));
  for (const file of gradleFiles) {
    const content = await danger.git.diffForFile(file);
    if (content?.diff) {
      const addedLines = content.diff.split('\\n').filter(l => l.startsWith('+'));
      blockedLibs.forEach(lib => {
        const regex = new RegExp(`\\\\b${lib.replace(/[-\\/\\\\^$*+?.()|[\]{}]/g, '\\\\$&')}\\\\b`);
        if (addedLines.some(line => regex.test(line))) {
          fail(`Biblioteca bloqueada ${lib} adicionada em ${file}.`);
        }
      });
    }
  }
}

// Verifica libs depreciadas
async function checkForDeprecatedLibs(files) {
  const gradleFiles = files.filter(f => f.endsWith('build.gradle.kts') || f.includes('libs.versions.toml'));
  for (const file of gradleFiles) {
    const content = await danger.git.diffForFile(file);
    if (content?.diff) {
      const addedLines = content.diff.split('\\n').filter(l => l.startsWith('+'));
      deprecatedLibs.forEach(lib => {
        if (addedLines.some(line => line.includes(lib) || line.includes(lib.split(':')[0]))) {
          message(`Biblioteca depreciada ${lib} adicionada em ${file}.`);
        }
      });
    }
  }
}

// Executa verificações
async function runPRChecks() {
  checkPRDescription();
  checkPRTitle();

  const createdFiles = danger.git.created_files || [];
  const modifiedFiles = danger.git.modified_files || [];
  const deletedFiles = danger.git.deleted_files || [];
  const allFiles = [...createdFiles, ...modifiedFiles, ...deletedFiles];
  const createdAndModifiedFiles = [...createdFiles, ...modifiedFiles];

  // Filtra arquivos Android relevantes
  const androidFiles = allFiles.filter(isAndroidFile);
  const androidCreatedAndModified = createdAndModifiedFiles.filter(isAndroidFile);

  const androidChanges = hasAndroidChanges(androidFiles);

  // Checagens sempre executadas
  checkLibsVersionsFile(androidFiles);
  checkModifiedFiles(androidFiles);
  await checkForBlockedLibs(androidFiles);
  await checkForDeprecatedLibs(androidFiles);

  // --- Calling new functions for all relevant files ---
  checkFileSizes(androidCreatedAndModified);
  await checkForTodos(androidCreatedAndModified);
  await checkForHardcodedStrings(androidCreatedAndModified);

  // Checagens condicionais só se houver impacto no Android
  if (androidChanges) {
    checkForUnitTests(createdFiles.filter(isAndroidFile), modifiedFiles.filter(isAndroidFile), deletedFiles.filter(isAndroidFile));
    checkForComposeFiles(androidFiles);
    checkAndroidCoreFiles(androidFiles);
  } else {
    message("PR não impacta arquivos críticos do Android, warnings de testes e core foram ignorados.");
  }
}

// Rodar Danger
runPRChecks();
