const fs = require('fs');
const path = require('path');

module.exports = async function stepDetektReports(danger) {
  const { warn, message } = danger;

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

  const detektReports = findFiles(process.cwd(), /detekt\.xml$/);

  let issuesCount = 0;
  const issueDetails = [];

  detektReports.forEach(report => {
    try {
      const content = fs.readFileSync(report, 'utf-8');

      // Detekt XML: <file name="..."><error line="..." column="..." severity="..." message="..." source="..." /></file>
      const errorMatches = content.matchAll(/<error\s+line="(\d+)"\s+column="\d+"\s+severity="([^"]+)"\s+message="([^"]+)"\s+source="([^"]+)"/g);

      for (const match of errorMatches) {
        issuesCount++;
        if (issuesCount < 15) { // Limit to avoid too long comments
           issueDetails.push(`- 🛠️ **${match[4]}**: ${match[3]} (Linha ${match[1]})`);
        }
      }
    } catch (e) {
      console.error(`Erro ao ler relatório Detekt ${report}:`, e.message);
    }
  });

  if (issuesCount > 0) {
    warn(`🛠️ **Detekt**: Encontrados **${issuesCount}** problemas de estilo/qualidade.\n\n${issueDetails.join('\n')}${issuesCount > 15 ? '\n...e mais.' : ''}`);
  } else if (detektReports.length > 0) {
    message('✅ Detekt: Código limpo e seguindo os padrões.');
  }
};
