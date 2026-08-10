module.exports = async function stepChangelog(danger) {
  const { git, github, warn } = danger;
  const prTitle = github.pr.title || '';
  const isFeatureOrFix = prTitle.startsWith('feat') || prTitle.startsWith('fix');

  if (isFeatureOrFix) {
    const changelogChanged = [...git.modified_files, ...git.created_files].some(f => f.toLowerCase() === 'changelog.md');
    if (!changelogChanged) {
      warn('Este PR introduz uma nova funcionalidade ou correção, mas o arquivo `CHANGELOG.md` não foi atualizado.');
    }
  }
};
