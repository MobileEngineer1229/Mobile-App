# Repository Guide

This repository is a workspace of independent mobile and AI projects, not one
application. Keep every change scoped to the target project directory; never
run a recursive refactor, formatter, build, or deletion from the repository
root.

## Project map

- `TalentBaby/`: Android Java app, Express/TypeScript/PostgreSQL backend, and
  Next.js admin app.
- `IoT/`: Android Java smart-home app and Express/TypeScript backend.
- `foodvisor/`: nutrition app with Express/TypeScript/Mongoose backend,
  Next.js admin app, and mobile client.
- `HeightIncrease/`: Android Java app and Express/MongoDB backend.
- `AI-assistant*/` and `Translator/`: Python ML and offline-assistant projects.
- Screenshot, article, video, and reference folders are source material; do
  not modify them unless the task explicitly targets them.

## Before changing a project

1. Read its `AGENTS.md`, `CLAUDE.md`, and `README.md` when present.
2. Inspect the project-specific package, build, and test configuration.
3. Make the smallest scoped change and run the relevant project-local check.

## Shared conventions

- Preserve each backend's configured port and environment-variable contract.
- Keep Android user-facing strings in resource files; do not hard-code UI text.
- Treat language corpora, generated model data, and translation dictionaries as
  functional data, not duplicate documentation.
- Do not delete a localized file solely because a similarly named English file
  exists. Confirm it is an equivalent document or feature first.
- Do not edit generated output, dependency directories, screenshots, or binary
  artifacts unless specifically requested.

## Verification

Run checks from the affected project directory. Typical commands include
`npm run lint`, `npm run build`, Android `./gradlew assembleDebug`, Flutter
`flutter analyze`, or the Python project's documented validation command.

## Working tree safety

The workspace may contain unrelated local edits. Preserve them and report
anything pre-existing that is relevant to the requested work.
