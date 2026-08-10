const config = require('../config');

module.exports = async function stepPRSize(danger) {
  const { github, warn, message } = danger;

  // additions + deletions from GitHub API
  const threshold = config.limits.prSize;
  const totalChanges = (github.pr.additions || 0) + (github.pr.deletions || 0);

  if (totalChanges > threshold) {
    warn(`Este PR é grande (${totalChanges} linhas). Considere dividi-lo em PRs menores para facilitar a revisão.`);
  } else {
    message(`Tamanho do PR adequado para revisão (${totalChanges} linhas).`);
  }
};
