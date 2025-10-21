const fs = require('fs');
const { message, fail } = require('danger');

// Lê bibliotecas bloqueadas e depreciadas
function getLibsFromFile(fileName) {
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

module.exports = {
  getLibsFromFile,
  isAndroidFile,
  hasAndroidChanges,
};
