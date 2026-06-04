"""Build the workout service database from local datasets.

Purpose:
    Convert the CSV files in `datas/` into a compact SQLite database that the
    backend can use quickly at runtime.

Inputs:
    datas/final_dataset.csv
    datas/final_dataset_BFP .csv
    datas/daily_gym_attendance_workout_data.csv
    datas/whoop_fitness_dataset_100k.csv
    datas/**/dailyActivity_merged.csv
    datas/**/sleepDay_merged.csv

Outputs:
    storage/workout.db

How it is used:
    Run this script whenever datasets are added or changed. User feedback is
    stored in separate tables and is not erased by this script unless the whole
    database file is manually deleted.
"""

from __future__ import annotations

import csv
import json
import sqlite3
from collections import Counter, defaultdict
from pathlib import Path


ROOT = Path(__file__).resolve().parents[1]
DATA_DIR = ROOT / "datas"
DB_PATH = ROOT / "storage" / "workout.db"


PLAN_DESCRIPTIONS = {
    "1": "Very gentle weight-gain and mobility plan for severely underweight users.",
    "2": "Gentle strength foundation plan for moderately underweight users.",
    "3": "Balanced beginner plan for mildly underweight users.",
    "4": "General fitness maintenance plan for normal BMI users.",
    "5": "Fat-loss and conditioning plan for overweight users.",
    "6": "Low-impact fat-loss plan for obese users.",
    "7": "Medical-caution low-impact plan for severe obesity users.",
}


def main() -> int:
    DB_PATH.parent.mkdir(parents=True, exist_ok=True)
    conn = sqlite3.connect(DB_PATH)
    conn.row_factory = sqlite3.Row
    create_schema(conn)
    clear_dataset_tables(conn)

    recommendation_count = ingest_recommendation_cases(conn)
    gym_count = ingest_gym_stats(conn)
    whoop_count = ingest_whoop_stats(conn)
    fitbit_count = ingest_fitbit_summaries(conn)
    insert_plan_templates(conn)

    conn.commit()
    conn.close()

    print("Workout database built.")
    print(f"  recommendation cases : {recommendation_count}")
    print(f"  gym workout stats    : {gym_count}")
    print(f"  whoop activity stats : {whoop_count}")
    print(f"  fitbit summaries     : {fitbit_count}")
    print(f"  database             : {DB_PATH}")
    return 0


def create_schema(conn: sqlite3.Connection) -> None:
    """Create dataset, runtime, and feedback tables."""
    conn.executescript(
        """
        CREATE TABLE IF NOT EXISTS recommendation_cases (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            weight_kg REAL,
            height_cm REAL,
            bmi REAL,
            body_fat_pct REAL,
            gender TEXT,
            age INTEGER,
            bmi_case TEXT,
            bfp_case TEXT,
            plan_code TEXT,
            source TEXT
        );

        CREATE TABLE IF NOT EXISTS gym_workout_stats (
            workout_type TEXT PRIMARY KEY,
            samples INTEGER NOT NULL,
            avg_duration_min REAL NOT NULL,
            avg_calories REAL NOT NULL,
            attendance_rate REAL NOT NULL
        );

        CREATE TABLE IF NOT EXISTS whoop_activity_stats (
            fitness_level TEXT NOT NULL,
            activity_type TEXT NOT NULL,
            samples INTEGER NOT NULL,
            avg_duration_min REAL NOT NULL,
            avg_calories REAL NOT NULL,
            avg_strain REAL NOT NULL,
            avg_recovery REAL NOT NULL,
            PRIMARY KEY (fitness_level, activity_type)
        );

        CREATE TABLE IF NOT EXISTS fitbit_daily_summary (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            source_file TEXT NOT NULL,
            users INTEGER NOT NULL,
            days INTEGER NOT NULL,
            avg_steps REAL NOT NULL,
            avg_very_active_min REAL NOT NULL,
            avg_fairly_active_min REAL NOT NULL,
            avg_lightly_active_min REAL NOT NULL,
            avg_sedentary_min REAL NOT NULL
        );

        CREATE TABLE IF NOT EXISTS plan_templates (
            plan_code TEXT PRIMARY KEY,
            description TEXT NOT NULL
        );

        CREATE TABLE IF NOT EXISTS users (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            created_at TEXT DEFAULT CURRENT_TIMESTAMP,
            profile_json TEXT NOT NULL
        );

        CREATE TABLE IF NOT EXISTS routine_plans (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            user_id INTEGER,
            created_at TEXT DEFAULT CURRENT_TIMESTAMP,
            profile_json TEXT NOT NULL,
            routine_json TEXT NOT NULL,
            FOREIGN KEY (user_id) REFERENCES users(id)
        );

        CREATE TABLE IF NOT EXISTS feedback (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            plan_id INTEGER,
            created_at TEXT DEFAULT CURRENT_TIMESTAMP,
            rating INTEGER,
            difficulty TEXT,
            completed INTEGER,
            notes TEXT,
            profile_json TEXT,
            routine_json TEXT,
            FOREIGN KEY (plan_id) REFERENCES routine_plans(id)
        );
        """
    )


def clear_dataset_tables(conn: sqlite3.Connection) -> None:
    """Clear derived dataset tables while preserving users, plans, and feedback."""
    for table in (
        "recommendation_cases",
        "gym_workout_stats",
        "whoop_activity_stats",
        "fitbit_daily_summary",
        "plan_templates",
    ):
        conn.execute(f"DELETE FROM {table}")


def ingest_recommendation_cases(conn: sqlite3.Connection) -> int:
    """Import profile-to-plan recommendation cases."""
    count = 0
    for filename in ("final_dataset.csv", "final_dataset_BFP .csv"):
        path = DATA_DIR / filename
        if not path.exists():
            continue
        with path.open("r", encoding="utf-8-sig", errors="replace", newline="") as handle:
            reader = csv.DictReader(handle)
            for row in reader:
                height_raw = to_float(row.get("Height"))
                height_cm = height_raw * 100 if height_raw and height_raw < 3 else height_raw
                conn.execute(
                    """
                    INSERT INTO recommendation_cases
                    (weight_kg, height_cm, bmi, body_fat_pct, gender, age, bmi_case, bfp_case, plan_code, source)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                    """,
                    (
                        to_float(row.get("Weight")),
                        height_cm,
                        to_float(row.get("BMI")),
                        to_float(row.get("Body Fat Percentage")),
                        clean_text(row.get("Gender")),
                        to_int(row.get("Age")),
                        clean_text(row.get("BMIcase")),
                        clean_text(row.get("BFPcase")),
                        clean_text(row.get("Exercise Recommendation Plan")),
                        filename,
                    ),
                )
                count += 1
    return count


def ingest_gym_stats(conn: sqlite3.Connection) -> int:
    """Aggregate gym workout type duration, calories, and attendance."""
    path = DATA_DIR / "daily_gym_attendance_workout_data.csv"
    if not path.exists():
        return 0
    stats = defaultdict(lambda: {"n": 0, "duration": 0.0, "calories": 0.0, "present": 0})
    with path.open("r", encoding="utf-8-sig", errors="replace", newline="") as handle:
        reader = csv.DictReader(handle)
        for row in reader:
            workout = clean_text(row.get("workout_type")) or "General"
            item = stats[workout]
            item["n"] += 1
            item["duration"] += to_float(row.get("workout_duration_minutes")) or 0
            item["calories"] += to_float(row.get("calories_burned")) or 0
            if clean_text(row.get("attendance_status")).lower() in {"present", "attended", "checked in"}:
                item["present"] += 1
    for workout, item in stats.items():
        n = max(1, item["n"])
        conn.execute(
            """
            INSERT INTO gym_workout_stats
            (workout_type, samples, avg_duration_min, avg_calories, attendance_rate)
            VALUES (?, ?, ?, ?, ?)
            """,
            (workout, item["n"], item["duration"] / n, item["calories"] / n, item["present"] / n),
        )
    return len(stats)


def ingest_whoop_stats(conn: sqlite3.Connection) -> int:
    """Aggregate recovery-aware activity statistics from WHOOP-like data."""
    path = DATA_DIR / "whoop_fitness_dataset_100k.csv"
    if not path.exists():
        return 0
    stats = defaultdict(lambda: {"n": 0, "duration": 0.0, "calories": 0.0, "strain": 0.0, "recovery": 0.0})
    with path.open("r", encoding="utf-8-sig", errors="replace", newline="") as handle:
        reader = csv.DictReader(handle)
        for row in reader:
            if clean_text(row.get("workout_completed")) != "1":
                continue
            level = clean_text(row.get("fitness_level")) or "Beginner"
            activity = clean_text(row.get("activity_type")) or "General"
            if activity.lower() == "rest day":
                continue
            item = stats[(level, activity)]
            item["n"] += 1
            item["duration"] += to_float(row.get("activity_duration_min")) or 0
            item["calories"] += to_float(row.get("activity_calories")) or 0
            item["strain"] += to_float(row.get("activity_strain")) or 0
            item["recovery"] += to_float(row.get("recovery_score")) or 0
    for (level, activity), item in stats.items():
        n = max(1, item["n"])
        conn.execute(
            """
            INSERT INTO whoop_activity_stats
            (fitness_level, activity_type, samples, avg_duration_min, avg_calories, avg_strain, avg_recovery)
            VALUES (?, ?, ?, ?, ?, ?, ?)
            """,
            (level, activity, item["n"], item["duration"] / n, item["calories"] / n, item["strain"] / n, item["recovery"] / n),
        )
    return len(stats)


def ingest_fitbit_summaries(conn: sqlite3.Connection) -> int:
    """Store compact summaries from Fitbit daily activity files."""
    count = 0
    for path in DATA_DIR.rglob("dailyActivity_merged.csv"):
        totals = Counter()
        users = set()
        days = 0
        with path.open("r", encoding="utf-8-sig", errors="replace", newline="") as handle:
            reader = csv.DictReader(handle)
            for row in reader:
                days += 1
                users.add(row.get("Id", ""))
                totals["steps"] += to_float(row.get("TotalSteps")) or 0
                totals["very"] += to_float(row.get("VeryActiveMinutes")) or 0
                totals["fair"] += to_float(row.get("FairlyActiveMinutes")) or 0
                totals["light"] += to_float(row.get("LightlyActiveMinutes")) or 0
                totals["sed"] += to_float(row.get("SedentaryMinutes")) or 0
        n = max(1, days)
        conn.execute(
            """
            INSERT INTO fitbit_daily_summary
            (source_file, users, days, avg_steps, avg_very_active_min, avg_fairly_active_min, avg_lightly_active_min, avg_sedentary_min)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?)
            """,
            (str(path.relative_to(ROOT)), len(users), days, totals["steps"] / n, totals["very"] / n, totals["fair"] / n, totals["light"] / n, totals["sed"] / n),
        )
        count += 1
    return count


def insert_plan_templates(conn: sqlite3.Connection) -> None:
    for code, description in PLAN_DESCRIPTIONS.items():
        conn.execute(
            "INSERT INTO plan_templates (plan_code, description) VALUES (?, ?)",
            (code, description),
        )


def to_float(value: str | None) -> float | None:
    try:
        if value is None or value == "":
            return None
        return float(value)
    except ValueError:
        return None


def to_int(value: str | None) -> int | None:
    number = to_float(value)
    return int(number) if number is not None else None


def clean_text(value: str | None) -> str:
    return " ".join(str(value or "").strip().split())


if __name__ == "__main__":
    raise SystemExit(main())
