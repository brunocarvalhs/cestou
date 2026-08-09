# 🚀 Agnostic Android CI/CD Framework

This folder contains a complete, modular, and agnostic CI/CD framework for Android projects. It automates testing, static analysis, versioning, and deployment using GitHub Actions and DangerJS.

## 📋 Features

- **Unified PR Pipeline**: Tests, Lint, Detekt, and DangerJS in a single workflow.
- **Centralized Config**: Manage all environment variables in one YAML file.
- **DangerJS Automation**: Conventional Commits, Architecture Guard, PR Size, and Code Hygiene checks.
- **Auto-Versioning**: Automatic `versionName` and `versionCode` bump based on commit messages.
- **Release Automation**: Generates AABs and deploys to Google Play/Firebase.

---

## 🚀 How to Setup in a New Project

### 1. Copy Files
Copy the `.github/` folder to the root of your new project.

### 2. Configure `pipeline-config.yml`
Open [.github/pipeline-config.yml](pipeline-config.yml) and adjust the project-specific values:
- `JDK_VERSION`, `NODE_VERSION`
- `APP_NAME`, `PACKAGE_NAME`
- `MAIN_GRADLE_FILE` (usually `app/build.gradle.kts`)
- `CHANGELOG_FILE` (usually `CHANGELOG.md`)

### 3. Configure Danger Rules
Open [.github/danger/config.js](danger/config.js) and update the `PROJECT SPECIFIC RULES` section:
- `architecture.rules`: Define how your modules should depend on each other.
- `naming.resourcePrefixes`: Define prefixes for your Android resources per module.

### 4. GitHub Secrets
Configure the following secrets in your GitHub Repository settings (`Settings > Secrets and variables > Actions`):

| Secret Name | Description |
| :--- | :--- |
| `TOKEN` | Personal Access Token with PR write access (for DangerJS comments). |
| `GOOGLE_SERVICE_JSON` | Base64 encoded `google-services.json` file. |
| `DEPLOY_SERVICE_ACCOUNT_JSON` | Google Play Service Account JSON. |
| `KEYSTORE_PASSWORD` | Password for your release keystore. |
| `KEYSTORE_ALIAS` | Alias for your release key. |
| `KEY_PASSWORD` | Password for your release key. |
| `FIREBASE_AUTH_TOKEN` | Token for Firebase App Distribution. |
| `FIREBASE_APP_ID` | App ID for Firebase. |
| `DOTENV` | (Optional) Content of your `.env` file. |

---

## 🛠️ Workflows

### 1. [Pull Request](workflows/pull_request.yml)
Runs on every PR.
- **Jobs**: Static Analysis, Unit Tests, DangerJS PR Review.
- **Feedback**: Posts a single consolidated comment on the PR.

### 2. [Build](workflows/build.yml)
Runs on push to `develop`.
- **Jobs**: Build Debug APK and deploy to Firebase App Distribution.

### 3. [Release](workflows/release.yml)
Runs on push to `master`.
- **Jobs**: Increments version, creates GitHub Release, builds AAB, and deploys to Google Play.

---

## 📄 License
This framework is agnostic and can be used in any Android project following the standard Gradle structure.
