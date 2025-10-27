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

module.exports = async function stepPRMetadata(dangerInstance) {
  checkPRDescription(dangerInstance);
  checkPRTitle(dangerInstance);
};
