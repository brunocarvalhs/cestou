const utils = require('../utils');

module.exports = async function stepUIChanges(danger) {
  const { git, github, warn } = danger;
  const allFiles = [...git.created_files, ...git.modified_files];
  const uiFiles = allFiles.filter(utils.isUiFile);

  if (uiFiles.length > 0) {
    const prBody = github.pr.body || '';
    const hasMedia = /!\[.*\]\(.*\)|<img|video|mp4|mov/i.test(prBody);

    if (!hasMedia) {
      warn('Mudanças de UI detectadas, mas nenhuma evidência visual (imagem/vídeo) foi encontrada na descrição do PR.');
    }
  }
};
