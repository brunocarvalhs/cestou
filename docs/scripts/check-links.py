#!/usr/bin/env python3
"""Check that every internal href/src in docs/**/*.html points to an existing file (and #id).

Usage: python3 docs/scripts/check-links.py   — exits 1 and lists broken links if any.
External (http/https/mailto/intent/javascript) links are not checked.
"""
import sys
from html.parser import HTMLParser
from pathlib import Path
from urllib.parse import unquote, urlsplit

DOCS = Path(__file__).resolve().parent.parent


class Collector(HTMLParser):
    def __init__(self):
        super().__init__()
        self.links, self.ids = [], set()

    def handle_starttag(self, tag, attrs):
        for name, value in attrs:
            if name == "id" and value:
                self.ids.add(value)
            if name in ("href", "src") and value:
                self.links.append(value)


def parse(path, cache={}):
    if path not in cache:
        c = Collector()
        c.feed(path.read_text(encoding="utf-8"))
        cache[path] = c
    return cache[path]


def main():
    broken, checked = [], 0
    for page in sorted(DOCS.rglob("*.html")):
        for link in parse(page).links:
            u = urlsplit(link)
            if u.scheme or link.startswith("//"):
                continue
            checked += 1
            target = (page.parent / unquote(u.path)).resolve() if u.path else page
            if target.is_dir():
                target = target / "index.html"
            if not target.exists():
                broken.append(f"{page.relative_to(DOCS)}: {link} (missing file)")
            elif u.fragment and target.suffix == ".html" and u.fragment not in parse(target).ids:
                broken.append(f"{page.relative_to(DOCS)}: {link} (missing #{u.fragment})")
    print(f"{checked} internal links checked, {len(broken)} broken")
    print("\n".join(broken))
    return 1 if broken else 0


if __name__ == "__main__":
    sys.exit(main())
