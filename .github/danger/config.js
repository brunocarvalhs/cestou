module.exports = {
  limits: {
    prSize: 500, // lines of code
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
    pattern: /\[?[A-Z]{2,}-\d+\]?/, // Matches [PROJ-123] or PROJ-123
  },
  architecture: {
    // Rules: moduleType: [allowedDependenciesTypes]
    // Types identified by directory name
    rules: {
      feature: ['core', 'domain'],
      core: ['core', 'domain'],
      domain: ['domain'],
      data: ['domain', 'core'], // data can depend on core (common/network) and domain (interfaces)
    },
    // Pattern to identify module type from path
    typeExtractor: /([^:]+):([^:]+)/,
  },
  naming: {
    resourcePrefixes: {
      'feature:products': 'feature_products_',
      'feature:shopping': 'feature_shopping_',
      'feature:settings': 'feature_settings_',
    },
    suffixes: {
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
    reports: {
      junit: '**/build/test-results/**/*.xml',
      lint: '**/build/reports/lint-results*.xml',
      detekt: '**/build/reports/detekt/detekt.xml',
    }
  },
  hygiene: {
    forbiddenPatterns: [
      { regex: /println\(|System\.out\.print/, message: 'Não use println ou System.out. Use o logger do projeto.', level: 'fail' },
      { regex: /Log\.[dv]\(/, message: 'Evite usar Log.d ou Log.v em produção.', level: 'warn' },
      { regex: /TODO/, message: 'Existem TODOs pendentes neste PR.', level: 'warn' },
    ]
  }
};
