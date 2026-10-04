# Local development workflow

This fork is being developed incrementally. Preserve completed work when adding
new fixes or features.

- Use branch names without a `codex/` prefix, as requested by the user.
- `tv-development` is the cumulative development branch. It includes the
  OTP authentication fix and the TV debug setup, which allows installing the
  debug app beside the official app. Continue local development and device
  testing with these changes present.
- Upstream PR #276 was merged and then reverted at the user's request because
  of problems observed during use. Its player overlay and scrollable release
  description changes are deferred; do not reintroduce them without a new
  user request.
- `fix/tv-otp-auth` contains only the authentication fix and regression
  tests. Its upstream PR is https://github.com/anilibria/anilibria-app/pull/304
  (replacing closed PR #303 after renaming the source branch),
  targeting `anilibria/anilibria-app:develop`.
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
