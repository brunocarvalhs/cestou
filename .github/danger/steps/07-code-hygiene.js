const config = require('../config');
const utils = require('../utils');

module.exports = async function stepCodeHygiene(danger) {
  const { git } = danger;
  const filesToCheck = [...git.created_files, ...git.modified_files].filter(utils.isAndroidFile);

  for (const file of filesToCheck) {
    const addedLines = await utils.getAddedLines(danger, file);

    for (const line of addedLines) {
      config.hygiene.forbiddenPatterns.forEach(pattern => {
        if (pattern.regex.test(line)) {
          const message = `${pattern.message} em ${file}`;
          if (pattern.level === 'fail') {
            danger.fail(message);
          } else {
            danger.warn(message);
          }
        }
      });

      // Check for hardcoded strings that look like UI text (simple heuristic)
      if (file.endsWith('.kt') && !utils.isTestFile(file)) {
        // match text = "Something" where it's not a log or constant
        const hardcodedStringRegex = /text\s*=\s*"([^"]{4,})"/;
        if (hardcodedStringRegex.test(line)) {
            danger.warn(`Possível string hardcoded em ${file}: \`${line.trim()}\`. Considere usar strings.xml.`);
        }
      }
    }
  }
};
