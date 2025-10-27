const sensitivePermissions = [
  'android.permission.CAMERA',
  'android.permission.RECORD_AUDIO',
  'android.permission.ACCESS_FINE_LOCATION',
  'android.permission.ACCESS_COARSE_LOCATION',
  'android.permission.BLUETOOTH',
  'android.permission.BLUETOOTH_CONNECT',
  'android.permission.READ_CONTACTS',
  'android.permission.WRITE_CONTACTS',
];

function extractAddedRemovedPermissions(diff) {
  const added = [];
  const removed = [];
  if (!diff) return { added, removed };
  const lines = diff.split('\n');
  for (const line of lines) {
    // linhas adicionadas começam com + e removidas com - (exclui linhas de diff que são '+++' ou '---')
    if (line.startsWith('+') && !line.startsWith('+++')) {
      for (const perm of sensitivePermissions) if (line.includes(perm)) added.push(perm);
    }
    if (line.startsWith('-') && !line.startsWith('---')) {
      for (const perm of sensitivePermissions) if (line.includes(perm)) removed.push(perm);
    }
  }
  return { added: Array.from(new Set(added)), removed: Array.from(new Set(removed)) };
}

module.exports = async function stepManifestPermissions(danger) {
  const { git, message, warn } = danger;
  const manifestFiles = (git.created_files || []).concat(git.modified_files || []).concat(git.deleted_files || []).filter(f => f.endsWith('AndroidManifest.xml'));
  if (manifestFiles.length === 0) return;

  for (const file of manifestFiles) {
    try {
      const diff = (await git.diffForFile(file))?.diff;
      const { added, removed } = extractAddedRemovedPermissions(diff);
      if (added.length > 0) {
        warn(`Permissões sensíveis adicionadas em ${file}: ${added.join(', ')} — por favor documente o motivo e o uso.`);
      }
      if (removed.length > 0) {
        message(`Permissões removidas em ${file}: ${removed.join(', ')}.`);
      }
    } catch (err) {
      // Se diff falhar, apenas log e continue
      // eslint-disable-next-line no-console
      console.error('Erro ao analisar manifest:', err.message || err);
    }
  }
};
