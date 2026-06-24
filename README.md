# Adventures in Us 💛

A tiny, offline-first **branching choose-your-own-adventure** builder and player —
write a little interactive quiz/story, play through its branches one passage at a
time, save it locally, and share it as a single tappable link (no server, no
account).

**Live demo:** https://avidrucker.github.io/us-quest/

> A ClojureScript learning project exploring re-frame architecture with
> domain-driven design, BDD scenarios, and red-green TDD.

## What it does

- **Play** — read a passage, pick a choice (A/B/C/D-style), and the next passage
  appends below with a gentle scroll, until you reach an ending.
- **Author** — a form-based editor: add passages (text + optional emoji/image),
  add choices that branch to other passages, set the start, with live validation
  and an in-place preview.
- **Save** — your library persists in the browser via `localStorage`.
- **Share** — encode an entire adventure into a URL; opening the link imports and
  plays it. No backend involved.

## Architecture

Three clean layers, each independently testable:

| Layer | Namespace(s) | Responsibility |
|-------|--------------|----------------|
| Domain (pure) | `adventure.domain` (CLJC) | The adventure data model + pure functions (build, traverse, validate). No re-frame, no DOM. |
| State | `adventure.events` / `subs` / `db` / `fx` | re-frame app-db, pure event handlers, derived subscriptions, and isolated effects (localStorage, share links). |
| View | `adventure.views` (Reagent) | Library, player, and editor components, routed by `:route`. |

The adventure **is data** — a flat map of passages keyed by id — which makes
authoring, saving, and sharing all just "move some immutable data around."
Function contracts are enforced with [malli](https://github.com/metosin/malli) +
[guardrails](https://github.com/fulcrologic/guardrails).

## Develop

```bash
npm install
npm run dev      # shadow-cljs watch → http://localhost:8080/index.html
npm test         # compile + run the cljs.test suite (node)
npm run e2e      # Playwright browser smoke tests (boots the dev build itself)
npm run release  # optimized build into public/js
```

`npm run e2e` uses the system Chrome (`channel: 'chrome'`); its `webServer`
config starts `npm run dev` automatically, so no separate server is needed.

## Tech

ClojureScript · [shadow-cljs](https://github.com/thheller/shadow-cljs) ·
[Reagent](https://reagent-project.github.io/) ·
[re-frame](https://day8.github.io/re-frame/) · malli · guardrails · cljs.test
