const { danger, fail, warn } = require('danger');

function checkPRDescription() {
  const prDescription = danger.github.pr.body || '';
  if (prDescription.length < 10) {
    fail("A descrição do PR deve ter pelo menos 10 caracteres.");
  }
}

function checkPRTitle() {
  const prTitle = danger.github.pr.title;
  const pattern = /^(feat|fix|docs|style|refactor|perf|test|chore|build|ci|revert|BREAKING CHANGE): .+/;
  if (!pattern.test(prTitle)) {
    fail("O título do PR deve seguir o Conventional Commit.");
  }
}

function checkPRSize() {
  const pr = danger.github.pr;
  const totalChanges = pr.additions + pr.deletions;
  if (totalChanges > 500) {
      warn("Este PR parece grande. Considere dividi-lo em PRs menores para facilitar a revisão.");
  }
}

function checkWIPStatus() {
  const prTitle = danger.github.pr.title;
  if (prTitle.match(/^(WIP|work in progress)/i)) {
      fail("Este PR está marcado como 'Work in Progress'. Remova a marcação 'WIP' quando estiver pronto para revisão.");
  }
}

module.exports = {
  checkPRDescription,
  checkPRTitle,
  checkPRSize,
  checkWIPStatus,
};
