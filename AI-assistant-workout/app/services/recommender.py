"""Profile-based workout routine recommender.

The recommender combines three layers:

1. Dataset similarity:
   Finds similar BMI/body-fat/age/gender cases from the imported datasets.
2. Activity evidence:
   Uses WHOOP and gym aggregates to choose realistic activity durations,
   calories, and strain ranges.
3. Safety rules:
   Adjusts volume and intensity based on age, BMI, recovery, sleep, injuries,
   available time, and user goal.

Feedback is stored by the API and can later be used to tune activity weights.
"""

from __future__ import annotations

import json
import math
import sqlite3
from collections import Counter
from dataclasses import dataclass
from pathlib import Path
from typing import Any


ROOT = Path(__file__).resolve().parents[2]
DB_PATH = ROOT / "storage" / "workout.db"
PLAN_MODEL_PATH = ROOT / "storage" / "plan_model.joblib"
_PLAN_MODEL_CACHE: dict[str, Any] | None = None


PLAN_NAMES = {
    "1": "Recovery Strength Foundation",
    "2": "Gentle Strength Builder",
    "3": "Balanced Starter",
    "4": "General Fitness Builder",
    "5": "Fat Loss Conditioning",
    "6": "Low Impact Fat Loss",
    "7": "Medical Caution Low Impact",
}

GOAL_PLAN_NAMES = {
    "fat_loss": "Fat Loss Conditioning",
    "muscle_gain": "Strength and Muscle Builder",
    "endurance": "Endurance Builder",
    "mobility": "Mobility and Recovery Builder",
    "general": "General Fitness Builder",
}

GOAL_LABELS = {
    "fat_loss": "fat loss",
    "muscle_gain": "muscle gain",
    "endurance": "endurance",
    "mobility": "mobility",
    "general": "general fitness",
}

GOAL_ACTIVITY_PRIORITY = {
    "fat_loss": ["Walking", "Cycling", "Swimming", "Weight Training", "Yoga", "HIIT"],
    "muscle_gain": ["Weight Training", "Walking", "Yoga", "Cycling"],
    "endurance": ["Walking", "Cycling", "Running", "Swimming", "Yoga"],
    "mobility": ["Yoga", "Walking", "Swimming"],
    "general": ["Weight Training", "Walking", "Yoga", "Cycling", "Swimming"],
}

LOW_IMPACT = {"Walking", "Cycling", "Swimming", "Yoga"}
HIGH_INTENSITY = {"HIIT", "CrossFit", "Running"}


@dataclass
class Profile:
    age: int
    gender: str
    height_cm: float
    weight_kg: float
    body_fat_pct: float | None
    fitness_level: str
    goal: str
    days_per_week: int
    session_minutes: int
    equipment: str
    injuries: str
    sleep_hours: float | None
    recovery_score: float | None


@dataclass
class TrainingCycle:
    weeks: int
    training_days_per_week: int
    available_days_per_week: int
    deload_week: int | None
    reason: str


def connect() -> sqlite3.Connection:
    conn = sqlite3.connect(DB_PATH)
    conn.row_factory = sqlite3.Row
    return conn


def database_ready() -> bool:
    if not DB_PATH.exists():
        return False
    with connect() as conn:
        row = conn.execute("SELECT COUNT(*) AS count FROM recommendation_cases").fetchone()
        return bool(row and row["count"] > 0)


def plan_model_ready() -> bool:
    return PLAN_MODEL_PATH.exists()


def normalize_profile(payload: dict[str, Any]) -> Profile:
    """Validate and normalize incoming user profile."""
    height_cm = float(payload.get("height_cm") or 170)
    weight_kg = float(payload.get("weight_kg") or 70)
    age = int(payload.get("age") or 30)
    days = max(2, min(6, int(payload.get("days_per_week") or 4)))
    minutes = max(15, min(120, int(payload.get("session_minutes") or 45)))
    body_fat = payload.get("body_fat_pct")
    recovery = payload.get("recovery_score")
    sleep = payload.get("sleep_hours")
    return Profile(
        age=max(13, min(90, age)),
        gender=str(payload.get("gender") or "Other"),
        height_cm=height_cm,
        weight_kg=weight_kg,
        body_fat_pct=float(body_fat) if body_fat not in (None, "") else None,
        fitness_level=normalize_level(str(payload.get("fitness_level") or "Beginner")),
        goal=str(payload.get("goal") or "general"),
        days_per_week=days,
        session_minutes=minutes,
        equipment=str(payload.get("equipment") or "bodyweight"),
        injuries=str(payload.get("injuries") or "").strip(),
        sleep_hours=float(sleep) if sleep not in (None, "") else None,
        recovery_score=float(recovery) if recovery not in (None, "") else None,
    )


def build_routine(payload: dict[str, Any], *, save: bool = True) -> dict[str, Any]:
    profile = normalize_profile(payload)
    bmi = profile.weight_kg / ((profile.height_cm / 100) ** 2)
    bmi_case = classify_bmi(bmi)
    bfp_case = classify_bfp(profile.body_fat_pct, profile.gender)
    plan_code, similar_cases, ml_prediction = choose_plan_code(profile, bmi, bfp_case)
    activity_pool = choose_activity_pool(profile, bmi_case, plan_code)
    cycle = predict_training_cycle(profile, bmi_case, plan_code)
    cycle_weeks = build_cycle(profile, activity_pool, cycle)
    warnings = build_warnings(profile, bmi_case)
    evidence = build_evidence(profile, plan_code, similar_cases, activity_pool, ml_prediction)

    routine = {
        "profile_summary": {
            "age": profile.age,
            "gender": profile.gender,
            "height_cm": round(profile.height_cm, 1),
            "weight_kg": round(profile.weight_kg, 1),
            "bmi": round(bmi, 1),
            "bmi_case": bmi_case,
            "body_fat_pct": profile.body_fat_pct,
            "body_fat_case": bfp_case,
            "fitness_level": profile.fitness_level,
            "goal": profile.goal,
        },
        "plan": {
            "code": plan_code,
            "name": display_plan_name(profile, plan_code),
            "dataset_plan_name": PLAN_NAMES.get(plan_code, "Personalized Plan"),
            "days_per_week": cycle.training_days_per_week,
            "available_days_per_week": cycle.available_days_per_week,
            "cycle_weeks": cycle.weeks,
            "deload_week": cycle.deload_week,
            "session_minutes": profile.session_minutes,
            "intensity": choose_intensity(profile, bmi_case, plan_code),
            "summary": build_summary(profile, bmi_case, plan_code, cycle),
        },
        "week": cycle_weeks[0]["days"] if cycle_weeks else [],
        "cycle": {
            "weeks": cycle.weeks,
            "training_days_per_week": cycle.training_days_per_week,
            "available_days_per_week": cycle.available_days_per_week,
            "deload_week": cycle.deload_week,
            "reason": cycle.reason,
            "schedule": cycle_weeks,
        },
        "progression": build_progression(profile, plan_code, cycle),
        "safety_notes": warnings,
        "dataset_evidence": evidence,
    }

    if save:
        user_id, plan_id = save_plan(profile, routine)
        routine["user_id"] = user_id
        routine["plan_id"] = plan_id
    return routine


def choose_plan_code(profile: Profile, bmi: float, bfp_case: str | None) -> tuple[str, list[dict[str, Any]], dict[str, Any]]:
    """Blend ML prediction with nearest-neighbor dataset evidence."""
    with connect() as conn:
        rows = conn.execute(
            """
            SELECT * FROM recommendation_cases
            WHERE plan_code IS NOT NULL AND plan_code != ''
            LIMIT 10000
            """
        ).fetchall()
    scored = []
    for row in rows:
        score = 0.0
        row_bmi = row["bmi"] or bmi
        row_age = row["age"] or profile.age
        score += abs(row_bmi - bmi) * 2.0
        score += abs(row_age - profile.age) / 12.0
        if row["gender"] and row["gender"].lower() != profile.gender.lower():
            score += 0.4
        if profile.body_fat_pct is not None and row["body_fat_pct"] is not None:
            score += abs(row["body_fat_pct"] - profile.body_fat_pct) / 8.0
        if bfp_case and row["bfp_case"] and row["bfp_case"].lower() != bfp_case.lower():
            score += 0.5
        scored.append((score, row))
    scored.sort(key=lambda item: item[0])
    nearest = scored[:35]
    votes = Counter(str(row["plan_code"]) for _, row in nearest)
    knn_code = votes.most_common(1)[0][0] if votes else fallback_plan_from_bmi(bmi)
    ml_prediction = predict_plan_with_model(profile, bmi, bfp_case)
    plan_code = blend_plan_predictions(knn_code, votes, ml_prediction)
    return plan_code, [
        {
            "plan_code": str(row["plan_code"]),
            "bmi": round(row["bmi"] or 0, 1),
            "age": row["age"],
            "gender": row["gender"],
            "source": row["source"],
            "distance": round(score, 3),
        }
        for score, row in nearest[:5]
    ], ml_prediction


def predict_plan_with_model(profile: Profile, bmi: float, bfp_case: str | None) -> dict[str, Any]:
    """Predict plan code with the trained scikit-learn model when available."""
    bundle = load_plan_model()
    if not bundle:
        return {"available": False, "reason": "storage/plan_model.joblib not found"}
    model = bundle["model"]
    sample = {
        "age": profile.age,
        "height_cm": profile.height_cm,
        "weight_kg": profile.weight_kg,
        "bmi": bmi,
        "body_fat_pct": profile.body_fat_pct if profile.body_fat_pct is not None else -1.0,
        "gender": profile.gender,
        "bmi_case": classify_bmi(bmi),
        "bfp_case": bfp_case or "unknown",
    }
    predicted = str(model.predict([sample])[0])
    probabilities = {}
    if hasattr(model, "predict_proba"):
        classes = [str(item) for item in model.classes_]
        probs = model.predict_proba([sample])[0]
        probabilities = {
            cls: round(float(prob), 4)
            for cls, prob in sorted(zip(classes, probs), key=lambda item: item[1], reverse=True)
        }
    confidence = max(probabilities.values()) if probabilities else None
    return {
        "available": True,
        "predicted_plan_code": predicted,
        "confidence": confidence,
        "probabilities": probabilities,
        "model_accuracy": round(bundle.get("metrics", {}).get("accuracy", 0), 4),
    }


def load_plan_model() -> dict[str, Any] | None:
    """Load cached scikit-learn plan prediction model."""
    global _PLAN_MODEL_CACHE
    if _PLAN_MODEL_CACHE is not None:
        return _PLAN_MODEL_CACHE
    if not PLAN_MODEL_PATH.exists():
        return None
    try:
        import joblib
    except ImportError:
        return None
    _PLAN_MODEL_CACHE = joblib.load(PLAN_MODEL_PATH)
    return _PLAN_MODEL_CACHE


def blend_plan_predictions(knn_code: str, votes: Counter, ml_prediction: dict[str, Any]) -> str:
    """Choose final plan code by combining model probabilities and KNN votes."""
    scores: Counter[str] = Counter()
    total_votes = sum(votes.values()) or 1
    for code, count in votes.items():
        scores[str(code)] += 0.40 * (count / total_votes)
    if ml_prediction.get("available"):
        for code, prob in ml_prediction.get("probabilities", {}).items():
            scores[str(code)] += 0.60 * float(prob)
    if not scores:
        return knn_code
    return scores.most_common(1)[0][0]


def choose_activity_pool(profile: Profile, bmi_case: str, plan_code: str) -> list[dict[str, Any]]:
    """Select activity types from dataset aggregates and safety rules."""
    goal = profile.goal if profile.goal in GOAL_ACTIVITY_PRIORITY else "general"
    desired = GOAL_ACTIVITY_PRIORITY[goal][:]
    if plan_code in {"6", "7"} or "obese" in bmi_case:
        desired = [a for a in desired if a in LOW_IMPACT] + ["Weight Training"]
    if profile.injuries:
        desired = [a for a in desired if a not in HIGH_INTENSITY]
    if profile.fitness_level == "Beginner":
        desired = [a for a in desired if a not in {"CrossFit"}]
    if not desired:
        desired = ["Walking", "Yoga", "Weight Training"]

    stats = load_activity_stats(profile.fitness_level)
    feedback_adjustments = load_feedback_activity_adjustments(profile.goal, profile.fitness_level)
    pool = []
    for rank, activity in enumerate(desired):
        item = stats.get(activity) or stats.get("Walking") or {}
        source_duration = max(1, item.get("duration", profile.session_minutes))
        duration = min(profile.session_minutes, max(15, source_duration))
        adjustment = feedback_adjustments.get(activity, default_feedback_adjustment())
        duration = max(15, min(profile.session_minutes, duration * adjustment["duration_factor"]))
        source_calories = item.get("calories", estimate_calories(activity, source_duration))
        adjusted_calories = source_calories * duration / source_duration
        pool.append(
            {
                "activity": activity,
                "base_duration": round(duration),
                "avg_calories": round(adjusted_calories),
                "avg_strain": round(item.get("strain", default_strain(activity)), 1),
                "source_samples": item.get("samples", 0),
                "feedback_score": round(adjustment["score"], 3),
                "feedback_samples": adjustment["samples"],
                "rank_score": round(rank - adjustment["score"], 3),
            }
        )
    pool.sort(key=lambda item: item["rank_score"])
    return pool[: max(3, min(5, len(pool)))]


def predict_training_cycle(profile: Profile, bmi_case: str, plan_code: str) -> TrainingCycle:
    """Predict routine cycle length and weekly frequency.

    `days_per_week` from the form is treated as user availability, not a fixed
    prescription. The service predicts the actual cycle based on goal, level,
    BMI risk, recovery, sleep, age, and plan code.
    """
    available = profile.days_per_week
    if profile.goal == "muscle_gain":
        weeks = 8
    elif profile.goal in {"fat_loss", "endurance"}:
        weeks = 6
    elif profile.goal == "mobility":
        weeks = 4
    else:
        weeks = 6

    if profile.fitness_level == "Beginner":
        weeks = min(weeks, 4 if profile.goal in {"mobility", "general"} else 6)
        training_days = min(available, 3 if available <= 4 else 4)
    elif profile.fitness_level == "Advanced":
        weeks = max(weeks, 8 if profile.goal in {"muscle_gain", "endurance"} else weeks)
        training_days = min(available, 5)
    else:
        training_days = min(available, 4)

    if plan_code in {"6", "7"} or "obese" in bmi_case:
        weeks = min(weeks, 4)
        training_days = min(training_days, 3)
    if profile.age >= 60:
        training_days = min(training_days, 3)
    if profile.recovery_score is not None and profile.recovery_score < 45:
        weeks = 4
        training_days = min(training_days, 3)
    if profile.sleep_hours is not None and profile.sleep_hours < 6:
        training_days = min(training_days, 3)

    training_days = max(2, min(6, training_days))
    deload_week = weeks if weeks >= 4 else None
    reason = (
        f"Predicted {weeks}-week cycle from goal={profile.goal}, level={profile.fitness_level}, "
        f"availability={available}, bmi_case={bmi_case}, recovery={profile.recovery_score}."
    )
    return TrainingCycle(
        weeks=weeks,
        training_days_per_week=training_days,
        available_days_per_week=available,
        deload_week=deload_week,
        reason=reason,
    )


def build_cycle(profile: Profile, pool: list[dict[str, Any]], cycle: TrainingCycle) -> list[dict[str, Any]]:
    """Generate every week in the predicted cycle."""
    weeks = []
    for week_index in range(1, cycle.weeks + 1):
        factor = week_progression_factor(week_index, cycle)
        weeks.append(
            {
                "week": week_index,
                "focus": week_focus(week_index, cycle),
                "days": build_week(profile, pool, cycle.training_days_per_week, week_index, factor),
            }
        )
    return weeks


def build_week(
    profile: Profile,
    pool: list[dict[str, Any]],
    training_days: int,
    week_number: int = 1,
    progression_factor: float = 1.0,
) -> list[dict[str, Any]]:
    days = []
    recovery_factor = recovery_factor_for(profile)
    for index in range(training_days):
        activity = pool[(index + week_number - 1) % len(pool)]
        duration = max(15, round(activity["base_duration"] * recovery_factor * progression_factor))
        duration = min(profile.session_minutes, duration)
        warmup = min(8, max(5, round(duration * 0.15)))
        cooldown = min(8, max(5, round(duration * 0.12)))
        main = max(8, duration - warmup - cooldown)
        days.append(
            {
                "day": index + 1,
                "week": week_number,
                "title": f"{activity['activity']} session",
                "activity": activity["activity"],
                "duration_min": duration,
                "estimated_calories": round(activity["avg_calories"] * duration / max(1, activity["base_duration"])),
                "structure": [
                    {"part": "Warm-up", "minutes": warmup, "details": warmup_details(activity["activity"])},
                    {"part": "Main work", "minutes": main, "details": main_details(activity["activity"], profile)},
                    {"part": "Cool-down", "minutes": cooldown, "details": cooldown_details(activity["activity"])},
                ],
                "target_effort": target_effort(profile, activity["activity"]),
            }
        )
    return days


def load_activity_stats(level: str) -> dict[str, dict[str, float]]:
    with connect() as conn:
        rows = conn.execute(
            """
            SELECT * FROM whoop_activity_stats
            WHERE fitness_level = ?
            """,
            (level,),
        ).fetchall()
        if not rows:
            rows = conn.execute("SELECT * FROM whoop_activity_stats WHERE fitness_level = 'Beginner'").fetchall()
    return {
        row["activity_type"]: {
            "samples": row["samples"],
            "duration": row["avg_duration_min"],
            "calories": row["avg_calories"],
            "strain": row["avg_strain"],
            "recovery": row["avg_recovery"],
        }
        for row in rows
    }


def save_plan(profile: Profile, routine: dict[str, Any]) -> tuple[int, int]:
    profile_json = json.dumps(profile.__dict__, ensure_ascii=False)
    routine_json = json.dumps(routine, ensure_ascii=False)
    with connect() as conn:
        cur = conn.execute("INSERT INTO users (profile_json) VALUES (?)", (profile_json,))
        user_id = int(cur.lastrowid)
        cur = conn.execute(
            "INSERT INTO routine_plans (user_id, profile_json, routine_json) VALUES (?, ?, ?)",
            (user_id, profile_json, routine_json),
        )
        plan_id = int(cur.lastrowid)
        conn.commit()
    return user_id, plan_id


def save_feedback(payload: dict[str, Any]) -> dict[str, Any]:
    with connect() as conn:
        conn.execute(
            """
            INSERT INTO feedback
            (plan_id, rating, difficulty, completed, notes, profile_json, routine_json)
            VALUES (?, ?, ?, ?, ?, ?, ?)
            """,
            (
                payload.get("plan_id"),
                payload.get("rating"),
                payload.get("difficulty"),
                1 if payload.get("completed") else 0,
                payload.get("notes", ""),
                json.dumps(payload.get("profile") or {}, ensure_ascii=False),
                json.dumps(payload.get("routine") or {}, ensure_ascii=False),
            ),
        )
        conn.commit()
    return {"ok": True, "message": "Feedback saved. It will be used to tune future recommendations."}


def feedback_summary() -> dict[str, Any]:
    with connect() as conn:
        row = conn.execute(
            "SELECT COUNT(*) AS count, AVG(rating) AS avg_rating FROM feedback"
        ).fetchone()
    return {"count": row["count"], "avg_rating": round(row["avg_rating"] or 0, 2)}


def load_feedback_activity_adjustments(goal: str, fitness_level: str) -> dict[str, dict[str, float]]:
    """Convert saved feedback into activity preference and duration modifiers.

    This is the first feedback-learning loop. It does not retrain a model yet,
    but it lets real user feedback immediately influence future routines:
    - high rating + completed increases activity preference,
    - "Too hard" lowers preference and shortens duration,
    - "Too easy" can slightly increase duration.
    """
    if not DB_PATH.exists():
        return {}
    adjustments: dict[str, dict[str, float]] = {}
    with connect() as conn:
        rows = conn.execute(
            """
            SELECT rating, difficulty, completed, profile_json, routine_json
            FROM feedback
            WHERE routine_json IS NOT NULL AND routine_json != ''
            ORDER BY id DESC
            LIMIT 1000
            """
        ).fetchall()
    for row in rows:
        try:
            profile = json.loads(row["profile_json"] or "{}")
            routine = json.loads(row["routine_json"] or "{}")
        except json.JSONDecodeError:
            continue
        if profile and profile.get("goal") and profile.get("goal") != goal:
            continue
        if profile and profile.get("fitness_level") and profile.get("fitness_level") != fitness_level:
            continue
        rating = float(row["rating"] or 0)
        completed = bool(row["completed"])
        difficulty = str(row["difficulty"] or "").lower()
        delta = (rating - 3.0) / 5.0
        if completed:
            delta += 0.15
        else:
            delta -= 0.25
        duration_delta = 0.0
        if "too hard" in difficulty:
            delta -= 0.25
            duration_delta -= 0.10
        elif "too easy" in difficulty:
            delta += 0.10
            duration_delta += 0.05
        feedback_days = []
        cycle_schedule = routine.get("cycle", {}).get("schedule", [])
        if cycle_schedule:
            for week in cycle_schedule:
                feedback_days.extend(week.get("days", []))
        else:
            feedback_days = list(routine.get("week", []))
        for day in feedback_days:
            activity = day.get("activity")
            if not activity:
                continue
            item = adjustments.setdefault(activity, {"score": 0.0, "duration_factor": 1.0, "samples": 0})
            item["score"] += delta
            item["duration_factor"] += duration_delta
            item["samples"] += 1
    for item in adjustments.values():
        samples = max(1, item["samples"])
        item["score"] = max(-0.8, min(0.8, item["score"] / samples))
        item["duration_factor"] = max(0.75, min(1.15, item["duration_factor"] / samples))
    return adjustments


def default_feedback_adjustment() -> dict[str, float]:
    return {"score": 0.0, "duration_factor": 1.0, "samples": 0}


def build_evidence(
    profile: Profile,
    plan_code: str,
    similar_cases: list[dict[str, Any]],
    pool: list[dict[str, Any]],
    ml_prediction: dict[str, Any],
) -> dict[str, Any]:
    return {
        "selected_plan_code": plan_code,
        "ml_prediction": ml_prediction,
        "similar_profile_cases": similar_cases,
        "activity_dataset_matches": pool,
        "feedback": feedback_summary(),
        "notes": [
            "Plan code was selected by blending scikit-learn ML prediction with nearest-profile dataset evidence.",
            "Activity durations and calories use WHOOP/gym aggregate statistics.",
            "Saved user feedback can be used for later model tuning and weighting.",
        ],
    }


def build_summary(profile: Profile, bmi_case: str, plan_code: str, cycle: TrainingCycle) -> str:
    goal_text = GOAL_LABELS.get(profile.goal, "general fitness")
    plan_name = display_plan_name(profile, plan_code)
    return (
        f"This {plan_name} routine targets {goal_text}. "
        f"It is adjusted for {profile.fitness_level.lower()} level, {bmi_case}, "
        f"a predicted {cycle.weeks}-week cycle, {cycle.training_days_per_week} training days per week, "
        f"and {profile.session_minutes}-minute sessions."
    )


def display_plan_name(profile: Profile, plan_code: str) -> str:
    """Return the user-facing plan name.

    The dataset plan code mainly describes body-composition risk/intensity.
    The visible plan name should follow the user's selected goal so the UI does
    not say "Fat Loss" when the user asked for general fitness.
    """
    return GOAL_PLAN_NAMES.get(profile.goal, PLAN_NAMES.get(plan_code, "Personalized Plan"))


def build_progression(profile: Profile, plan_code: str, cycle: TrainingCycle) -> list[str]:
    if plan_code in {"6", "7"}:
        return [
            "Week 1-2: keep every session easy enough to speak in full sentences.",
            f"Week 3-{max(3, cycle.weeks - 1)}: add a small amount of duration only if recovery feels good.",
            f"Week {cycle.weeks}: deload and review pain, completion rate, and energy.",
        ]
    return [
        "Week 1: learn movements and finish sessions with energy left.",
        f"Week 2-{max(2, cycle.weeks - 1)}: gradually add duration, load, or one set to selected sessions.",
        f"Week {cycle.weeks}: deload, compare energy and soreness, then rebuild the next cycle.",
    ]


def week_progression_factor(week_number: int, cycle: TrainingCycle) -> float:
    if cycle.deload_week and week_number == cycle.deload_week:
        return 0.78
    if cycle.weeks <= 4:
        factors = {1: 0.9, 2: 1.0, 3: 1.06}
        return factors.get(week_number, 0.78)
    build_weeks = max(1, cycle.weeks - 1)
    return min(1.12, 0.9 + (week_number - 1) * (0.22 / build_weeks))


def week_focus(week_number: int, cycle: TrainingCycle) -> str:
    if cycle.deload_week and week_number == cycle.deload_week:
        return "Deload and assessment"
    if week_number == 1:
        return "Technique and baseline"
    if week_number <= max(2, cycle.weeks // 2):
        return "Build consistency"
    return "Progressive overload"


def build_warnings(profile: Profile, bmi_case: str) -> list[str]:
    notes = [
        "Stop training if there is chest pain, dizziness, sharp joint pain, or unusual shortness of breath.",
        "This service is not a medical diagnosis. High-risk users should consult a qualified professional.",
    ]
    if profile.age >= 55:
        notes.append("Because age is 55+, start conservatively and prioritize warm-up, balance, and recovery.")
    if "obese" in bmi_case:
        notes.append("Use low-impact options first to protect knees, hips, and lower back.")
    if profile.injuries:
        notes.append(f"Injury note supplied: {profile.injuries}. Avoid painful ranges and high-impact work.")
    if profile.sleep_hours is not None and profile.sleep_hours < 6:
        notes.append("Sleep is below 6 hours; reduce intensity today and keep one additional recovery day.")
    if profile.recovery_score is not None and profile.recovery_score < 45:
        notes.append("Recovery score is low; replace intense training with walking, mobility, or rest.")
    return notes


def classify_bmi(bmi: float) -> str:
    if bmi < 16:
        return "sever thinness"
    if bmi < 17:
        return "moderate thinness"
    if bmi < 18.5:
        return "mild thinness"
    if bmi < 25:
        return "normal"
    if bmi < 30:
        return "over weight"
    if bmi < 35:
        return "obese"
    return "severe obese"


def classify_bfp(value: float | None, gender: str) -> str | None:
    if value is None:
        return None
    male = gender.lower().startswith("m")
    if male:
        if value < 6:
            return "Essential"
        if value < 14:
            return "Athletes"
        if value < 18:
            return "Fitness"
        if value < 25:
            return "Acceptable"
        return "Obese"
    if value < 14:
        return "Essential"
    if value < 21:
        return "Athletes"
    if value < 25:
        return "Fitness"
    if value < 32:
        return "Acceptable"
    return "Obese"


def fallback_plan_from_bmi(bmi: float) -> str:
    if bmi < 16:
        return "1"
    if bmi < 17:
        return "2"
    if bmi < 18.5:
        return "3"
    if bmi < 25:
        return "4"
    if bmi < 30:
        return "5"
    if bmi < 35:
        return "6"
    return "7"


def normalize_level(value: str) -> str:
    value = value.strip().lower()
    if value in {"intermediate", "medium"}:
        return "Intermediate"
    if value in {"advanced", "elite"}:
        return "Advanced"
    return "Beginner"


def choose_intensity(profile: Profile, bmi_case: str, plan_code: str) -> str:
    if profile.recovery_score is not None and profile.recovery_score < 45:
        return "recovery"
    if plan_code in {"6", "7"} or "obese" in bmi_case or profile.fitness_level == "Beginner":
        return "low-to-moderate"
    if profile.fitness_level == "Advanced":
        return "moderate-to-high"
    return "moderate"


def recovery_factor_for(profile: Profile) -> float:
    factor = 1.0
    if profile.sleep_hours is not None and profile.sleep_hours < 6:
        factor -= 0.2
    if profile.recovery_score is not None:
        if profile.recovery_score < 45:
            factor -= 0.3
        elif profile.recovery_score > 75:
            factor += 0.1
    if profile.age >= 60:
        factor -= 0.1
    return max(0.55, min(1.1, factor))


def estimate_calories(activity: str, minutes: int) -> float:
    per_min = {
        "Walking": 4,
        "Yoga": 3,
        "Weight Training": 7,
        "Cycling": 9,
        "Swimming": 8,
        "Running": 10,
        "HIIT": 12,
        "CrossFit": 12,
    }.get(activity, 6)
    return per_min * minutes


def default_strain(activity: str) -> float:
    return {
        "Walking": 3,
        "Yoga": 4,
        "Weight Training": 9,
        "Cycling": 11,
        "Swimming": 11,
        "Running": 13,
        "HIIT": 15,
        "CrossFit": 15,
    }.get(activity, 7)


def warmup_details(activity: str) -> str:
    if activity in {"Running", "Walking"}:
        return "Easy walk, ankle circles, hip swings, gradual pace increase."
    if activity == "Weight Training":
        return "Joint circles, bodyweight squats, light hinge, easy push/pull rehearsal."
    return "Breathing, mobility, and easy full-body movement."


def main_details(activity: str, profile: Profile) -> str:
    if activity == "Weight Training":
        if profile.equipment == "gym":
            return "Goblet squat or leg press, row, chest press, Romanian deadlift, carry. 2-4 controlled sets."
        return "Squat to chair, incline push-up, hip hinge, backpack row, plank. 2-3 controlled sets."
    if activity == "Yoga":
        return "Gentle flow: cat-cow, lunge stretch, downward dog option, balance pose, breathing."
    if activity == "Walking":
        return "Brisk but conversational pace. Add short faster intervals only if joints feel good."
    if activity == "Cycling":
        return "Steady zone-2 ride with smooth cadence. Keep resistance comfortable."
    if activity == "Swimming":
        return "Easy laps or intervals with plenty of rest. Keep technique relaxed."
    if activity == "HIIT":
        return "Short intervals: 20-30 seconds work, 60-90 seconds easy. Stop before form breaks."
    if activity == "Running":
        return "Run/walk intervals. Keep effort controlled and avoid sprinting early."
    if activity == "CrossFit":
        return "Technique-first circuit with scaled movements and no max lifts."
    return "Controlled full-body session at the target effort."


def cooldown_details(activity: str) -> str:
    return "Slow breathing, easy movement, and gentle stretching for trained areas."


def target_effort(profile: Profile, activity: str) -> str:
    if profile.recovery_score is not None and profile.recovery_score < 45:
        return "RPE 3-4, recovery only"
    if activity in HIGH_INTENSITY:
        return "RPE 7-8, short intervals, never all-out"
    if activity in LOW_IMPACT:
        return "RPE 4-6, conversational effort"
    return "RPE 5-7, controlled form"
