# Learning Russian App

## Overview
A language learning mobile app focused on Russian, teaching alphabet, vocabulary, grammar, and conversation through interactive lessons, spaced repetition, and gamification.

---

## Features

### Core Features
- **Alphabet (Cyrillic) Module** — Interactive lessons for each letter with pronunciation audio
- **Vocabulary Flashcards** — Word cards with image, translation, audio pronunciation
- **Spaced Repetition System (SRS)** — Smart review scheduling (Anki-style)
- **Grammar Lessons** — Structured lessons on cases, verb conjugation, gender, etc.
- **Listening Exercises** — Audio clips with comprehension questions
- **Speaking Practice** — Record and compare pronunciation with native speaker audio
- **Writing Practice** — Trace Cyrillic letters, type words in Cyrillic keyboard
- **Daily Lessons** — Bite-sized structured lessons by proficiency level (A1–C2)

### Additional Features
- **Streak & XP System** — Daily goals, streaks, experience points
- **Leaderboard** — Weekly ranking against other learners
- **Phrasebook** — Common travel/conversation phrases with audio
- **Mini-Games** — Word match, fill-in-the-blank, listening quiz
- **Progress Dashboard** — Words learned, accuracy rate, time spent
- **Offline Mode** — Download lessons for offline use
- **Keyboard Support** — Built-in Cyrillic keyboard for input exercises

---

## Application Logic

### Spaced Repetition Logic
- Use SM-2 algorithm (SuperMemo) to schedule card reviews
- Score: 0 (blackout) to 5 (perfect recall)
- Ease factor adjusts interval based on performance
- New cards → short intervals; mastered cards → long intervals (days → weeks)

### Lesson Progression Logic
- Lock lessons until prerequisites are completed
- Track lesson completion percentage (vocabulary + exercises)
- Unlock next unit when current unit reaches 80% mastery

### Pronunciation Scoring Logic
- Record user audio → convert to phoneme sequence
- Compare against reference phoneme sequence using DTW (Dynamic Time Warping) or speech recognition API
- Display score and highlight mispronounced sounds

### Streak Logic
- A streak increments if at least one lesson is completed per day
- Grace period of 1 day with a "streak freeze" item
- Reset streak if no activity and no freeze used

---

## Challenges

| Challenge | Description |
|-----------|-------------|
| Cyrillic Input | Implementing a smooth Cyrillic keyboard experience on both iOS and Android |
| Audio Quality | Recording and storing high-quality native speaker audio for all vocabulary |
| Speech Recognition | Accurately evaluating Russian pronunciation from non-native speakers |
| Grammar Complexity | Russian has 6 grammatical cases — modeling and teaching them progressively |
| SRS Tuning | Calibrating SRS intervals for the Russian learner's typical forgetting curve |
| Offline Sync | Syncing SRS review data and progress when coming back online |
| Motivation Retention | Keeping users engaged beyond the first week (gamification balance) |

---

## Recommended Tech Stack
- **Mobile**: Flutter
- **Backend**: Node.js + PostgreSQL
- **Audio**: expo-av / just_audio, hosted on S3 or Firebase Storage
- **Speech**: Google Speech-to-Text API or Azure Speech SDK
- **SRS**: Custom SM-2 implementation
- **State Management**: Riverpod / BLoC
