# TODO

- **Ruleset.** Require `lanes`, `codex` and `zizmor` on `main`, as the siblings do
  (`repo setup` in mikelward/repo).
- **Adopt in each app, one at a time.** clothescast first (new), then snoozemo,
  stopdash, and simmo last (billed CI). Each migration deletes the app's own
  copy of the math and the gesture and keeps its storage and Settings controls.
- **Simmo's slider is stepped (5%).** Keep the rounding in simmo's own control;
  the library stays continuous (SPEC *What it does*).
- **Stopdash serializes its writes** through a queue so two quick changes land
  in order. That is storage, so it stays in stopdash; the library's callbacks
  fire in the order the user acted.
