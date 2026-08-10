const { danger, message, warn, fail } = require('danger');
const fs = require('fs');
const path = require('path');

// Carrega e executa todos os steps em .github/danger/steps
async function loadAndRunSteps() {
  const stepsDir = path.join(__dirname, 'steps');
  let files = [];
  try {
    files = fs.readdirSync(stepsDir).filter(f => f.endsWith('.js'));
  } catch (e) {
    warn('Não foi possível encontrar o diretório de steps do Danger.');
    return;
  }

  // Ordena alfabeticamente
  files.sort();

  message(`🚀 Executando ${files.length} verificações automáticas...`);

  for (const file of files) {
    const fullPath = path.join(stepsDir, file);
    try {
      const step = require(fullPath);
      if (typeof step === 'function') {
        try {
          const result = step(danger);
          if (result && typeof result.then === 'function') await result;
          // Log success for each step
          const stepName = file.replace(/^\d+-/, '').replace('.js', '');
          console.log(`✅ Step concluído: ${stepName}`);
        } catch (err) {
          console.error(`Erro no step ${file}:`, err.message || err);
          fail(`Falha técnica ao executar verificação: ${file}`);
        }
      }
    } catch (err) {
      console.error(`Falha ao carregar step ${file}:`, err.message || err);
    }
  }
}

loadAndRunSteps();
