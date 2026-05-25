# AI Old Friends & Relatives Finder

## Overview

An Android Native (Java) app that automatically discovers and visualizes connections between people based on shared educational history — elementary school, middle school, high school, and university. When a user signs up and inputs their school history, the system quietly runs AI matching in the background and presents a living relationship graph: nodes for people, edges for how they are connected (classmates, schoolmates, relatives, colleagues). The UI is inspired by the relationship visualization in **Romance of the Three Kingdoms 13 (三國志13)** — a radial or force-directed graph where tapping a node reveals that person's story and their connections.

The app solves a real problem: elderly people who grew up before mobile phones and the internet have no way to find the friends and relatives they lost touch with decades ago. They do not need a search bar — they need the app to figure it out for them automatically.

---

## Core Concept — Relationship Graph (Sanguo-style)

```
              [Kim Cheol-su]
             /      |        \
      (classmate)  (cousin)  (schoolmate)
          /          |            \
   [Lee Minja]  [Park Sungho]  [Choi Yeonsuk]
        |                           |
  (university)               (same hometown)
        |                           |
  [Jung Minsu]              [Han Daeyoung]
```

- Every person is a **node** on the graph
- Every shared connection is an **edge** with a label (Elementary '72, Middle School '78, Relatives, etc.)
- The graph is **interactive**: pinch to zoom, tap a node to expand, swipe to explore the network
- Edges are **color-coded** by relationship type:
  - Blue → School (elementary / middle / high)
  - Green → University / Vocational school
  - Orange → Family / Relatives
  - Purple → Workplace colleagues
  - Gray → Hometown / Neighborhood

---

## Features

### Auto-Matching (No Search Required)
- User fills in their profile once: name, birth year, schools attended with graduation years, hometown, family info
- AI runs silently in the background, comparing against all other user profiles
- When a match is found above a confidence threshold, the node appears on the user's graph
- User confirms or dismisses — confirmed connections strengthen the graph for everyone

### Relationship Graph UI
- **Force-directed graph** (like Three Kingdoms relationship screen) — nodes repel each other and settle naturally
- **Tap a node** → profile card slides up: name, photo, schools, shared memories
- **Long-press an edge** → see why they are connected ("Same elementary school, Pyongyang School No.1, graduated 1972")
- **Pinch to zoom** — explore dense clusters (e.g., a whole class from 1975)
- **Filter by relationship type** — toggle school / family / workplace layers on or off

### Profile Input
- Name (with alternate spellings / name-change history)
- Birth year
- Elementary school — name, location, graduation year
- Middle school — name, location, graduation year
- High school — name, location, graduation year
- University / vocational school — name, graduation year
- Hometown / district
- Known relatives (name, relationship label, approximate birth year)
- Optional: old photo upload

### AI Matching Engine
- Matches on: same school name + overlapping graduation years (±2 years)
- Fuzzy school name matching — handles renamed schools, merged districts, different romanizations
- Name normalization — marriage name changes, transliteration variants, spelling differences
- Family tree intersection — if two users share a named relative, they are likely connected
- Confidence score shown on edge label (e.g., "87% match — same school, overlapping years")

### Memory Verification (Lightweight)
- When a match is proposed, the app shows one simple prompt to confirm:
  - "Did your elementary school have a red gate?" (generated from era + region data)
  - "Do you remember a teacher named Park?" 
- Not a quiz — just a gentle human confirmation before the node appears

### Notifications
- "A new connection was found — someone from your middle school joined"
- "3 people from Pyongyang School No.1 class of 1972 are now on the app"
- "Your possible relative Kim Sungil confirmed the connection"

---

## Application Logic

### Matching Pipeline

```
New user registers
        ↓
Profile indexed (school names normalized → ElasticSearch)
        ↓
Background job: compare against all existing profiles
        ↓
Score each candidate:
  +40  same elementary school, ±2 graduation years
  +35  same middle school, ±2 graduation years
  +25  same high school
  +20  same university
  +30  shared named relative
  +10  same hometown district
  +5   same birth year range
        ↓
Candidates above threshold → proposed as graph nodes
        ↓
User confirms / dismisses → graph updates
        ↓
Confirmed connection → both users' graphs update bidirectionally
```

### Graph Data Model

```
Node: Person
  - userId
  - displayName
  - birthYear
  - profilePhotoUrl
  - schools[ { name, type, graduationYear, location } ]
  - relatives[ { name, relation } ]

Edge: Connection
  - fromUserId
  - toUserId
  - relationshipType  (SCHOOL | UNIVERSITY | FAMILY | WORKPLACE | HOMETOWN)
  - label             ("Elementary School No.1, class of 1972")
  - confidenceScore
  - confirmedByBoth   (boolean)
```

### Project Structure

```
ai-friends-finder-android/
├── app/
│   ├── src/main/java/com/friendsfinder/
│   │   ├── ui/
│   │   │   ├── graph/
│   │   │   │   ├── GraphActivity.java          # main relationship graph screen
│   │   │   │   ├── GraphRenderer.java          # force-directed layout engine
│   │   │   │   ├── NodeView.java               # individual person node
│   │   │   │   └── EdgeView.java               # connection line + label
│   │   │   ├── profile/
│   │   │   │   ├── ProfileSetupActivity.java   # onboarding: school history input
│   │   │   │   └── PersonCardFragment.java     # slide-up card when node tapped
│   │   │   ├── notifications/
│   │   │   │   └── MatchNotificationService.java
│   │   │   └── onboarding/
│   │   │       └── OnboardingActivity.java
│   │   ├── data/
│   │   │   ├── api/
│   │   │   │   ├── MatchingApiService.java     # Retrofit interface to backend
│   │   │   │   └── GraphApiService.java
│   │   │   ├── model/
│   │   │   │   ├── Person.java
│   │   │   │   ├── Connection.java
│   │   │   │   └── School.java
│   │   │   └── repository/
│   │   │       ├── GraphRepository.java
│   │   │       └── ProfileRepository.java
│   │   └── service/
│   │       └── BackgroundMatchService.java     # WorkManager job for auto-matching
│   ├── res/
│   │   ├── layout/
│   │   │   ├── activity_graph.xml
│   │   │   ├── activity_profile_setup.xml
│   │   │   └── fragment_person_card.xml
│   │   └── values/
│   │       ├── colors.xml                      # edge color scheme
│   │       └── strings.xml
│   └── build.gradle
├── backend/                                    # Node.js / NestJS
│   ├── src/
│   │   ├── profiles/
│   │   ├── matching/                           # AI scoring engine
│   │   ├── graph/                              # Neo4j queries
│   │   └── notifications/                      # FCM push
│   └── prisma/schema.prisma
└── ai-service/                                 # Python FastAPI
    ├── normalizer/
    │   ├── school_name.py                      # fuzzy school name matching
    │   └── person_name.py                      # transliteration, name variants
    ├── scorer/
    │   └── match_scorer.py                     # weighted scoring pipeline
    └── main.py
```

---

## Graph UI — Sanguo Style Reference

| Sanguo 13 Feature | This App Equivalent |
|---|---|
| Character relationship web | Person relationship web |
| Alliance / enemy / neutral edges | School / family / colleague edges |
| Tap character → stats card | Tap person → profile card with shared schools |
| Color-coded relationship lines | Color-coded by school / family / work |
| Zoom in/out on the map | Zoom in/out on the relationship graph |
| Relationships auto-update as game progresses | Graph auto-updates as new users join and matches are confirmed |

---

## Challenges

| Challenge | Description |
|-----------|-------------|
| School Name Changes | Schools renamed, merged, or closed over decades — need a curated alias dictionary by region and year |
| Graph Performance | Rendering hundreds of nodes smoothly on mid-range Android devices requires level-of-detail culling and lazy loading |
| Name Variants | Korean, Chinese, and other names have many spelling and transliteration forms — fuzzy matching must be tuned carefully |
| Cold Start | Graph is empty for new users until others from the same schools sign up — community boards or "invite a schoolmate" flow helps bootstrap |
| Elderly UX | Large touch targets, simple onboarding, minimal typing (dropdowns for school names where possible), family-member proxy setup |
| False Positive Matches | Two people at the same school in different years should not be shown as classmates — graduation year window must be tight (±1 or ±2 years) |

---

## Tech Stack

| Layer | Technology |
|---|---|
| Mobile | Android Native — Java, XML layouts, Material Design 3 |
| Graph Rendering | Custom View (Canvas + ObjectAnimator) or **GraphView** library adapted for person nodes |
| Networking | Retrofit 2 + OkHttp |
| Local DB | Room (SQLite) — cached graph data for offline viewing |
| Background Matching | WorkManager — periodic background job |
| Push Notifications | Firebase Cloud Messaging (FCM) |
| Backend API | Node.js / NestJS + PostgreSQL |
| Relationship Graph DB | Neo4j — friend-of-friend queries |
| AI / ML Service | Python FastAPI — name normalization, school fuzzy match, confidence scoring |
| Auth | Firebase Auth |
| Photo Storage | AWS S3 |

## MVP Scope

1. Profile setup: name, birth year, elementary + middle + high school + university with graduation years
2. Background AI matching job — runs on new registrations
3. Relationship graph screen — force-directed layout, color-coded edges, tap to expand
4. Person card fragment — profile info + shared connection reason
5. Match confirmation flow — "Is this your classmate?" yes / no
6. FCM notification when a new match is found
