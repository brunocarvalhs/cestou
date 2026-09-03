# Legal pages

`privacy.html` and `terms.html` are the hosted versions of the same copy already shown in-app
(`feature/settings` strings: `settings_privacy_content` / `settings_terms_content`), needed because
Google Play Console requires a reachable Privacy Policy **URL** at submission time — in-app text
alone doesn't satisfy that.

## Why these aren't wired into the app yet

The app already has a deep link on `https://cestou.app` (see `app/src/main/AndroidManifest.xml`),
so that's the obvious home for these pages — but whether that domain is on GitHub Pages, another
host, or something else isn't something this session can see or decide. Wiring a URL into
`CustomMethodPickerTerms` and the Settings screen before confirming where these pages actually get
served would risk linking to a 404.

## To finish this

1. Pick a host: the simplest option is enabling **GitHub Pages** for this repo (Settings → Pages →
   deploy from a branch, `/docs` folder) — that would put these at
   `https://<username>.github.io/How-Much/legal/privacy.html`. Alternatively, if `cestou.app` is
   already pointed at something (Firebase Hosting, another static host), these files can be copied
   there under `/privacy` and `/terms` instead.
2. Once the URL is live, add it to:
   - `feature/auth`'s `CustomMethodPickerTerms` (the "you agree to our Terms/Privacy" text on the
     sign-in screen) — turn it into a clickable link, or add a link below it.
   - `feature/settings`'s `settings_item_terms` / `settings_item_privacy` rows — the Legal screen
     (`LegalContentScreen`) currently renders `settings_terms_content`/`settings_privacy_content`
     in-app; either keep that and additionally link out, or replace it with a link to the hosted
     page.
   - Google Play Console's "App content" → "Privacy policy" field.

The privacy page's copy already includes a line about the in-app account deletion feature added
this session (Settings > Data > Delete Account) — keep both copies (in-app and hosted) in sync if
either changes.
