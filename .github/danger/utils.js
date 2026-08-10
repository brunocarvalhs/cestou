const config = require('./config');

function isAndroidFile(file) {
  return !config.ignoredDirectories.some(prefix => file.startsWith(prefix));
}

function isTestFile(file) {
  return file.includes('/test/') || file.includes('/androidTest/') || /Test\.kt$/.test(file);
}

function isUiFile(file) {
  return file.endsWith('.xml') || file.includes('Composable') || file.includes('Screen') || file.includes('Component');
}

function getModuleName(file) {
  // Example path: feature/products/src/main/... -> feature:products
  const parts = file.split('/');
  if (parts[0] === 'feature' || parts[0] === 'core') {
    return `${parts[0]}:${parts[1]}`;
  }
  return null;
}

function getModuleType(moduleName) {
  if (!moduleName) return null;
  return moduleName.split(':')[0];
}

async function getFileContent(danger, file) {
  try {
    return await danger.github.utils.fileContents(file);
  } catch (e) {
    // Fallback if not on GitHub context or file deleted
    return '';
  }
}

async function getAddedLines(danger, file) {
  try {
    const diff = await danger.git.diffForFile(file);
    if (!diff || !diff.diff) return [];
    return diff.diff.split('\n').filter(line => line.startsWith('+') && !line.startsWith('+++'));
  } catch (e) {
    return [];
  }
}

module.exports = {
  isAndroidFile,
  isTestFile,
  isUiFile,
  getAddedLines,
};
