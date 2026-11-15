# BioBuzz Auto Lab (source)

Live page: https://claude.ai/artifact/EsGvvBmmtKn4Ryt5jtRJxa

- `page.src.html`: the page, with two placeholders
- `autorun.js`: the Pedro-style follower and auto runner (also runs under Node)
- The physics (`SimCore`) is copied unchanged from `docs/BioBuzz Test Drive.html` (the script that starts "BioBuzz test drive: physics")

To rebuild `index.html`, replace `/*__SIMCORE__*/` with that script and `/*__AUTORUN__*/` with `autorun.js`.
