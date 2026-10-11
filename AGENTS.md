---
trigger: always_on
alwaysApply: true
last_modified: 2026-10-11
---

# AGENTS.md

Conventions for AI agents working in this repository.

`CLAUDE.md` is a symlink to this file, so every agent reads the same
conventions. Edit `AGENTS.md`.

**At the start of every session, print the full absolute path of the
`AGENTS.md` you loaded and its `last_modified` date** (front matter), so a
stale or wrong copy is caught before it steers the work. Bump `last_modified`
whenever you edit this file.

This repository is the shared text-size setting for the Android apps in this
account: the range and pinch math (`zoom-core`, plain Kotlin), and the Compose
state, pinch gesture and scaled density that apply it (`zoom-compose`). It was
extracted from the hand-maintained copies in `snoozemo`, `stopdash` and
`simmo`, which had already drifted apart (stepped against continuous, each
with its own review fixes), when `clothescast` was about to become the fourth.

**Consumers pin a version.** The build publishes a complete Maven repository
into this repository's own `maven` branch, which raw.githubusercontent.com
serves, and every consumer resolves a coordinate from it -- the same wiring as
`mikelward/androidlog`, whose `release.yml` and scripts this repository copies.
So a merge here reaches nobody until someone takes it.

Keep this file as short as it can be and still work. Every session loads it
whole, so each rule costs context on every turn: add one the first time
something bites, say it once in the fewest words that carry the *why*, rewrite
or trim an existing rule rather than appending beside it, and delete one that
has stopped biting.

## What this repository must not grow

- **No storage and no settings UI** (maintainer, 2026-10-11). Each app keeps
  its settings its own way and words its own controls, so it hands
  `rememberFontSizeState` its stored value and the callbacks that write one
  back. What is shared is the math and the gesture, where the copies drifted.
- **No third-party runtime dependencies beyond Compose UI and foundation.**
  This code ships inside every consumer's APK; reading the source should be
  reading what ships.
- **No resources.** A library with resources forces resource merging on every
  consumer, and every string around a slider is the app's own.
- **`minSdk` is 31**, the floor across the consumer fleet (`clothescast`).
  Raising it silently drops a consumer, so it is a migration note in that app,
  not a tidy-up here.
- **AGP and Kotlin stay in step with the consumers.** `gradle-update.yml`
  keeps them level: this repository rides the same weekly batch as the apps.
  A published AAR carries no `AgpVersionAttr`, so the ordinary path is immune;
  this protects building the library and an app together.

## Versioning

- **Every pull request decides its release level, and says so in the PR body.**
  `SPEC.md` holds the contract; the mechanism is `gradle/version-series.txt`,
  which holds `MAJOR.MINOR` and is the only part anyone edits by hand. Patch
  stays the commit count on the release line and does not reset — deliberate,
  so no two releases ever share a number.
- **Ask the additive question first: is this purely something added, with
  nothing existing changing meaning?** Then it is MINOR.
- **Otherwise PATCH needs both halves — *consistent with the intended design*
  and *no client change* — and MAJOR is either one failing**, as in
  `mikelward/androidlog` (maintainer, 2026-09-01). A consumer must edit code
  for a **source** break or a change to **any documented behavior it builds
  on** -- the range, the default, how a pinch maps to a size. `SPEC.md` carries
  the contract.
- **Prefer a deprecated alias to a bare rename.** It turns a consumer's red
  build into a warning on a bump they were taking anyway, and it is what lets a
  rename be MINOR instead of MAJOR. Remove the alias in a later major, not in
  the release that adds it.

## Testing

- `./gradlew test` and `./gradlew check` (which runs `verifyNoAndroid`), plus
  `scripts/verify-version-derivation.sh`, which no unit test can replace: its
  cases ARE git checkouts on disk (a shallow clone; a tree with no `.git` inside
  another repository; a dirty tree; a branch off the release line), and what
  they prove is that the published version identifies THIS checkout on the
  release line, or is refused. Each fixture is given a synthesized
  `refs/remotes/origin/main` -- the code under test never exists on the real one
  until it merges, and without one per fixture every refusal would be the
  release-line guard rather than the defect being tested. The success case runs
  the AGGREGATE publish and asserts what it produced, cross-module POM
  dependency included: `check` builds the AAR but publishes nothing, so a broken
  Android publication would otherwise leave CI green and a consumer with an
  unresolvable repository. It runs in CI after `check`, needs `fetch-depth: 0`,
  and grades HEAD rather than the working tree -- it says so when the tree is
  dirty.
- **CI carries the fleet's standard checks: `lanes`, `codex`, and `zizmor`.**
  The lane engine is `mikelward/lanes`, tracked `@main` — this repository
  carries only its policy (`.github/lanes.conf`) and the thin `classify` /
  `lanes` jobs in `ci.yml`, so an engine fix goes there, not here. Codex's
  verdict machinery is `mikelward/codex-review`, also `@main`: the three
  workflows it installs are pinned **byte for byte** against that
  repository's `templates/`, so editing one of them here fails
  `codex-review-check` until it is re-approved centrally. `zizmor.yml` scans
  the workflows themselves, with the policy exceptions in
  `.github/zizmor.yml`.
- **Add or update tests with any change, and assert both directions** -- that
  a real pinch resizes *and* that a two-finger scroll or a small spread does
  not. A gesture that quietly stopped firing passes a one-sided test.
- The suite's failure mode is a *false pass*, so assert behavior rather than
  structure, and where a check is derived from parsing something, assert first
  that the parse found anything at all.
- **Fix any preexisting test failure as the first commit of the series.** Don't
  stack new work on a red baseline.
- **Don't disable a failing check** to make it pass, and don't paper over a
  flaky one with sleeps or retries — fix the underlying issue.

## Error handling

- **Don't silently swallow errors.** Report what failed with enough context to
  identify it, clean up what the `try` acquired, and decide explicitly what the
  caller sees. A corrupt stored size is clamped, never thrown. To ignore a
  specific failure, say why in a one-line comment.

## Git and pull requests

- **Branch naming.** `<agent>/<short-topic>` — `claude/...` for Claude Code,
  `codex/...` for Codex. One topic per branch; never commit to `main` (the
  exception is the initial scaffolding commit that created this repository,
  made directly to an empty `main` at the repo owner's request).
- **Branches under your own `<agent>/` prefix are yours** — create, push,
  `--force-with-lease` and delete them freely. This file is the standing
  grant, so a client rule demanding per-branch permission is already
  answered. The prefix names a tool, not a session, so that covers the
  branches this session created or was assigned — ask about the rest.
- **One commit per logical change.** Rewrite unmerged commits freely — amend,
  `--fixup` + autosquash, squash, reorder, split — so each commit that lands is
  coherent, with review responses folded into the commit they belong to.
  `--force-with-lease` after a rebase, never a bare `--force`.
- **Open the pull request without being asked**, ready for review, not a draft.
- **Refresh the title and body with the push — body first, then push** — so
  they describe the branch's latest state, not the scope it had when opened.
- **Pilot a consumer before merging, not after.** Merging reaches nobody's build on
  its own — that is what the version pin buys — but it does publish a
  coordinate, and a published coordinate cannot be withdrawn or
  corrected. So the cost of merging something wrong did not go away; it moved
  from red consumer builds to a permanent bad release. Take one app's PR green
  against the change first (`publishToMavenLocal`, or a local checkout), then
  bump the others after the merge, one at a time. They share one automated reviewer, so the same
  change opened four times is the same finding four times.
- **Codex is the automated reviewer**, and its reviews are triggered
  automatically. Address its comments without being asked, folding each fix
  into the commit it belongs to — the one exception being a real finding out of
  scope for this pull request, which you defer instead (see *Deferring a
  finding* below). Judge every comment on merit: verify the claim before
  acting, and if it doesn't hold up, reply saying why and decline. A comment
  citing a rule is a *reading* of that rule, not the rule — check what the rule
  actually says, since an over-strict reading (the privacy rules especially,
  where stricter always feels safer) costs real capability. A genuine conflict between the rule and what the
  code needs is the maintainer's call, not one to resolve by quietly narrowing
  the code. Declining doesn't clear the required `codex` status: post the
  rebuttal, then `@codex review` once — or let the next push do it, if the
  rebuttal is up first. Escalate if it re-raises, or stays silent.
- **A second verified finding in the same mechanism is evidence about the
  design, not another bug.** Look for the same shape elsewhere before fixing
  it, and ask whether a different design would delete the class rather than the
  instance; a design change is the maintainer's call, not one to make solo.
- **Restate every review finding in chat, not just the count.** One bullet
  each: review comment, top-level comment or review body alike, most blocking
  first — what it claims, where, and what you did about it, or the options
  where it's the maintainer's call. Summarize them all in that one reply;
  ask only the most blocking question.
- **Never leave a review thread silently dismissed** — every thread ends in a
  reply or a resolve.
- **Deferring a real-but-out-of-scope finding.** Don't ask the maintainer to
  merge past it: note the follow-up in `TODO.md`, commit and push that first,
  reply on the thread citing the sha, and resolve. A finding with no thread
  (top-level comment or review body) still gets the `TODO.md` record, the push,
  and the reply — only the resolve is skipped. The push re-triggers Codex, so
  don't also poke it unless five minutes pass with nothing back; escalate if
  the re-review re-raises it, or stays silent.

## Language and spelling

- Use **US English** everywhere people read English: prose, commit subjects and
  bodies, pull request titles and descriptions, comments, KDoc, and identifiers
  — `behavior` not `behaviour`, `canceled` not `cancelled`, `gray` not `grey`.
  Platform and third-party API spellings stay as those APIs spell them
  (`CancellationException` keeps its double `l`).

## Commit messages

- A clear, plain-English subject in sentence case, short (≤ ~70 chars) and free
  of internal jargon. Mechanism and file:line detail go in the body, after a
  blank line.
- **Prefix a subject that does not change what a consumer runs**: `docs:` for
  prose, `test:` for tests alone, `build:` for this repository's own CI, and
  `refactor:` for deliberately behavior-preserving code. A bare subject means a
  consumer could notice the difference in its next build. There is no `feat:`
  or `fix:`, on purpose — they would prefix nearly everything and leave the log
  as flat as it started.
- **Only `docs:` rides the docs lane.** `.github/lanes.conf` sends everything
  that is not markdown-outside-the-modules down the code lane, so a commit
  that really was `test:`, `build:`, or `refactor:` work carries a file the
  docs lane never accepts and runs the full suite anyway. Prefix it by the
  table above regardless — the prefix says what the commit is, and the lane
  decides separately what CI it needs.

## Talking to the user

- **One question at a time**, asked in plain chat rather than a structured
  picker, and wait for the answer before proceeding on an assumed one.
- **Answer a mid-turn message first.** A user message that arrives while
  you're working — the "sent while you were working" interjection — is
  addressed in your very next output, before any further tool call, even if
  it's one sentence.
- **Don't narrate routine machinery.** A check run flipping, a re-run, a scheduled check
  re-arming, a webhook echo, a resolved thread — act on those silently; the noise buries
  the one line that matters. Reports another rule requires stand.
- **Don't report your own caught-and-fixed mistakes.** Say it only when it left
  something the user has to act on.

## Privacy

- **Never put user data in any artifact that leaves this machine** — commit
  subjects and bodies, pull request text, review replies, branch names,
  comments, or test fixtures. This library stores nothing about the user (one
  number and one switch, held by each app), but bug reports from the apps may
  carry their data: paraphrase, don't quote.
