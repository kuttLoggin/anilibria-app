# Local development workflow

This fork is developed incrementally. Preserve completed work and existing
local changes.

## Branches and integration

- Use branch names without a `codex/` prefix.
- `tv-development` is the cumulative development branch. Preserve its existing
  TV changes and debug setup, which allows installation beside the official app.
- Before starting a change, inspect Git status and the current branch. When
  working with topic branches or integration, read
  [TV development records](docs/tv-development.md) and check the relevant PRs
  live. Do not reset the cumulative branch to upstream to prepare a clean PR.
- Prepare independent topic branches from current `upstream/develop`, carrying
  only the topic's changes. Keep each upstream PR limited to one fix or feature.
  Preserve and explicitly identify dependencies on pending PRs.
- Integrate completed topic changes into `tv-development`; build the TV test APK
  from this cumulative branch.
- Keep local debug setup and workflow documents out of unrelated upstream PRs.
- Record branch/PR changes, commits and verification evidence in
  [TV development records](docs/tv-development.md), especially after upstream
  merges. Keep AGENTS.md focused on standing instructions.

## Verification

- Run the builds and tests relevant to the change. For TV changes, use
  `:app-tv:assembleAppDebug :app-tv:testAppDebugUnitTest --offline`.
- For shared `data` changes, also run `:data:testDebugUnitTest` and build mobile
  with `:app-mobile:assembleAppDebug`.
- Validate TV UI and player behavior on the real device, using screenshots or
  recordings where needed. Distinguish build/test results from device checks;
  record limitations if a device check cannot be completed.
- Keep verification reports and APKs under `outputs/tv-diagnostics/`; link
  relevant evidence from the TV development records. Historical test counts and
  old APK checks are not proof of the current build.

## PR descriptions

Write descriptions in Russian, include concrete reproduction or usage examples,
and disclose OpenAI Codex assistance. Follow
[the PR description guide](docs/pr-descriptions.md): short introduction →
«Воспроизведение» → «Изменение» → «Проверка».
