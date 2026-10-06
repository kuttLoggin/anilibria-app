# PR description guide

Use this guide when creating or rewriting a PR description for this repository.
Write in Russian. The user approved the structure of
[PR #309](https://github.com/anilibria/anilibria-app/pull/309) on 2026-10-07.

## Structure

Start with a short paragraph describing the concrete problem and resulting
behavior. Then use these headings in this order:

```markdown
<Кратко: проблема и поведение после изменения.>

## Воспроизведение

<Конкретный сценарий и поведение до/после изменения.>

## Изменение

<Итоговое поведение, существенные детали и ограничения.>

## Проверка

<Выполненные сборки, тесты, проверки на устройстве и границы проверки.>

<Fixes #... при наличии связанной задачи.>

Изменение подготовлено с помощью OpenAI Codex.
```

## Content rules

- For a feature, describe a concrete usage scenario in «Воспроизведение».
  Include release name, episode, timecode, device or API response when relevant.
- Describe the final implementation. Scale detail to the change; avoid a
  chronological changelog or conversational history.
- Place meaningful implementation limits and tradeoffs after the changes and
  before verification.
- Distinguish independent PR-branch builds/tests from checks on the cumulative
  `tv-development` APK. Include actual test counts and confirmed device scenarios.
- Explicitly retain checks that were not performed. Do not claim previous
  verification was rerun when only editing the description.
- Preserve issue-closing references, dependencies and original authorship where
  another PR's commits are included.
- End with a short OpenAI Codex disclosure.
