const config = require('../config');

function checkPRDescription(danger) {
  const { fail } = danger;
  const prDescription = danger.github?.pr?.body || '';
  if (prDescription.length < 10) {
    fail('A descrição do PR deve ter pelo menos 10 caracteres.');
  }
}

function checkPRTitle(danger) {
  const { fail } = danger;
  const prTitle = danger.github?.pr?.title || '';
  const pattern = /^(feat|fix|docs|style|refactor|perf|test|chore|build|ci|revert|BREAKING CHANGE): .+/;
  if (!pattern.test(prTitle)) {
    fail('O título do PR deve seguir o Conventional Commit.');
  }
}

function checkJiraLink(danger) {
  const { warn } = danger;
  const prTitle = danger.github?.pr?.title || '';
  const prBody = danger.github?.pr?.body || '';

  if (!config.jira.pattern.test(prTitle) && !config.jira.pattern.test(prBody)) {
    warn('Não foi encontrado ID de tarefa (ex: [PROJ-123]) no título ou descrição do PR.');
  }
}

module.exports = async function stepPRMetadata(dangerInstance) {
  checkPRDescription(dangerInstance);
  checkPRTitle(dangerInstance);
  checkJiraLink(dangerInstance);
};
