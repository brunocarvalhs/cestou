const config = require('../config');

function checkPRDescription(danger) {
  const { fail } = danger;
  const prDescription = danger.github?.pr?.body || '';
  if (prDescription.length < 10) {
    fail('📝 **Descrição muito curta!** Por favor, conte-nos um pouco mais sobre o que este PR faz para facilitar a revisão.');
  }
}

function checkPRTitle(danger) {
  const { fail } = danger;
  const prTitle = danger.github?.pr?.title || '';
  const pattern = /^(feat|fix|docs|style|refactor|perf|test|chore|build|ci|revert|BREAKING CHANGE): .+/;
  if (!pattern.test(prTitle)) {
    fail('🏷️ **Título fora do padrão!** Use o padrão Conventional Commits (ex: `feat: add new button`). Isso nos ajuda a gerar o changelog automaticamente.');
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
