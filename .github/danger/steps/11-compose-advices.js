const config = require('../config');
const utils = require('../utils');

module.exports = async function stepComposeAdvices(danger) {
  const { git, warn } = danger;
  const kotlinFiles = [...git.created_files, ...git.modified_files].filter(f => f.endsWith('.kt'));

  for (const file of kotlinFiles) {
    const addedLines = await utils.getAddedLines(danger, file);
    let inComposable = false;
    let composableParams = 0;

    for (const line of addedLines) {
      if (line.includes('@Composable')) {
        inComposable = true;
        composableParams = 0;
        continue;
      }

      if (inComposable) {
        // Count parameters - very simple heuristic: count commas in lines until closing parenthesis
        // This is not perfect but works for many cases
        const commaCount = (line.match(/,/g) || []).length;
        composableParams += commaCount;

        if (line.includes(') {') || line.includes(')=')) {
          inComposable = false;
          if (composableParams >= config.compose.maxParameters) {
             warn(`Jetpack Compose: A função Composable em \`${file}\` tem muitos parâmetros (${composableParams + 1}). Considere usar um Data Class ou Hoisting de Estado.`);
          }
        }
      }

      // Check for mutableStateOf without remember
      if (line.includes('mutableStateOf(') && !line.includes('remember') && !file.includes('ViewModel')) {
        warn(`Jetpack Compose: Uso de \`mutableStateOf\` sem \`remember\` detectado em \`${file}\`. Isso pode causar problemas de recomposição.`);
      }
    }
  }
};
