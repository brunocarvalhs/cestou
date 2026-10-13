# Site do Cestou (GitHub Pages)

Site estático, sem build: `index.html` (landing em pt-BR), `changelog.html` (gerado), `assets/`
(CSS, ícone, screenshots copiados de `fastlane/metadata/android/pt-BR/images/phoneScreenshots/`).

## Publicar

1. GitHub → **Settings → Pages → Build and deployment → Deploy from a branch**.
2. Branch: a padrão do repositório; pasta: **`/docs`**. Salvar.
3. `docs/.nojekyll` desliga o Jekyll, para que arquivos como `legal/.well-known/` e os `.md` sejam
   servidos como estão.

Atenção: as páginas desta conta do GitHub usam por padrão o domínio próprio `bruno-carvalho.dev.br`,
que hoje não resolve (ver `legal/README.md`). Antes de divulgar o site, aponte o DNS desse domínio
para o GitHub Pages ou remova o domínio próprio nas configurações da conta. Depois, atualize
`canonical`, `og:url` e `og:image` em `index.html` (marcados com `TODO(owner)`).

## Regenerar o changelog

O `CHANGELOG.md` da raiz muda a cada release. Depois de cada release:

```sh
python3 docs/scripts/build-changelog.py          # reescreve docs/changelog.html
python3 docs/scripts/build-changelog.py --test   # autoteste do parser
```

O script só mostra `feat`/`fix` e os itens de `### Added`/`### Fixed`, e descarta merges e escopos
internos (ci, deps, testes, detekt…). Se algo interno escapar, acrescente a palavra ou o escopo em
`NOISE_WORDS`/`NOISE_SCOPES`.

## Checar links

```sh
python3 docs/scripts/check-links.py
```

Verifica se todo `href`/`src` interno de `docs/**/*.html` aponta para um arquivo (e `#id`) que existe.

## O que NÃO mexer

`docs/legal/` é publicado pelo Firebase Hosting (`firebase.json` → `"public": "docs/legal"`, em
`how-much-2a72e.web.app`). O Google Play tem cadastrada a URL
`https://how-much-2a72e.web.app/delete-account.html`, e o app abre `privacy.html`/`terms.html` e os
convites `join/`. Não mova, renomeie nem altere esses arquivos a partir do site: a landing só aponta
para eles com links relativos (`legal/privacy.html`), que funcionam nos dois hosts.
