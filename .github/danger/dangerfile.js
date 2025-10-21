const { danger } = require('danger');
const fs = require('fs');
const path = require('path');

// Carrega e executa todos os steps em .github/danger/steps
async function loadAndRunSteps() {
  const stepsDir = path.join(__dirname, 'steps');
  let files = [];
  try {
    files = fs.readdirSync(stepsDir).filter(f => f.endsWith('.js'));
  } catch (e) {
    // Se não existir steps, nada a fazer
    return;
  }

  // Ordena alfabeticamente para permitir controle via prefixo numérico
  files.sort();

  for (const file of files) {
    const fullPath = path.join(stepsDir, file);
    try {
      // eslint-disable-next-line global-require, import/no-dynamic-require
      const step = require(fullPath);
      if (typeof step === 'function') {
        // Cada step recebe o objeto danger
        // Isola erros por step para não interromper execuções subsequentes
        try {
          // permitir que o step seja sync ou async
          const result = step(danger);
          if (result && typeof result.then === 'function') await result;
        } catch (err) {
          // log e continua
          // eslint-disable-next-line no-console
          console.error(`Erro no step ${file}:`, err.message || err);
        }
      }
    } catch (err) {
      // eslint-disable-next-line no-console
      console.error(`Falha ao carregar step ${file}:`, err.message || err);
    }
  }
}

loadAndRunSteps();