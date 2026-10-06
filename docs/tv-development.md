# TV development: branches and verification history

This is an on-demand reference for work on TV branches, integration and existing
pull requests. It contains the feature records moved out of AGENTS.md on
2026-10-07. Paths in the records are relative to the repository root.

## Branch snapshot

Checked against GitHub on 2026-10-07. All eight PRs below were open. Refresh their
state before preparing or integrating a change; this table is not live status.

`tv-development` is the cumulative local development branch. It includes the
TV debug setup and the topic changes recorded below.

| PR | Topic branch | Remote head at verification |
| --- | --- | --- |
| [#304](https://github.com/anilibria/anilibria-app/pull/304) | `fix/tv-otp-auth` | `b2c7fc5c` |
| [#305](https://github.com/anilibria/anilibria-app/pull/305) | `fix/tv-player-letterbox` | `35b6dfd2` |
| [#306](https://github.com/anilibria/anilibria-app/pull/306) | `fix/tv-controls-and-description` | `97bf6320` |
| [#307](https://github.com/anilibria/anilibria-app/pull/307) | `fix/tv-default-quality` | `cc96941d` |
| [#308](https://github.com/anilibria/anilibria-app/pull/308) | `fix/tv-release-blocked-message` | `e6130387` |
| [#309](https://github.com/anilibria/anilibria-app/pull/309) | `fix/tv-player-buffer-memory` | `14e34f6c` |
| [#310](https://github.com/anilibria/anilibria-app/pull/310) | `fix/tv-skip-prompts-seeking` | `5fecc34b` |
| [#311](https://github.com/anilibria/anilibria-app/pull/311) | `feature/tv-poster-status` | `25d1c3c8` |

## Feature and device records

The records below preserve the previous AGENTS.md entries. Test counts, APKs,
device results and local integration statements describe earlier verification
stages; they are not a fresh build or device check. Different cumulative test
counts belong to different stages. Inspect current Git state and the referenced
reports when continuing a topic. Update this document as branches are merged or
their checkouts change.

### tv-development

- `tv-development` is the cumulative development branch. It includes the
  OTP authentication fix and the TV debug setup, which allows installing the
  debug app beside the official app. Continue local development and device
  testing with these changes present.

### Original player overlay integration

- It also integrates upstream PR #276 (player overlay and scrollable release
  descriptions). The previous rollback was undone at the user's request;
  original PR commits and subsequent layout/player corrections are preserved.
  Preserve these changes in subsequent device builds.

### fix/tv-controls-and-description

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
  A local follow-up uses `DescriptionScrollView` to draw only complete text
  lines at both scroll-window edges, including during scrolling. It is applied
  to `tv-development` and the PR branch checkout in
  `outputs/tv-controls-and-description`. Follow-up commit `97bf6320` is pushed
  to the user's existing fork in PR #306; its description is updated.
  Both TV builds pass and the cumulative 46 TV tests pass. The cumulative APK
  is installed and checked on "Takt Op. Destiny" and "Steel Ball Run", including
  the final description lines and focus transfer to actions. Evidence is in
  `outputs/tv-diagnostics/description-validation.md`.

### fix/tv-otp-auth

- `fix/tv-otp-auth` contains only the authentication fix and regression
  tests. Its upstream PR is https://github.com/anilibria/anilibria-app/pull/304
  (replacing closed PR #303 after renaming the source branch),
  targeting `anilibria/anilibria-app:develop`.

### fix/tv-player-letterbox

- `fix/tv-player-letterbox` contains the black player background fix, reproduced
  with "Suzume" and verified on the user's TV. Its upstream PR is
  https://github.com/anilibria/anilibria-app/pull/305. This fix is also included
  in `tv-development`.

### fix/tv-default-quality

- `fix/tv-default-quality` adds an Auto option showing the resolution selected
  from the physical display and available episode streams. All releases default
  to Auto; manual quality is stored separately per release and does not use the
  legacy global quality preference. Episodes missing that manual quality use
  Auto temporarily; selecting Auto clears the release's manual preference.
  It is included in `tv-development`. Its upstream PR is
  https://github.com/anilibria/anilibria-app/pull/307, targeting `develop`.

### fix/tv-release-blocked-message

- `fix/tv-release-blocked-message` shows the API block reason on TV release
  cards when `blockedInfo.blocked` is true, with a generic fallback if the
  reason is missing. Verified on "Oshi no Ko 3rd Season" and included in
  `tv-development`. Its upstream PR is
  https://github.com/anilibria/anilibria-app/pull/308, targeting `develop`.

### fix/tv-player-buffer-memory

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

### fix/tv-skip-prompts-seeking

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

### feature/tv-poster-status

- `feature/tv-poster-status` is an independent feature branch based on
  `upstream/develop` (`a188bfd5`), with its checkout in `outputs/tv-poster-status`.
  Its upstream PR is https://github.com/anilibria/anilibria-app/pull/311,
  targeting `develop`. Topic commit `25d1c3c8` is pushed to the user's existing
  fork; previous TV fixes and local debug setup are excluded. It makes blocked release posters
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
  The user authorized publication after local/device testing; PR #311 is OPEN.

### feature/tv-watched-progress

- Local independent branch from current upstream/develop a188bfd5, checkout
  outputs/tv-watched-progress, topic commits e870c2ff, bfaf75f7 and 7c1683b1. No pending PR dependency.
  The user requested preparation for a PR but explicitly withheld PR creation;
  the branch is not pushed and no PR exists.
- TV separates saved progress from isViewed. A valid positive ending start takes
  priority; the flag remains true from that start through the end. Otherwise 85%
  of duration counts, following the public website's TimecodeController.
  Natural completion also counts. Confirmed seeks count; Leanback previews do not.
  Rewinding before the threshold clears the flag. Opening a watched episode
  resets its seek and flag before preparing video; unfinished episodes resume.
- Continue resumes the latest unfinished episode, or advances from a watched
  episode to the next unviewed one, wrapping to an earlier gap if necessary.
  Its caption is "Продолжить (12:34)" for one available episode,
  "Продолжить (1 серия - 12:34)" for an unfinished episode in a multi-episode
  release, and "Продолжить (2 серия)" for an unstarted next episode.
  With one available episode, "Смотреть" is hidden while Continue or Restart
  is available, and returns immediately after history is cleared.
  Stopped-position descriptions use nonzero seek.
  Both TV episode menus show a check icon for isViewed; reactive updates preserve
  the selected row. Manual mark-all adds checks without inventing progress.
  When all available episodes are marked, the card shows "Заново", opening the
  first episode from zero. Other flags remain until those episodes are restarted.
  Quality changes preserve the current position and flag.
  Mobile retains its existing automatic flag behavior through setAccessSeek;
  TV uses the new setPlaybackProgress contract.
- Changes are also applied to tv-development. Both TV and mobile builds pass.
  Cumulative: 75 TV and 15 data tests. Independent: 29 TV and 8 data tests.
  Both TV builds/tests were repeated for the caption/action refinement;
  shared data and mobile code have not changed since their previous checks.
  The final cumulative APK is installed on Smart TV Pro.
- Takt Op. Destiny: all 12 marks show "Заново"; it opens episode 1 at zero.
  At 20:05 out of 23:41 the flag is false, at 20:19 true. A backward preview
  preserves true; confirming 19:36 clears it and Continue resumes at 19:36.
  Selecting watched episode 2 resets its saved 15:00 to zero. Natural completion
  of episode 2 marks it and opens watched episode 3 at zero with its flag cleared.
  Changing to 720p at 21:25 preserves progress and true. History was restored and
  compared after restart and final APK installation. Ending-start behavior is
  unit-tested; a real TV episode with valid ending markers was not checked.
- TCL also verified the caption refinement: Takt Op. Destiny resumes episode 1
  at 12:34 and advances from watched episode 1 to unstarted episode 2 at zero.
  Kashita Maryoku (release 10322, one available episode) shows the time-only
  Continue caption or Restart without Watch; clearing history restores Watch.
  Focus skips the hidden action, and visible buttons fit in one row.
  The pre-test history was restored and compared in full after restarting.
- Evidence: outputs/tv-diagnostics/watched-restart-validation.md and
  outputs/tv-diagnostics/continue-labels-validation.md;
  final APK: outputs/tv-diagnostics/AniLiberty-TV-continue-labels-debug.apk.
  Russian PR description draft: outputs/pr-descriptions/tv-watched-progress.md.

### fix/tv-release-actions-loading

- Independent local branch from upstream/develop a188bfd5, checkout
  outputs/tv-release-actions-loading, topic commit 6d20768d. No pending PR
  dependency; it excludes watched-progress changes and local debug setup.
  The branch is not pushed and no PR has been created.
- Short list metadata previously revealed actions before full episode data:
  Favorite appeared first, then moved when Watch/Continue was added.
  The action row now starts invisible and is revealed only after full details
  have been bound. Space is retained, and focus moves to the first visible action
  after the row appears. A full release with no available episodes still reveals
  Favorite; readiness does not depend on the playlist being nonempty.
- Applied to tv-development. Both TV APK builds and unit-test tasks pass;
  cumulative 75 TV tests, zero failures/errors. Upstream has no TV unit-test
  sources, so the independent branch's test task reports NO-SOURCE.
  This fix changes only app-tv; shared data/mobile have not changed.
- TCL recording reproduces the initial Favorite-only frame on Fantasies of the
  River Backwaters before the fix. The new cumulative APK shows the complete
  Watch/Favorite/Other row together on first load and reopening. Princess Knight
  shows Continue (00:02) without Watch; Takt Op. Destiny shows Continue (1 серия)
  with Watch. Focus is correct in each case and Right reaches Favorite.
  Blocked Oshi no Ko 3rd Season shows its reason and focuses Favorite after load.
- Latest installed cumulative APK:
  outputs/tv-diagnostics/AniLiberty-TV-release-actions-loading-debug.apk.
  Evidence: outputs/tv-diagnostics/release-actions-loading-validation.md;
  Russian PR draft: outputs/pr-descriptions/tv-release-actions-loading.md.
