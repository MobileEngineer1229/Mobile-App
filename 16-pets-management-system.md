# Pets Management System

## Overview
A comprehensive mobile app for pet owners to manage their pets' health, daily care, medical records, appointments, and wellbeing — supporting dogs, cats, birds, rabbits, and small animals with multi-pet household management.

---

## Features

### Core Features
- **Pet Profiles** — Create a profile per pet: name, species, breed, age, weight, color, photo, microchip ID
- **Vaccination Tracker** — Log vaccines by type and date; receive reminders before boosters are due
- **Medication & Dosage Log** — Schedule recurring medications with dose, frequency, and refill reminders
- **Vet Appointment Manager** — Book, track, and receive reminders for vet, grooming, and dental visits
- **Weight & Growth Chart** — Log weight over time with a visual chart; flag underweight/overweight against breed standards
- **Feeding Log** — Track meal times, portion sizes, food brand, and water intake per pet
- **Health Records Vault** — Store lab results, X-rays, discharge summaries, prescriptions as uploaded files

### Additional Features
- **Symptom Checker** — Log symptoms and get AI-guided triage (home care vs. vet urgently)
- **Walk & Exercise Tracker** — GPS-tracked walks, daily activity goals, exercise history per pet
- **Heat / Breeding Cycle Tracker** — For unspayed females: track heat cycles, predict next cycle, breeding window
- **Lost Pet Alert** — Broadcast a missing pet alert with photo and last-known location to nearby app users
- **Pet Insurance Manager** — Store policy details, track claims, log reimbursements
- **Multi-Pet Dashboard** — See all pets at a glance: who needs feeding, whose vaccine is due, who hasn't walked today
- **Vet Directory** — Find nearby vets, emergency clinics, and groomers with ratings and hours
- **Pet Journal** — Daily diary entries with photos to capture milestones and memories

---

## Application Logic

### Vaccination Reminder Logic
- Each vaccine has a standard booster interval (e.g., Rabies: 1 or 3 years; DHPP: annual)
- System calculates next due date from last administered date
- Send reminders at: 30 days before, 7 days before, on due date, and overdue alerts at +7 and +30 days
- Flag vaccines as "overdue" with a visual indicator on the pet's health score

### Weight Monitoring Logic
- Maintain a weight log with date and value per entry
- Compare against breed-specific healthy weight range (sourced from reference table: breed → min/max kg)
- Calculate Body Condition Score (BCS) trend from sequential weight entries:
  - Losing > 10% body weight in 30 days → alert owner and suggest vet visit
  - Gaining > 15% over baseline → flag as overweight with diet tips
- Plot weight curve against a breed average growth curve for puppies and kittens

### Feeding Schedule Logic
- Owner sets a feeding schedule (times, portions, food type per pet)
- App sends meal-time push notifications
- Log actual feeding (time eaten, amount consumed, any refusal)
- Flag missed meals: 2+ consecutive missed meals triggers a health alert
- Track daily caloric intake against breed/weight/age-based energy requirement (kcal/day)

### Symptom Checker & Triage Logic
- Owner selects symptoms from a categorized list (digestive, respiratory, behavioral, skin/coat, mobility)
- Rule-based triage engine maps symptom combinations to urgency levels:
  - **Emergency** (go to vet now): difficulty breathing, suspected poisoning, seizure, uncontrolled bleeding
  - **Urgent** (vet within 24h): vomiting > 3 times, lethargy + loss of appetite, eye injury
  - **Monitor at home**: mild loose stool (once), minor limping after exercise, mild sneezing
- Log symptom history — AI detects recurring patterns (e.g., vomiting every Monday → correlate with Monday food brand)

### Heat Cycle Prediction Logic (Breeding Tracker)
- Dog heat cycle average: every 6 months, lasts 2–4 weeks
- Cat heat cycle: every 2–3 weeks during breeding season (spring–autumn)
- Collect last 2–3 observed heat start dates → calculate average interval
- Predict next heat window with ± 7-day range
- Fertile window estimate:
  ```
  Dogs: Days 9–14 of heat (standing heat / estrus phase)
  Cats: Days 2–8 of heat
  ```
- Notify owner 5 days before predicted heat start

### Lost Pet Alert Logic
- Owner taps "My Pet Is Lost" → captures last-known GPS location + pet photo
- Alert is broadcast to all app users within a configurable radius (default 5 km)
- Nearby users receive a push notification with the pet's photo, description, and owner contact
- If a user spots the pet, they tap "I Found This Pet" → owner is notified with spotter's location
- Alert auto-expires after 30 days (owner can extend)

### Multi-Pet Health Score Logic
- Each pet has a daily health score (0–100) calculated from:
  - Feeding logged today: +20
  - Medications taken on schedule: +20
  - Walk/exercise met target: +15
  - No overdue vaccines: +20
  - No active symptom flags: +25
- Dashboard shows a color-coded score per pet (green ≥ 80, yellow 50–79, red < 50)

---

## Challenges

| Challenge | Description |
|-----------|-------------|
| Multi-Species Breed Data | Breed-specific reference data (weight ranges, vaccine schedules, lifespan) required for dogs, cats, and exotic pets |
| Symptom Triage Accuracy | Wrong triage (classifying emergency as home-care) could result in harm; requires conservative, vet-reviewed logic |
| GPS Battery Drain | Continuous GPS tracking during walks must balance accuracy with battery usage |
| Data Portability | Owners want to share health records with a new vet — export format must be widely readable (PDF, CSV) |
| Lost Pet Privacy | Broadcasting a pet's location and owner contact must not expose the owner's home address |
| Multi-Pet UX Complexity | Households with 4+ pets need a clean UI that doesn't collapse under notification overload |
| Medication Errors | Wrong dose reminders for weight-dosed medications (e.g., antibiotics per kg) are a safety risk |
| Offline Reliability | Feeding logs and medication logs must work fully offline and sync reliably when reconnected |

---

## Recommended Tech Stack
- **Mobile**: Android Native (Java) — min SDK API 24 (Android 7.0+)
- **Backend**: Node.js + Express.js + PostgreSQL
- **HTTP Client**: Retrofit 2 + OkHttp
- **Charts**: MPAndroidChart — weight growth curve, activity history bar chart, health score trend line
- **Maps / GPS**: Google Maps SDK for Android + FusedLocationProviderClient — GPS walk tracking, lost pet broadcast radius
- **AI Symptom Engine**: Rule-based triage (local JSON ruleset in Room DB) + TensorFlow Lite for recurring pattern detection
- **Push Notifications**: Firebase Cloud Messaging (FCM) — vaccine reminders, meal alerts, lost pet broadcasts
- **File Storage**: AWS S3 (via backend) or Firebase Storage — vet records, X-rays, pet photos
- **Image Loading**: Glide
- **Auth**: Firebase Auth (Android SDK) + Android BiometricPrompt API (optional biometric lock)
- **Local DB / Offline**: Room Database + WorkManager — all logs and profiles work fully offline; background sync on reconnect
- **PDF Export**: iText 7 for Android — shareable health summary per pet
- **Architecture**: MVVM + LiveData + ViewModel (Android Jetpack)
