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
  Pressing Up closes the player controls on key release when no new focus
  target exists above; navigation within the controls remains available.
- `fix/tv-otp-auth` contains only the authentication fix and regression
  tests. Its upstream PR is https://github.com/anilibria/anilibria-app/pull/304
  (replacing closed PR #303 after renaming the source branch),
  targeting `anilibria/anilibria-app:develop`.
- `fix/tv-player-letterbox` contains the black player background fix, reproduced
  with "Suzume" and verified on the user's TV. Its upstream PR is
  https://github.com/anilibria/anilibria-app/pull/305. This fix is also included
  in `tv-development`.
- `fix/tv-default-quality` adds an Auto option showing the resolution selected
  from the physical display and available episode streams. All releases default
  to Auto; manual quality is stored separately per release and does not use the
  legacy global quality preference. Episodes missing that manual quality use
  Auto temporarily; selecting Auto clears the release's manual preference.
  It is included in `tv-development`. Its upstream PR is
  https://github.com/anilibria/anilibria-app/pull/307, targeting `develop`.
- `fix/tv-release-blocked-message` shows the API block reason on TV release
  cards when `blockedInfo.blocked` is true, with a generic fallback if the
  reason is missing. Verified on "Oshi no Ko 3rd Season" and included in
  `tv-development`. Its upstream PR is
  https://github.com/anilibria/anilibria-app/pull/308, targeting `develop`.
- Write PR descriptions in Russian. State concrete reproduction examples and
  disclose OpenAI Codex assistance when preparing PRs for this project.
- `fix/tv-player-buffer-memory` is based on `upstream/develop`, with its checkout
  in `outputs/tv-player-buffer-memory`. Its upstream PR is
  https://github.com/anilibria/anilibria-app/pull/309, targeting `develop`.
  The same changes are applied and committed to `tv-development`.
  It gives the TV player a target buffer of
  one eighth of Runtime.maxMemory(), capped at 64 MiB (24 MiB on the tested
  192 MiB heap), and prioritizes bytes over the default duration target.
  A single HLS chunk can exceed the target, so it leaves heap headroom for
  large chunks and network buffers. The cumulative debug APK was verified on
  "Takt Op. Destiny", episode 11, across 21:56 and the large ending segments
  in 1080p without the previous OutOfMemoryError. Debug-only buffer telemetry
  records sizes and positions without request URLs or credentials.
  MPEG-TS HLS reads additionally wait for allocator space at twice the target
  budget, with reads capped at 64 KiB. Waits are interruptible on seek/release;
  paused playback may retain a full buffer. A stream unable to free samples
  while playing fails after 30 seconds instead of waiting indefinitely.
  This controls source reads, not every allocation in the extractor or HTTP.
  The enhanced build passed the same heavy section again after seeking back,
  completed episode 11 and resumed episode 12 in 1080p in the same process.
  The read gate actually waited, with an observed allocator maximum of 48 MiB.
  The cumulative build and 13 unit tests pass; the independent branch has its
  own JUnit dependency and 6 passing tests. Topic commit `14e34f6c` is pushed
  to the user's existing fork; earlier TV changes are excluded from this PR.
- `fix/tv-skip-prompts-seeking` is based on `upstream/develop`, with its
  checkout in `outputs/tv-skip-prompts-seeking`. Its upstream PR is
  https://github.com/anilibria/anilibria-app/pull/310, targeting `develop`.
  Latest topic commit `5fecc34b` is pushed to the user's existing fork.
  Skip prompts and their timers are suppressed during Leanback seeking.
  Confirming a position inside an opening/ending shows its prompt, including
  when returning to an already watched range; cancelling a preview preserves
  previous dismissals. The buttons use the original 48 dp end padding and 16 dp bottom
  padding, placing them below the playback times without overlapping them.
  The same changes are applied and committed to `tv-development`.
  The cumulative debug APK is installed on the TV. Its 35 tests pass,
  including 10 seek-state and 12 readiness/countdown tests; the independent branch's
  22 tests also pass.
  On episode 11 of Takt Op. Destiny, seeking to 02:46 preserves progress-bar
  focus during preview and shows the opening prompt after confirmation, with
  a visible gap before the time text. Ending state is covered by tests;
  the TV's legacy API returns empty ending markers for this release.
  Prompts wait for a rendered frame and READY after media changes and actual seeks.
  No-op seeks keep the existing frame. A 250 ms fade completes before the five-second
  countdown starts; pause, buffering and Watch focus freeze its remaining time.
  Skip gains focus before the fade and appears highlighted from its first visible
  frame; the animation end does not reclaim focus. Clicks wait for the fade to end.
  When auto-skip is enabled, the (5) label also appears from the first visible frame;
  only the countdown waits for the fade. See skip-timer-validation.md for TV evidence.
  Prompts now fade out over 250 ms, with countdown/actions blocked immediately and
  the current styling/text retained through the fade. A renewed prompt cancels
  the old fade and resumes from the current alpha; media changes remove it immediately.
  Both builds and the same 35/22 tests pass. The fade-out refinement is installed
  and device-validated on episode 12: Watch dismisses with a smooth fade and focus
  returns to the progress bar. A rapid seek confirmation cancels the previous fade
  and preserves the newly shown prompt with its paused (5) label.
  The new cumulative APK and report are under `outputs/tv-diagnostics/skip-hide*`
  and `outputs/tv-diagnostics/AniLiberty-TV-skip-hide-debug.apk`.
  This refinement was checked in a TV screen recording; see skip-focus-validation.md.
  Prompt appearance no longer forcibly hides controls. Play/Pause is routed from
  the prompt buttons, which are outside Leanback's grid. Jobs/animations are disposed
  with the view. Verified episode 12 from 00:00, repeated seeks to zero, and episode 11
  Watch focus while playing, pause, and countdown resumption. Debug-only safe events,
  APK and validation report are saved under `outputs/tv-diagnostics/skip-lifecycle*`.
- Keep each upstream PR limited to one fix or feature. Do not include the local
  debug setup or these workflow instructions in unrelated upstream PRs.
- `feature/tv-poster-status` is a local independent feature branch based on
  `upstream/develop` (`a188bfd5`), with its checkout in `outputs/tv-poster-status`.
  No PR has been created or branch pushed. It makes blocked release posters
  grayscale and shows the block reason instead of list metadata, including
  Continue Watching. Favorite releases show a translucent top-right star except
  in Main's favorite updates and Watching's Favorites rows. The release detail
  header poster stays unchanged. Lists and quick search now retain the API's
  blocked/favorite fields. Successful favorite actions update visible badges
  and the detail header's favorite button, including after reopening the card;
  per-account local actions take precedence over older API snapshots.
  The same feature is applied to `tv-development`. Both TV builds pass:
  cumulative 46 TV + 7 data tests, independent 11 TV + 2 data tests.
  The cumulative mobile debug build also passes. The final cumulative APK is
  installed on the TV; screenshots and testing evidence are in
  `outputs/tv-diagnostics/posters-validation.md`, with the APK at
  `outputs/tv-diagnostics/AniLiberty-TV-poster-status-debug.apk`.
  Initial device checks cover Main, Watching, the two badge exceptions,
  Continue Watching block reasons, Schedule, unchanged detail posters, and
  adding/removing favorites. The favorite button regression was reproduced on
  "Gensou Suikoden" and verified after the fix by adding/removing, navigating
  back and reopening twice. Four flow tests cover stale responses, reopening,
  and clearing local overrides. The installed corrected APK is also saved at
  `outputs/tv-diagnostics/AniLiberty-TV-favorite-card-debug.apk`.
  Await the user's testing before publishing a PR.
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
