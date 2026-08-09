const fs = require('fs');
const path = require('path');

module.exports = async function stepLintReports(danger) {
  const { fail, warn, message } = danger;

  function findFiles(dir, pattern) {
    let results = [];
    if (!fs.existsSync(dir)) return results;
    const list = fs.readdirSync(dir);
    list.forEach(file => {
      file = path.join(dir, file);
      const stat = fs.statSync(file);
      if (stat && stat.isDirectory()) {
        if (!file.includes('node_modules') && !file.includes('.git')) {
          results = results.concat(findFiles(file, pattern));
        }
      } else if (file.match(pattern)) {
        results.push(file);
      }
    });
    return results;
  }

  const lintReports = findFiles(process.cwd(), /lint-results.*\.xml$/);

  let errors = 0;
  let warnings = 0;
  const issues = [];

  lintReports.forEach(report => {
    try {
      const content = fs.readFileSync(report, 'utf-8');

      // Regex to find issues: <issue id="..." severity="Error/Warning" message="..." category="..." priority="..." summary="..." explanation="..." errorLine1="..." errorLine2="...">
      const issueMatches = content.matchAll(/<issue\s+id="([^"]+)"\s+severity="([^"]+)"\s+message="([^"]+)"[^>]*>/g);

      for (const match of issueMatches) {
        const id = match[1];
        const severity = match[2];
        const msg = match[3];

        if (severity === 'Error' || severity === 'Fatal') {
          errors++;
          issues.push(`- ❌ **${id}**: ${msg}`);
        } else {
          warnings++;
          // We limit warnings in the main PR comment if they are too many
          if (warnings < 10) {
             issues.push(`- ⚠️ **${id}**: ${msg}`);
          }
        }
      }
    } catch (e) {
      console.error(`Erro ao ler relatório de lint ${report}:`, e.message);
    }
  });

  if (errors > 0) {
    fail(`❌ **Android Lint**: Encontrados **${errors}** erros.\n\n${issues.filter(i => i.includes('❌')).join('\n')}`);
  }

  if (warnings > 0) {
    warn(`⚠️ **Android Lint**: Encontrados **${warnings}** avisos. Verifique o relatório completo para detalhes.`);
  } else if (lintReports.length > 0 && errors === 0) {
    message('✅ Android Lint: Nenhuma falha crítica encontrada.');
  }
};
