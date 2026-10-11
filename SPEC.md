# androidzoom — specification

What this library promises its consumers, and why. Implementation detail lives
in the code and its comments; this file is the part a consumer can rely on
without reading the source.

## What it does

An app's own text can be sized by the user. The setting is a **multiplier on
top of the system font scale**, not a replacement for it: a user who enlarged
text in Android keeps that, and this says how much bigger or smaller the app is
than everything else. **Only text scales** -- paddings, icons and touch targets
keep their layout, because only the density's `fontScale` is overridden.

- **Range: 80% to 160%.** A stored value outside it is clamped; a non-finite
  one (a corrupt preference) falls back to the app's default rather than
  sizing text to infinity.
- **Default: 115%**, a notch above the system's own, which is where the first
  consumers landed. An app with dense screens passes its own default. Only the
  starting point is a default; a stored value is read back unchanged.
- **Continuous, not stepped.** A pinch lands wherever the fingers put it, so a
  size it leaves behind is one a continuous slider returns to exactly. An app
  that wants a stepped slider rounds in its own control.

### The pinch

A two-finger pinch anywhere under `ProvideFontSize` resizes the text live and
persists once, where the fingers stop.

- **A small gesture does nothing.** The fingers' *separation* must change by
  24dp, well above the platform's touch slop, before anything resizes, and the
  movement that crosses it is spent rather than applied. A two-finger scroll,
  whose separation does not change, never resizes however far it goes, and
  still reaches the list underneath: nothing is consumed until the gesture is a
  pinch.
- **Amplified, not 1:1.** The range is only 2x wide, so the zoom is squared: a
  comfortable one-hand spread covers most of it.
- **An overshoot is remembered, within bounds,** so spreading past an end and
  coming back lands where the fingers say, not at the clamp.
- **The switch is the app's.** `pinchEnabled` turns the gesture off without
  touching the size.

### Windows

A dialog, sheet or menu is a window of its own, and neither the scaled density
nor the gesture crosses into it. An app that wants the size there wraps the
window's content in `FontSizeWindow`, or `FontSizePinchWindow` for a surface
that should also take the pinch.

### Storage

The library stores nothing. The app hands `rememberFontSizeState` its current
stored value and the callbacks that write a new one, and each later stored
value back as it changes. A value arriving while the fingers are down is held
until they lift, so a warm read landing mid-pinch never snaps the text out from
under the user, and the gesture's own size is what gets written.

## Versioning

**Semantic versioning**, the contract `mikelward/androidlog` uses:

| Part | Moves when | A consumer must |
|---|---|---|
| **MAJOR** | the intended design moved, **or** a consumer must edit code — either alone is enough | read the release, then fix and bump together |
| **MINOR** | something is added and nothing existing changes meaning | nothing — the bump is safe to take |
| **PATCH** | neither of the above: a fix, a refactor, docs, tests | nothing |

**Ask the additive question first.** Is this purely something *added*, with
nothing existing changing meaning? Then it is MINOR. Otherwise it is PATCH only
if it is both consistent with the intended design above and needs no client
change. Moving the range, the default, the slop or the gain is a design change,
and MAJOR.

### How the number is produced

`MAJOR.MINOR` is declared, in `gradle/version-series.txt`. **Editing that file
is the release decision**, and it is the only one anyone makes by hand.

`PATCH` is derived, and it is the number of commits on the release line —
`git rev-list --count HEAD` — So it
never has to be tracked, and every commit that reaches `main` is publishable
exactly once with a number no later commit can reuse.

**That is one deliberate deviation from strict SemVer**, which asks for the
patch to reset to 0 when the minor moves. It does not reset here. A reset buys
tidiness; not resetting buys the property that actually matters for coordinates
that can never be withdrawn — no two releases in this repository's entire
history share a number, whatever the series has done in between. Ordering is
unaffected, since within a series the count only grows.

A version that cannot be derived with confidence is not guessed — and the refusal
comes in two tiers, which are told apart by what the version string says.

**The string itself reads `undetermined`** where the count cannot be trusted at
all: a shallow clone, a tree with no `.git` of its own, or a series file that is
missing, unreadable, or not `MAJOR.MINOR`. It is `<series>.0-undetermined`, or
`0.0.0-undetermined` in that last case, since there is then no series to name.

**A dirty checkout, or a commit that is not on `origin/main`, still derives the
ordinary `<series>.<count>`** and is refused only when a publish task runs. That
is deliberate rather than an oversight: the version is inert under
`includeBuild`, so an ordinary build, a test run, and a consumer's composite
build all keep working while someone is mid-edit or on a feature branch. Only
publishing is irreversible, so only publishing is guarded — and testing an
in-progress change against a consuming app is what `includeBuild` is for,
precisely so nobody has to publish to try something.

### What a consumer should expect

- **The weekly `gradle-update` batch moves the patch and minor level and
  auto-merges on green CI.** That is safe by construction: neither can break a
  build, and if one does, the version was wrong and that is a bug in this
  repository, not in the consumer.
- **A major never arrives that way.** It waits for someone to take it.
- **A deprecated symbol is kept for at least one minor release** before a major
  removes it, so a consumer meets a rename as a warning on a bump it was going
  to take anyway, rather than as a red build.
