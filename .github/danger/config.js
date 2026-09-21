const fs = require('fs');
const path = require('path');

// Load project-specific configuration exported by the workflow
let projectConfig = {};
const configPath = path.join(__dirname, '../pipeline-config.json');

try {
  if (fs.existsSync(configPath)) {
    projectConfig = JSON.parse(fs.readFileSync(configPath, 'utf8'));
  }
} catch (e) {
  console.warn('Could not load pipeline-config.json, using defaults.');
}

const dangerRules = projectConfig.danger?.rules || {};

module.exports = {
  limits: {
    prSize: dangerRules.pr_size_limit || 500,
  },
  ignoredDirectories: [
    '.github/',
    'docs/',
    'scripts/',
    '.idea/',
    '.kotlin/',
    '.artifacts/',
  ],
  criticalAndroidPatterns: [
    'AndroidManifest.xml',
    'build.gradle',
    'settings.gradle',
    'proguard-rules.pro',
    '/src/',
    '/res/',
    '/assets/',
    '/libs/',
    '.gradle',
  ],
  sensitivePermissions: [
    'android.permission.CAMERA',
    'android.permission.RECORD_AUDIO',
    'android.permission.ACCESS_FINE_LOCATION',
    'android.permission.ACCESS_COARSE_LOCATION',
    'android.permission.BLUETOOTH',
    'android.permission.BLUETOOTH_CONNECT',
    'android.permission.READ_CONTACTS',
    'android.permission.WRITE_CONTACTS',
  ],
  jira: {
    pattern: new RegExp(dangerRules.jira_pattern || '\\[?[A-Z]{2,}-\\d+\\]?'),
  },
  architecture: {
    rules: dangerRules.architecture?.rules || {},
    typeExtractor: /([^:]+):([^:]+)/,
  },
  naming: {
    resourcePrefixes: dangerRules.naming?.resource_prefixes || {},
    suffixes: dangerRules.naming?.suffixes || {
      ViewModel: 'ViewModel',
      Repository: 'Repository',
      RepositoryImpl: 'RepositoryImpl',
    }
  },
  compose: {
    maxParameters: 10,
  },
  paths: {
    blockedLibs: '.github/danger/excludes/blockedLibs.txt',
    deprecatedLibs: '.github/danger/excludes/deprecatedLibs.txt',
  },
  hygiene: {
    forbiddenPatterns: [
      { regex: /println\(|System\.out\.print/, message: 'Do not use println or System.out. Use the project logger.', level: 'fail' },
      { regex: /Log\.[dv]\(/, message: 'Avoid using Log.d or Log.v in production.', level: 'warn' },
      { regex: /TODO/, message: 'Pending TODOs found in this PR.', level: 'warn' },
    ]
  }
};
