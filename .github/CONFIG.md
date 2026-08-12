# CI/CD Infrastructure Guide

This directory contains the agnostic CI/CD infrastructure for the project. It is designed to be independent of the specific application logic, with all project-specific settings centralized in `pipeline-config.yaml`.

## ⚙️ Centralized Configuration (`pipeline-config.yaml`)

The source of truth for all pipeline settings is the [.github/pipeline-config.yaml](file:///Users/bruno/Developers/How-Much/.github/pipeline-config.yaml) file. 

### Key Sections:
- **`project`**: Metadata like name, display name, and package.
- **`build`**: JVM distribution, versions, and build variants (flavors).
- **`test`**: Configuration for unit and integration tests.
- **`linters`**: Static analysis tool definitions.
- **`release`**: Deployment settings for Google Play and GitHub Releases.
- **`danger`**: Rules for automated code review (PR size, Jira patterns, architecture rules).

---

## 🔐 Secrets Documentation

The following secrets must be configured in your GitHub repository (`Settings > Secrets and variables > Actions`):

| Secret | Description |
| :--- | :--- |
| `TOKEN` | GitHub Personal Access Token with `repo` and `workflow` scopes. |
| `GOOGLE_SERVICE_JSON` | `google-services.json` file content encoded in Base64. |
| `KEYSTORE_PASSWORD` | Password for the Android release keystore. |
| `KEYSTORE_ALIAS` | Alias for the signing key in the keystore. |
| `KEY_PASSWORD` | Password for the specific signing key. |
| `FIREBASE_AUTH_TOKEN` | Token generated via `firebase login:ci`. |
| `FIREBASE_APP_ID` | The App ID from Firebase Console (can be overridden in config). |
| `DEPLOY_SERVICE_ACCOUNT_JSON` | Google Play Service Account JSON in Base64. |
| `DOTENV` | Content of the `.env` file for API keys and environment variables. |

---

## 🛠️ Infrastructure Maintenance

- **Danger JS**: Rules are dynamically loaded from `pipeline-config.yaml`. If you need to add a new naming convention or architectural rule, update the `danger.rules` section in the YAML.
- **Issue Forms**: The bug and feature templates use GitHub YAML forms located in `ISSUE_TEMPLATE/`.
