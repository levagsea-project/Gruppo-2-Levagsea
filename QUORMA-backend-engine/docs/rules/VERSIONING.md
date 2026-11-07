# Ruleset versioning

- **Status:** PROPOSED 2026-11-07. Not in `DECISIONS.md` yet: it becomes a decision when the team agrees.
- **Goal:** version 1.0 is the release. Until then anything may change, in rules and in code. After it, a released version never changes, and every stored match can be replayed with the engine that played it.

## 1. What "a version" is

A version of the game is three things that must always travel together:

| Part | Where | Identified by |
|---|---|---|
| Rulebook | `docs/rules/regolamento-*.md` | the ruleset version string |
| Rule pack | `data/*.json` (`vocab`, `keyword`, `evolutions`, `synergies`, `balance`, `invalid-combinations`) | **rule pack hash**: SHA-256 of the canonical re-serialization (`docs/plans/ruleset-loader.md` section 5) |
| Engine | `game-engine` code | **engine version** (the Maven version plus the git commit) |

A match already records all of them: `matches.ruleset_version`, `matches.ruleset_hash`, `matches.engine_version` (backend migration V6), and ruleset 12.2 requires the same. Nothing in the schema has to change for this policy.

The **ruleset version string** is for people (`1.0-alpha`, `1.0`, `1.1`). The **hash** is for machines: it proves which exact data a match used, even before 1.0 when the string does not change.

## 2. Phase A: development (now, until 1.0)

The version string stays **`1.0-alpha`**. It does not change with every edit.

- **Anything may change**, in the rulebook, `data/`, and the engine. There is no compatibility promise.
- **Every change is logged**, in the same pull request that makes it:
  1. one line in `docs/rules/CHANGELOG.md` under *Unreleased*: date, area (`rules`, `data`, `engine`), what changed, and the `DECISIONS.md` or `KNOWN-ISSUES.md` ID if there is one;
  2. an entry in `docs/DECISIONS.md` when it is a design decision (existing rule);
  3. the rulebook, `data/` and the engine tests updated together, so they never disagree (existing rule). A golden file changes only with a reason in the commit message.
- **Exact state** of any moment = git commit + rule pack hash. A match played in development can be replayed only with that same hash and engine commit. Development matches are disposable: when the rules change, old development replays may be deleted.
- **Optional milestone labels**, if the team wants them: `1.0-alpha` (now), `1.0-beta` (all MVP rules implemented and covered by golden files; balance still moves), `1.0-rc.1`, `1.0-rc.2`, ... (no changes except bug fixes). Git tags mark each one (`ruleset-1.0-beta`). Skip them if they add no value; `1.0-alpha` straight to `1.0` is allowed.
- **`sim.ruleset_version`** in `balance.json` records which version a simulation ran on. In development it says `1.0-alpha` for everything, so it means "tested during alpha", not "tested on this exact data". Use the hash when it matters.

## 3. The release of 1.0

Release is a checklist, done once, by one named person:

1. No open entry in `docs/KNOWN-ISSUES.md` that changes a rule or an outcome (resolve it, or write the chosen behaviour into the rulebook).
2. The rulebook, `data/` and the engine agree. `make validate` shows 0 errors. All engine tests pass.
3. Golden files exist for every rule the demo exercises, plus draws, turn 50, empty lanes and simultaneous deaths (`game-engine/AGENTS.md`).
4. Freeze the files:
   - `docs/rules/releases/regolamento-v1.0.md`: a copy of the rulebook with the version in its title and no "in sviluppo" note.
   - `data/releases/1.0/*.json`: a copy of the rule pack, plus a file `HASH` with the rule pack hash.
   - `game-engine/src/test/resources/golden/1.0/`: the golden files of this version.
5. Set the engine version (Maven `1.0.0`, no `-SNAPSHOT`) and tag the commit: `ruleset-v1.0` and `engine-v1.0.0`.
6. Move the *Unreleased* entries of `CHANGELOG.md` under a `1.0` heading with the release date.
7. In the backend, only released versions may be used to start a real match (section 5).

The working copy (`docs/rules/regolamento-*.md`, `data/*.json`) then becomes the **development head** of the next version.

## 4. Phase B: after the release

**A released version is immutable.** These never change after the tag:

- `docs/rules/releases/regolamento-v1.0.md`
- `data/releases/1.0/**` (a test compares its hash with `HASH`, so an edit fails the build)
- `game-engine/src/test/resources/golden/1.0/**`
- the behaviour of the engine when it plays version 1.0

**Any change that can alter the result of some match is a new version.** Never edit a released version to fix it.

| Change | New version | Example |
|---|---|---|
| Only numbers in `balance.json` (placeholders, rebalance) | minor: 1.0 → 1.1 | Pair multiplier 1100 → 1150 |
| A rule, the turn structure, hand ranking, vocabulary or a new effect or keyword | major: 1.x → 2.0 | A new hand type, a new phase |
| Wording only, no outcome changes (typo, clearer sentence) | none: an **erratum** line in `CHANGELOG.md` | "7.5: clarified that Ace is high" |

If you are not sure whether a change alters an outcome, it does: run the golden files of every released version. If one fails, the change needs a new version.

## 5. How the engine stays linked to a version (replayability)

1. **The engine loads a version by name.** `RulesetSource.load(String version)` (already in the loader plan) reads `data/releases/<version>/`, bundled into the jar. An unknown version is an error. The development head is only available as `1.0-alpha` (or the current label) in development builds.
2. **The engine refuses a replay if the hash differs.** A replay request carries `ruleset_version` and `ruleset_hash`. The engine recomputes the hash of the pack it loaded and fails on a mismatch, never "close enough".
3. **One engine can play many versions.** A behaviour that differs between versions is switched by the ruleset version inside the code, kept as small as possible and written as data when it can be (`AGENTS.md`: no hidden logic). Example: a hand ranking changed in 2.0 is a `ruleset.version()` check in one place, not a copy of the engine.
4. **CI replays every released version on every build.** The golden files of 1.0, 1.1, 2.0, ... run in the normal test run. A code change that breaks the replay of an old version fails the build. This is what enforces the freeze on the code side.
5. **Bug fixes after the release.** If the bug changes the result of version 1.0 matches or golden files, 1.0 keeps its old behaviour (the fix applies from the next version, behind the version check). A fix that changes no stored result and no golden file is a normal change. A bug that must be fixed in 1.0 anyway is a decision for `DECISIONS.md`, and affected stored matches are marked as played on a faulty version, not rewritten.
6. **Fallback if the version checks grow too heavy:** keep a tagged engine image per major version (`engine-service:1.x`, `engine-service:2.x`) and let the backend route a replay to the image of the match's `engine_version`. Not needed at 1.0.
7. **The backend** uses only released versions for real matches (a table or a configured list of allowed `ruleset_version` + `ruleset_hash` pairs) and stores the three identifiers it already has. A card pool snapshot is also pinned to a ruleset version (`pool_snapshots.ruleset_version`); a new ruleset version needs a new snapshot (see `2026-11-06-ruleset-change-illegal-deck` for the open problem with existing decks).

## 6. Naming and files

| Item | During development | After 1.0 |
|---|---|---|
| Rulebook file | `docs/rules/regolamento-v1.0-alpha.md` (as now) | working file renamed `regolamento.md`, released copies in `releases/` |
| Rule pack | `data/*.json` | `data/*.json` is the next version, `data/releases/<version>/` are frozen |
| Changelog | `docs/rules/CHANGELOG.md`, *Unreleased* | one heading per released version |
| Git tags | optional milestones | `ruleset-vX.Y`, `engine-vX.Y.Z` |

`docs/pdf/` and the PDF export next to the rulebook are for humans and are never a source (`docs/AGENTS.md`). A released rulebook may get a PDF export, which is a copy and does not count as a version.

## 7. Open points (need a decision before 1.0)

1. **Rulebook file name.** Renaming `regolamento-v1.0-alpha.md` breaks links in `docs/AGENTS.md`, `docs/plans/engine-handoff.md` and the PDF export. Proposal: rename at the 1.0 release, not before.
2. **Who releases 1.0** and who may approve a new version after it.
3. **Engine version numbers.** Proposal: the engine version changes on any engine release (patch for fixes, minor for a new supported ruleset version) and is independent of the ruleset version. Only the combination (ruleset version, hash, engine version) must be stored.
4. **Where `RulesetSource.load(version)` reads the released packs from** (jar resources, as proposed, or a mounted folder for the service). The loader owner decides; this policy only needs it to be addressable by version.
5. **How long old versions stay supported.** Proposal: forever for replay, since a replay of a stored match must always work.
