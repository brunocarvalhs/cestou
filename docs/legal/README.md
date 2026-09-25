# Legal pages

`privacy.html` and `terms.html` are the hosted versions of the same copy already shown in-app
(`feature/settings` strings: `settings_privacy_content` / `settings_terms_content`), needed because
Google Play Console requires a reachable Privacy Policy **URL** at submission time — in-app text
alone doesn't satisfy that.

## Where these are hosted

Deployed via Firebase Hosting on the app's existing Firebase project (`how-much-2a72e`, the same
project used by Auth/Analytics/Crashlytics), not GitHub Pages:

- https://how-much-2a72e.web.app/privacy.html
- https://how-much-2a72e.web.app/terms.html

`core/common/.../LegalUrls.kt` points to these. Redeploy after editing either file with:
`firebase deploy --only hosting --project how-much-2a72e`.

GitHub Pages was tried first (repo already has `docs/` set up as its Pages source), but this
GitHub account's Pages sites default to the custom domain `bruno-carvalho.dev.br`, which doesn't
currently resolve (DNS for that domain isn't pointed at GitHub Pages) — that's what caused the
Play Store rejection on 2026-09-25 ("Privacy Policy inválida"). If that domain's DNS ever gets
fixed (A records to GitHub Pages' IPs: 185.199.108/109/110/111.153), migrating back is an option,
but there's no need to chase that for these pages to work.

## Still to do

- `feature/auth`'s `CustomMethodPickerTerms` (the "you agree to our Terms/Privacy" text on the
  sign-in screen) doesn't link out yet — turn it into a clickable link, or add a link below it.
- `feature/settings`'s `settings_item_terms` / `settings_item_privacy` rows — the Legal screen
  (`LegalContentScreen`) currently renders `settings_terms_content`/`settings_privacy_content`
  in-app; either keep that and additionally link out, or replace it with a link to the hosted page.

The privacy page's copy already includes a line about the in-app account deletion feature added
this session (Settings > Data > Delete Account) — keep both copies (in-app and hosted) in sync if
either changes.
