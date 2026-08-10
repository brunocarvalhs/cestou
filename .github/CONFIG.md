# GitHub Actions Secrets Documentation

Este documento descreve os secrets necessários para o funcionamento dos workflows do GitHub Actions
neste repositório. Certifique-se de configurar todos os secrets corretamente para evitar falhas nas
execuções.

## Secrets Necessários

---

## 1. `TOKEN` (GitHub Personal Access Token)

- **Descrição:** Token de autenticação do GitHub usado para criar PRs, comentários e aprovações.
- **Como gerar:**
    1.
  Acesse [GitHub → Settings → Developer settings → Personal access tokens → Tokens (classic)](https://github.com/settings/tokens).
    2. Clique em **Generate new token**.
    3. Selecione escopos:
        - `repo` (acesso total ao repositório)
        - `workflow` (disparar workflows)
    4. Copie o token gerado.
- **Como configurar no GitHub:**  
  Vá em `Settings > Secrets and variables > Actions > New repository secret`, nomeie `TOKEN` e cole
  o valor.

---

## 2. `GOOGLE_SERVICE_JSON` (Firebase JSON)

- **Descrição:** Arquivo `google-services.json` codificado em Base64, necessário para o Firebase no
  Android.
- **Como gerar:**
    1. Baixe do Firebase Console:
       `Project Settings → General → Android App → Download google-services.json`.
    2. Codifique em Base64:
       ```bash
       base64 -w 0 google-services.json
       ```
       > Windows PowerShell:
       > ```powershell
  > [Convert]::ToBase64String([IO.File]::ReadAllBytes("google-services.json"))
  > ```
    3. Copie a saída.
- **Como configurar no GitHub:**  
  Crie o secret `GOOGLE_SERVICE_JSON` e cole o valor.

---

## 3. `KEYSTORE_PASSWORD` (Senha do Keystore)

- **Descrição:** Senha do keystore usada para assinar APK/AAB no Android.

### Como gerar o keystore

Se você ainda não possui um keystore, execute o seguinte comando no terminal:

```bash
keytool -genkey -v -keystore my-release-key.jks -keyalg RSA -keysize 2048 -validity 10000 -alias my-alias
```

### Durante a criação do keystore

- Defina uma senha para o keystore → este será o valor do `KEYSTORE_PASSWORD`.
- Anote o alias da chave → será usado como `KEYSTORE_ALIAS`.
- Defina a senha da chave → será usada como `KEY_PASSWORD`.
- Preencha os dados solicitados (nome, organização, país, etc.).

### Como configurar no GitHub Actions

1. Acesse o repositório no GitHub → **Settings > Secrets and variables > Actions > New repository
   secret**.
2. Nomeie o secret como `KEYSTORE_PASSWORD`.
3. Cole a senha do keystore definida durante a criação.
4. Clique em **Add secret** para salvar.

---

### 4. `KEYSTORE_ALIAS` (Alias do Keystore)

- **Descrição:** Alias da chave dentro do keystore, usado para assinar APK/AAB no Android.
- **Uso:** Necessário no workflow `release.yml` para assinar o bundle de lançamento.

### Como obter o alias

1. Se você criou o keystore, durante a criação será solicitado um **alias**. Por exemplo:
   ```bash
   keytool -genkey -v -keystore my-release-key.jks -keyalg RSA -keysize 2048 -validity 10000 -alias my-alias
   ```
    - No exemplo acima, o alias é my-alias.
2. Se você já possui um keystore e não lembra o alias, pode listar os aliases com:
   ```bash
    keytool -list -v -keystore my-release-key.jks
    ```
    - Ele exibirá todos os aliases disponíveis dentro do keystore.

### Como configurar no GitHub Actions

1. Acesse o repositório no GitHub → Settings > Secrets and variables > Actions > New repository
   secret.
2. Nomeie o secret como KEYSTORE_ALIAS.
3. Cole o valor do alias do keystore.
4. Clique em Add secret para salvar.

> ⚠️ **Importante:** O alias é sensível e deve corresponder exatamente ao definido no keystore para
> que a assinatura funcione corretamente.

---

### 5. `KEY_PASSWORD` (Senha da Chave do Keystore)

- **Descrição:** Senha da chave dentro do keystore, usada para assinar APK/AAB no Android.
- **Uso:** Necessário no workflow `release.yml` para assinar o bundle de lançamento.

### Como obter a senha da chave

1. Durante a criação do keystore, você definiu uma senha para a chave. Por exemplo:
   ```bash
   keytool -genkey -v -keystore my-release-key.jks -keyalg RSA -keysize 2048 -validity 10000 -alias my-alias
    ```
    - A senha que você definiu para a chave é o valor que você deve usar como `KEY_PASSWORD`.
2. Se você esqueceu a senha da chave, infelizmente não há como recuperá-la. Você precisará criar um
   novo keystore e definir uma nova senha para a chave.

### Como configurar no GitHub Actions

1. Acesse o repositório no GitHub → Settings > Secrets and variables > Actions > New repository
   secret.
2. Nomeie o secret como `KEY_PASSWORD`.
3. Cole a senha da chave definida durante a criação do keystore.
4. Clique em **Add secret** para salvar.

> ⚠️ **Importante:** A senha da chave é sensível e deve corresponder exatamente à definida no
> keystore para que a assinatura funcione corretamente.

---

### 6. `FIREBASE_AUTH_TOKEN` (Token de Autenticação do Firebase CLI)

- **Descrição:** Token usado pelo Firebase CLI para autenticar e fazer uploads de APK/AAB.
- **Uso:** Utilizado no workflow `build.yml` para enviar o APK para o Firebase App Distribution.

### Como gerar o token

1. Instale o Firebase CLI, caso ainda não tenha:
   ```bash
   npm install -g firebase-tools
    ```
2. Faça login no Firebase:
    ```bash
    firebase login
     ```
3. Gere o token:
    ```bash
    firebase login:ci
     ```
4. Copie o token gerado.

### Como configurar no GitHub Actions

1. Acesse o repositório no GitHub → **Settings > Secrets and variables > Actions > New repository
   secret**.
2. Nomeie o secret como `FIREBASE_AUTH_TOKEN`.
3. Cole o token gerado.
4. Clique em **Add secret** para salvar.

> ⚠️ **Importante:** Nunca compartilhe este token publicamente, ele dá acesso ao Firebase para
> uploads e distribuições de apps.

---

### 7. `FIREBASE_APP_ID` (ID do Aplicativo no Firebase)

- **Descrição:** Identificador único do aplicativo no Firebase.
- **Uso:** Utilizado no workflow `build.yml` para identificar o aplicativo ao enviar APK/AAB para o Firebase App Distribution.

### Como obter o App ID
1. Acesse o [Firebase Console](https://console.firebase.google.com/).
2. Selecione seu projeto.
3. Vá em **Configurações do Projeto → Geral → Seus Apps**.
4. Copie o **App ID** correspondente ao seu aplicativo Android.

### Como configurar no GitHub Actions
1. Acesse o repositório no GitHub → **Settings > Secrets and variables > Actions > New repository secret**.
2. Nomeie o secret como `FIREBASE_APP_ID`.
3. Cole o valor do App ID.
4. Clique em **Add secret** para salvar.

> ⚠️ **Importante:** Certifique-se de copiar o App ID corretamente, pois ele é sensível a maiúsculas/minúsculas.

---

### 8. `DEPLOY_SERVICE_ACCOUNT_JSON`

- **Descrição**: Credenciais do serviço para deploy no Google Play, codificadas em Base64.
- **Uso**: Necessário no workflow `release.yml` para fazer o upload do AAB para o Google Play.
- **Como configurar**: Codifique o arquivo JSON da conta de serviço em Base64 e adicione como um
  secret chamado `DEPLOY_SERVICE_ACCOUNT_JSON`.

# 🔑 Criando a Conta de Serviço e JSON para Google Play Console

## 1. Acesse o Google Cloud Console

[Google Cloud Console](https://console.cloud.google.com/)

## 2. Crie uma Conta de Serviço

- Vá em **IAM e administrador > Contas de serviço**.
- Clique em **Criar conta de serviço**.
- Dê um nome à conta, por exemplo: `play-publisher-service`.

## 3. Conceda permissões adequadas

- Inicialmente, você pode criar a conta sem permissões.
- As permissões serão vinculadas posteriormente ao Google Play Console.

## 4. Gere a chave JSON

- Clique na conta de serviço criada.
- Vá em **Chaves** → **Adicionar chave** → **Criar nova chave**.
- Escolha o tipo **JSON**.
- O arquivo `.json` será baixado automaticamente.
- Esse é o arquivo que você utilizará como `DEPLOY_SERVICE_ACCOUNT_JSON`.

## ⚠️ Importante

- Nunca compartilhe esse JSON publicamente, pois ele dá acesso direto ao seu projeto.

---

### 9. `DOTENV` (Arquivo de Variáveis de Ambiente)

- **Descrição:** Conteúdo completo do arquivo `.env`, necessário para as configurações do aplicativo e chaves de API.
- **Como configurar:**
    1. Copie o conteúdo do seu arquivo `.env`.
    2. Vá em **Settings > Secrets and variables > Actions**.
    3. Recomenda-se criar um **Secret** (para segurança) chamado `DOTENV`.
    4. Cole o conteúdo diretamente.

> ⚠️ **Importante:** O arquivo `.env` contém chaves de API sensíveis. Use **Secrets** em vez de **Variables** para proteger seus dados.

---

Certifique-se de que todos os secrets estejam configurados corretamente antes de executar os
workflows. Para mais informações, consulte
a [documentação oficial do GitHub Actions](https://docs.github.com/en/actions).