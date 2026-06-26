# TIL 2026-06-25 — ELDERBERRY

**Context:** Built `us-quest` (a ClojureScript + re-frame branching choose-your-own-adventure app) from scaffold through a public deploy, then added several demo adventures via the pmtools fleet workflow. Lessons below span issues #11, #14, #16–18, #21 and the initial milestone build.

---

## 1. Guardrails `>defn-` must be in the ns `:refer`, or you get a runtime arity error

**What happened:** During the domain core (TDD), tests failed at *load* time with
`TypeError: Cannot read properties of undefined (reading 'cljs$core$IFn$_invoke$arity$5')`.
The culprit: I used `(>defn- reachable-ids …)` but only `:refer`'d `[>defn >def => ?]`,
not `>defn-`. Because `>defn-` was unresolved, CLJS compiled it as a *function call to
`undefined`* with five args (name, docstring, argvec, gspec, body) — hence "arity 5".

**What I learned:** An unreferred macro doesn't necessarily fail compilation; it can sail
through and blow up at load as an undefined *invoke*. The arity in the error = the number
of forms you handed the "macro", which is a strong fingerprint for "this is a missing
refer," not a logic bug.

**The rule:** **When you use `>defn-` (or any guardrails macro), add it to the `:refer` vector — an "arity-N invoke of undefined" at load almost always means an unreferred macro.**

---

## 2. ClojureScript advanced compilation strips docstrings — verify the *artifact*, and grep precisely

**What happened:** Before making the repo public I had to scrub a partner's name. A
docstring in `samples.cljc` said "…for Trust." I panicked that `grep -ci trust public/js/main.js`
returned `1` in the release build. It turned out the *only* match was React's `Trusted`Types
— the docstring had been **elided by advanced compilation** and never shipped. Separately,
case-insensitive greps for personal terms false-matched on substrings: `acro` inside
`macro`/`across`, `avi` inside `beh`**`avi`**`oral` and `s`**`avi`**`ng`.

**What I learned:** Source ≠ shipped artifact. Comments/docstrings vanish under `:advanced`,
so a name in a docstring isn't a leak in the bundle — but you must check the *built* file to
know. And substring greps over minified JS produce confident false positives.

**The rule:** **Audit the compiled artifact, not just source; and grep for whole tokens (`grep -o -i 'trust[a-z]*' | sort | uniq -c`) so substring matches don't masquerade as leaks.**

---

## 3. A stateless branching engine encodes "state" structurally, not with variables

**What happened:** The engine is a pure tree (passage → choices → passage; no variables,
no RNG). For the dungeon-crawl demo (#18) I needed "did you grab the gold?" to change the
ending. With no state, I made a **separate passage** — `cavern-rich` — that the treasure
path routes through, while the no-treasure path goes to plain `cavern`. The two share the
dragon scene but lead to different endings.

**What I learned:** "Remembering" a fact in a stateless tree means *duplicating the
downstream node per fact value*. It's more passages, but it keeps the model pure and every
path explicit. This composes only so far (N independent flags → 2^N nodes), which is exactly
why it stays a demo and dice/HP/inventory were scoped out.

**The rule:** **In a stateless choose-your-own-adventure, encode remembered facts as distinct passages on the path, not as variables — and treat the node blow-up as the signal that you actually need an engine with state.**

---

## 4. Getting demo-content updates to *returning* users: stable ids + a seeded marker (#14)

**What happened:** New demos (and fixes) never reached existing users, because
`initial-db` only seeded when `localStorage` was empty, and demos had random ids each load
(no key to dedup against). The fix: give each built-in a **stable `:adventure/id`**, persist
a `seeded` set of ids, and on load **add only built-ins whose id isn't in `seeded`** — never
overwrite, never resurrect a deleted demo. Pre-existing libraries (random-id demos, no
marker) get a one-time migration that retires legacy auto-seeds **by title** and seeds the
stable ones, keeping user-authored adventures.

**What I learned:** "Reach returning users" is really an *identity + idempotency* problem.
Stable ids are the primitive; the seeded-set turns "add missing" into something that respects
deletes. The nasty case is the migration from a pre-identity world, where matching old data
needs a heuristic (title) with a documented edge (a user adventure named exactly like a demo
would be retired).

**The rule:** **Before you can sync managed content into user storage idempotently, give the content stable identity and track what's been seeded — "merge by id, skip if seen" beats "seed only when empty."**

---

## 5. Rewriting git history under an active fleet: minimal range + `--force-with-lease`

**What happened:** Making the repo public required purging the scrubbed name from history.
But a concurrent fleet agent was committing to `main` at the same time (the reflog showed
`checkout`/`pull`/`merge` I never ran, and a push landed mid-task). I limited blast radius by
rewriting only the contaminated range (`git filter-branch … -- 9345ae2..HEAD`, keeping earlier
SHAs intact) and pushing with `--force-with-lease` so a concurrent push couldn't be clobbered.

**What I learned:** On a shared branch with autonomous writers, a full-history rewrite is a
foot-gun; scope the rewrite to the fewest commits and let `--force-with-lease` be your
safety interlock. Also: `pmtools close` deletes the worktree that is your shell's cwd, so the
command ends with `getcwd: cannot access parent directories` **after** a successful close —
cosmetic, not a failure.

**The rule:** **Rewrite the shortest commit range that fixes the problem and always `--force-with-lease`; and don't read the post-`close` getcwd error as a failure — check the "CLOSED" banner instead.**

---

## 6. Two deploy realities: free-plan Pages needs a public repo; shadow dev-http won't serve `/`

**What happened:** `gh api … /pages` returned `422 "Your current plan does not support
GitHub Pages for this repository"` — free-tier Pages only serves **public** repos, which
forced an explicit privacy decision before publishing. And in dev, `http://localhost:8080/`
404s while `/index.html` works — shadow-cljs `dev-http` doesn't auto-resolve the directory
index.

**What I learned:** Hosting choices have privacy consequences you can't paper over: a public
repo under your account inherently exposes the account name (URL + commit author), so
"no mention of me" can only ever apply to *file content and history*, not the account. Worth
surfacing that tension to the human *before* flipping visibility, not after.

**The rule:** **Confirm the privacy/visibility trade-off (and that the host even supports your plan) before publishing; and reach the local app at `/index.html`, not `/`, under shadow dev-http.**

---

## What landed

| Issue | Change |
|---|---|
| #16 / #17 | Japanese demo: Aiko asks the player's name, hiragana chunked for beginners |
| #14 | Stable demo ids + seeded-merge so new/updated demos reach returning users |
| #18 | "Into the Cracked Crypt" dungeon-crawl demo (15 passages, 6 endings) |
| — | Public deploy to GitHub Pages after a history scrub |

## Open threads

- #9 — Playwright e2e smoke tests for the browser-only surfaces (fleet scaffolded `npm run e2e`).
- #15 — automate the Pages deploy (GitHub Action on push to `main`) so deploys stop being manual.
