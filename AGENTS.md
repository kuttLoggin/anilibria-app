# Local development workflow

This fork is being developed incrementally. Preserve completed work when adding
new fixes or features.

- Use branch names without a `codex/` prefix, as requested by the user.
- `tv-development` is the cumulative development branch. It includes the
  OTP authentication fix and the TV debug setup, which allows installing the
  debug app beside the official app. Continue local development and device
  testing with these changes present.
- It also integrates upstream PR #276 (player overlay and scrollable release
  descriptions). The previous rollback was undone at the user's request;
  original PR commits and subsequent layout/player corrections are preserved.
  Preserve these changes in subsequent device builds.
- `fix/tv-controls-and-description` merges original PR #276 into current
  `upstream/develop`, then fixes controls auto-hide, skip timer lifecycle,
  description scrolling and single-line marquee for overflowing release
  metadata (short metadata stays static). It addresses
  issues #39 and #96. Its upstream PR is
  https://github.com/anilibria/anilibria-app/pull/306, targeting `develop`.
  It also reserves space for description scrollbars and moves focus down
  to the first visible action after reaching the end of the description.
- `fix/tv-otp-auth` contains only the authentication fix and regression
  tests. Its upstream PR is https://github.com/anilibria/anilibria-app/pull/304
  (replacing closed PR #303 after renaming the source branch),
  targeting `anilibria/anilibria-app:develop`.
- `fix/tv-player-letterbox` contains the black player background fix, reproduced
  with "Suzume" and verified on the user's TV. Its upstream PR is
  https://github.com/anilibria/anilibria-app/pull/305. This fix is also included
  in `tv-development`.
- Write PR descriptions in Russian. State concrete reproduction examples and
  disclose OpenAI Codex assistance when preparing PRs for this project.
- Keep each upstream PR limited to one fix or feature. Do not include the local
  debug setup or these workflow instructions in unrelated upstream PRs.
- Before starting another change, inspect Git status, the current branch and
  the status of preceding PRs. Do not reset the cumulative branch to upstream
  or drop preceding changes merely to prepare a clean PR.
- For an independent change, prepare its PR branch from current
  `upstream/develop`, carrying only that change's commits. If it depends on a
  pending PR, preserve the dependency and clearly identify it; do not silently
  combine earlier work into an unrelated PR.
- Integrate completed topic changes into `tv-development` and build the
  TV test APK from the cumulative branch. Update this file as branches and PRs
  evolve, especially after upstream merges.
