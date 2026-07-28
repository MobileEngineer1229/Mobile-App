# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## What This Repository Is

A multi-project workspace, not a single codebase. Each top-level directory is an independent app project with its own stack, docs, and (often) its own `CLAUDE.md`. The numbered markdown files at the root (`01-baby-grow-app.md` … `21-ai-old-friends-relatives-finder.md`, indexed by `todo.md`) are product specs for planned/ongoing apps.

**Scope all work to one project directory.** Sibling folders are unrelated — never let a search, refactor, or build touch another project. Recursive searches from the root time out (many `node_modules/`, `build/`, and large asset folders); always pass the project subdirectory as the search path.

## Code Projects

Each has its own docs — **read the project's own `CLAUDE.md`/`README.md` before working in it**:

| Directory | What it is | Stack | Own docs |
|---|---|---|---|
| `TalentBaby/` | Baby growth & talent development app | Android Java (`mobile/`) + Express/TS/PostgreSQL (`backend/`, port 3004) + Next.js admin (`web-admin/`) | `TalentBaby/CLAUDE.md`, `web-admin/CLAUDE.md` |
| `IoT/` | "Smartify" smart home system (MQTT/EMQX, BLE) | Android Java (`Smart-Home/`) + Express/TS/PostgreSQL (`Smart-Home-Backend/`, port 3003) | `IoT/CLAUDE.md`, `Smart-Home/CLAUDE.md` |
| `foodvisor/` | Food/nutrition tracking app | Express/TS/Mongoose (`backend/`, port 4000) + Next.js 15 admin (`web-admin/`) + `mobile/` | `foodvisor/CLAUDE.md` |
| `HeightIncrease/` | Height increase / workout app | Android Java (`mobile/`) + Express/MongoDB (`backend/`) | READMEs in each subdir |
| `AI-assistant/` | GPT-style LLM trained from scratch on DPRK Korean corpus | Python/PyTorch + Gradio | `AI-assistant/CLAUDE.md` |
| `AI-assistant-RAG/` | Offline RAG chatbot over local documents | Python | `README.md` (Korean) |
| `AI-assistant-workout/` | Offline workout-domain AI assistant | Python | `README.md` (Korean) |
| `Translator/` | Offline DPRK Korean ↔ EN/ZH/RU translator (text/voice/OCR) | Python ML + web frontend | `README.md` (Korean) |

## Reference & Asset Directories (no code)

- `DailyYoga/`, `DownDog/`, `FaceYoga/`, `GameUI/`, `HairTry/`, `Youcam Makeup/` — screenshots of competitor/reference apps, used as design references.
- `Videos/`, `Articles/`, `docs/` — media and saved reading material.
- `HomeDesign/`, `World Cuisines/` — empty placeholders for planned apps.

## Cross-Project Conventions

The full-stack app projects (TalentBaby, IoT, foodvisor, HeightIncrease) share a common shape:

- **Backend**: Express + layered architecture (`routes → controllers → services → repositories`), JWT auth, Swagger UI at `/api-docs`. Standard scripts: `npm run dev` / `build` / `lint` / `migrate` / `seed`. Each backend uses a distinct port (see table above) — check the project's `.env`.
- **Mobile**: native Android in Java (min SDK 24, target 33), Retrofit + OkHttp with a JWT `AuthInterceptor`, built with `./gradlew assembleDebug` from the project's `mobile/` (or `Smart-Home/`) directory. Emulator reaches a local backend at `http://10.0.2.2:<port>`; physical devices use the LAN IP.
- **i18n**: all user-facing Android text goes through the default English string resources (`res/values/strings.xml`). Database seed data and content stays **English only**.
- Project documentation and data are maintained in English.
