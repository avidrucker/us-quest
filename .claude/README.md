# `.claude/` — fleet orchestration

`orchestrate.json` configures the `fruit-agent-orchestrate` (pmtools) workflow
for this repo. It is plain JSON (no comments), so this file documents how it is
wired.

## How it's wired

- **host: github** — work items are GitHub issues on `avidrucker/us-quest`.
- **mode: fleet** — work is split across the named agents in `roster`
  (APPLE … FIG … HONEYDEW); each gets per-issue assignments.
- **worktreeBranchPattern / defaultBase / paths.worktreeDir** — each agent works
  an issue in its own git worktree under `.claude/worktrees/` on a branch named
  `<agent>/issue-<n>` (e.g. `fig/issue-3`), based off `origin/main`.
- **pmtools.port: js** — uses the Node port of pmtools. `pmtools` is expected on
  PATH (installed via `~/code/pmtools/install.sh`), so `home` is `null` and the
  enrichment commands carry no hardcoded paths.
- **enrichment** — `status` / `claim` / `preflight` / `close` dispatch straight
  to the `pmtools` CLI.
- **storage** — error and velocity tracking are both **disabled** for this repo
  (no SQLite store, no CSV mirrors).

## Usage

From the repo root, with `pmtools` on PATH:

```bash
pmtools status --json          # what's open / claimable
pmtools claim <n> --as fig     # claim issue n into a worktree
pmtools preflight <n>          # pre-work checks
pmtools close <n>              # from inside the worktree, after committing `Closes #n`
```

Run `/fruit-agent-orchestrate` to triage open issues and produce per-agent
assignments from this config.

> Reference configs: `JavaScript/lccjs` (npm-shim + storage) and `Python/pycats`
> (worktree fleet) in the same Study tree. This repo follows the pycats template.
