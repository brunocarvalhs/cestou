const fs = require('fs');

// Lê bibliotecas bloqueadas e depreciadas
// Agora recebe 'fail' e 'message' como parâmetros
function getLibsFromFile(fileName, { fail, message }) {
  try {
    const fileContent = fs.readFileSync(fileName, 'utf-8');
    return fileContent.split('\n').map(lib => lib.trim()).filter(lib => lib.length > 0);
  } catch (error) {
    if (error.code === 'ENOENT') {
      // Usa a função 'message' que foi passada como parâmetro
      message(`Arquivo ${fileName} não encontrado.`);
    } else {
      // Usa a função 'fail' que foi passada como parâmetro
      fail(`Erro ao ler ${fileName}: ${error.message}`);
    }
    return [];
  }
}

// Filtra arquivos que impactam o Android (não precisa de mudanças)
function isAndroidFile(file) {
  const ignoredPrefixes = ['.github/', 'docs/', 'scripts/'];
  return !ignoredPrefixes.some(prefix => file.startsWith(prefix));
}

// Verifica se algum arquivo crítico foi alterado (não precisa de mudanças)
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
