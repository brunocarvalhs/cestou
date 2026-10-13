#!/usr/bin/env python3
"""Regenerate docs/changelog.html from the repo-root CHANGELOG.md (stdlib only).

Usage (from anywhere):
    python3 docs/scripts/build-changelog.py          # writes docs/changelog.html
    python3 docs/scripts/build-changelog.py --test   # runs the parser self-check

CHANGELOG.md mixes two formats: semantic-release bullets ("* feat(scope): msg") and
hand-written Keep-a-Changelog sections ("### Added" + "- **feat**: msg"). Only what a user
would notice is kept: feat/fix bullets, plus every bullet under "### Added"/"### Fixed".
Merges, chore/ci/test/refactor/docs/build and internal scopes are dropped.
"""
import html
import re
import sys
from pathlib import Path

DOCS = Path(__file__).resolve().parent.parent
SOURCE = DOCS.parent / "CHANGELOG.md"
OUT = DOCS / "changelog.html"

KEEP_TYPES = {"feat": "new", "fix": "fix"}
KEEP_SECTIONS = {"added": "new", "fixed": "fix"}
# ponytail: keyword/scope denylist is a heuristic for internal commits; extend it when one slips through.
NOISE_SCOPES = {"ci", "release", "build", "deps", "test", "docs", "sdd", "g10", "detekt", "github",
                "store", "core", "core-domain", "core-ui", "core-data", "billing", "remote-config", "pm"}
NOISE_WORDS = re.compile(r"detekt|pipeline|esteira|regressivo|lint|kover|coverage|baseline|test|"
                         r"usecase|refatora|google-service|ofusca|keystore|cobertura|dangerfile|"
                         r"gradle|\bbuild\b|imports?\b|magic number|hilt|dead |version|release|"
                         r"preview|\.md\b|room|savedstatehandle|navcontroller|appfuctions|rotas?\b|"
                         r"remote config|modulariz|SDKs?\b|StorageService|assetlinks|retry|vers[aã]o \d", re.I)
# Internal ticket refs at the end of a message: (AD-012), (PR5), (G16), (#67)
REF_RE = re.compile(r"\s*\((?:AD-\d+|PR\d+|G\d+|#\d+)\)")
RELEASE_RE = re.compile(r"^##\s+\[([^\]]+)\](?:\s*-\s*(\S+))?")
CONV_RE = re.compile(r"^(\w+)(?:\(([^)]*)\))?!?:\s*(.+)$")


def classify(text, section):
    """Return ("new"|"fix", message) for a user-facing bullet, else None."""
    plain = text.replace("**", "").strip()
    m = CONV_RE.match(plain)
    if m and m.group(1).lower() in KEEP_TYPES:
        kind, scope, msg = KEEP_TYPES[m.group(1).lower()], (m.group(2) or "").lower(), m.group(3)
    elif section in KEEP_SECTIONS:
        # "- **shopping**: msg" under "### Fixed": the bold word is a scope, not a type
        kind, scope = KEEP_SECTIONS[section], (m.group(2) or m.group(1)).lower() if m else ""
        msg = m.group(3) if m else plain
    else:
        return None
    if scope in NOISE_SCOPES or NOISE_WORDS.search(msg) or msg.lower().startswith(("merge", "revert")):
        return None
    return kind, REF_RE.sub("", msg)


def parse(markdown):
    releases, section = [], ""
    for line in markdown.splitlines():
        rel = RELEASE_RE.match(line)
        if rel:
            releases.append({"version": rel.group(1), "date": rel.group(2) or "", "bullets": []})
            section = ""
        elif not releases:
            continue
        elif line.startswith("### "):
            section = line[4:].strip().lower()
        elif re.match(r"^[*-]\s+", line):
            releases[-1]["bullets"].append([re.sub(r"^[*-]\s+", "", line).strip(), section])
        elif line.startswith("  ") and line.strip() and releases[-1]["bullets"]:
            releases[-1]["bullets"][-1][0] += " " + line.strip()  # wrapped bullet
    for r in releases:
        r["items"] = []
        for text, sec in r.pop("bullets"):
            hit = classify(text, sec)
            if hit and hit not in r["items"]:
                r["items"].append(hit)
    return releases


def inline(msg):
    msg = html.escape(msg[:1].upper() + msg[1:], quote=False)
    return re.sub(r"`([^`]+)`", r"<code>\1</code>", msg)


def render(releases):
    parts = []
    for r in releases:
        news = [m for k, m in r["items"] if k == "new"]
        fixes = [m for k, m in r["items"] if k == "fix"]
        date = f' <time datetime="{r["date"]}">{r["date"]}</time>' if r["date"] else ""
        title = "Sem versão" if r["version"].lower() == "unreleased" else f'Versão {html.escape(r["version"])}'
        body = ""
        for label, items in (("Novidades", news), ("Correções", fixes)):
            if items:
                body += f"<h3>{label}</h3>\n<ul>\n" + "".join(f"<li>{inline(m)}</li>\n" for m in items) + "</ul>\n"
        if not body:
            body = "<p>Só melhorias internas nesta versão.</p>\n"
        parts.append(f'<article class="release" aria-labelledby="v-{len(parts)}">\n'
                     f'<h2 id="v-{len(parts)}">{title}{date}</h2>\n{body}</article>\n')
    return TEMPLATE.replace("{{RELEASES}}", "".join(parts))


TEMPLATE = """<!doctype html>
<html lang="pt-BR">
<head>
<meta charset="utf-8">
<meta name="viewport" content="width=device-width, initial-scale=1">
<title>Novidades do Cestou — changelog</title>
<meta name="description" content="O que mudou em cada versão do Cestou, o app de lista de compras compartilhada.">
<meta name="theme-color" content="#16A34A">
<link rel="icon" type="image/png" href="assets/icon.png">
<link rel="stylesheet" href="assets/site.css">
</head>
<body>
<!-- Gerado por docs/scripts/build-changelog.py a partir de CHANGELOG.md. Não edite à mão. -->
<a class="skip" href="#conteudo">Pular para o conteúdo</a>
<header class="site-header">
  <div class="wrap">
    <a class="brand" href="./"><img src="assets/icon.png" alt="" width="36" height="36">Cestou</a>
    <nav aria-label="Principal">
      <ul>
        <li><a href="./">Início</a></li>
        <li><a href="./#documentos">Documentos</a></li>
      </ul>
    </nav>
  </div>
</header>
<main id="conteudo">
  <section>
    <div class="wrap">
      <h1>Novidades</h1>
      <p class="section-intro">Novos recursos e correções de cada versão do app. As descrições vêm do histórico do projeto, por isso algumas estão em inglês.</p>
{{RELEASES}}    </div>
  </section>
</main>
<footer class="site-footer">
  <div class="wrap">
    <nav aria-label="Rodapé">
      <ul>
        <li><a href="legal/privacy.html">Política de Privacidade</a></li>
        <li><a href="legal/terms.html">Termos de Uso</a></li>
        <li><a href="legal/delete-account.html">Excluir conta</a></li>
        <li><a href="changelog.html">Changelog</a></li>
      </ul>
    </nav>
    <p>Cestou · Android e Wear OS</p>
  </div>
</footer>
</body>
</html>
"""


def _selftest():
    sample = """# Changelog
## [2.0.0] - 2026-01-02
* Merge pull request #1 from x
* feat(cart): add `thing`
* fix(ci): internal
* chore: noise
* feat(cart): add `thing`
## [Unreleased] - 2026-01-01
intro text
### Added
- **feat(wear)**: watch
### Fixed
- **shopping**: debounced scans;
  second line
### Changed
- **auth**: dropped
## [1.0.0]
* ci: only internal
"""
    r = parse(sample)
    assert [x["version"] for x in r] == ["2.0.0", "Unreleased", "1.0.0"], r
    assert r[0]["items"] == [("new", "add `thing`")], r[0]
    assert r[1]["items"] == [("new", "watch"), ("fix", "debounced scans; second line")], r[1]
    assert r[2]["items"] == [] and "Só melhorias internas" in render(r[2:])
    assert "<code>thing</code>" in render(r)
    print("ok")


if __name__ == "__main__":
    if "--test" in sys.argv:
        _selftest()
    else:
        releases = parse(SOURCE.read_text(encoding="utf-8"))
        OUT.write_text(render(releases), encoding="utf-8")
        print(f"wrote {OUT.relative_to(DOCS.parent)}: {len(releases)} versions, "
              f"{sum(len(r['items']) for r in releases)} entries")
