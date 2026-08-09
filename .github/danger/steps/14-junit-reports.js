const fs = require('fs');
const path = require('path');
const glob = require('glob');

module.exports = async function stepJUnitReports(danger) {
  const { fail, message } = danger;

  // glob is usually available in many environments or we can use a simple recursive search
  // For simplicity in this environment, let's assume we might need to install glob or use a helper
  // I will use a simple recursive file finder if glob is not guaranteed

  function findFiles(dir, pattern) {
    let results = [];
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

  const testReports = findFiles(process.cwd(), /test-results\/.*\.xml$/);

  let totalTests = 0;
  let totalFailures = 0;
  const failureDetails = [];

  testReports.forEach(report => {
    try {
      const content = fs.readFileSync(report, 'utf-8');

      // Simple regex to extract testsuite info
      const suiteMatch = content.match(/<testsuite.*tests="(\d+)".*failures="(\d+)"/);
      if (suiteMatch) {
        totalTests += parseInt(suiteMatch[1], 10);
        totalFailures += parseInt(suiteMatch[2], 10);
      }

      // Extract failure details
      const failureMatches = content.matchAll(/<testcase classname="([^"]+)" name="([^"]+)"[^>]*>\s*<failure[^>]*>([\s\S]*?)<\/failure>/g);
      for (const match of failureMatches) {
        failureDetails.push(`- **${match[1]}**: \`${match[2]}\``);
      }
    } catch (e) {
      console.error(`Erro ao ler relatório de teste ${report}:`, e.message);
    }
  });

  if (totalFailures > 0) {
    fail(`❌ **${totalFailures}** testes unitários falharam (de um total de ${totalTests}).\n\n${failureDetails.join('\n')}`);
  } else if (totalTests > 0) {
    message(`✅ Todos os **${totalTests}** testes unitários passaram com sucesso!`);
  }
};
