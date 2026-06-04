"""Train the workout plan prediction machine-learning model.

Purpose:
    Train a scikit-learn classifier that predicts `Exercise Recommendation Plan`
    from user profile features. This turns the service from rule-only
    recommendation into a true ML-assisted recommender.

Inputs:
    storage/workout.db, specifically the `recommendation_cases` table created by
    scripts/ingest_datasets.py.

Outputs:
    storage/plan_model.joblib
    storage/plan_model_metrics.json

Model:
    RandomForestClassifier inside a preprocessing Pipeline:
    - numeric features: age, height, weight, BMI, body-fat percentage
    - categorical features: gender, BMI case, BFP case

Notes:
    Keep this script in scripts/ because it is an operational training utility.
    Re-run it whenever the dataset is updated or enough new feedback has been
    collected to build a stronger training set.
"""

from __future__ import annotations

import json
import sqlite3
from pathlib import Path

import joblib
from sklearn.ensemble import RandomForestClassifier
from sklearn.feature_extraction import DictVectorizer
from sklearn.metrics import accuracy_score, classification_report
from sklearn.model_selection import train_test_split
from sklearn.pipeline import Pipeline


ROOT = Path(__file__).resolve().parents[1]
DB_PATH = ROOT / "storage" / "workout.db"
MODEL_PATH = ROOT / "storage" / "plan_model.joblib"
METRICS_PATH = ROOT / "storage" / "plan_model_metrics.json"

NUMERIC_FEATURES = ["age", "height_cm", "weight_kg", "bmi", "body_fat_pct"]
CATEGORICAL_FEATURES = ["gender", "bmi_case", "bfp_case"]
FEATURES = NUMERIC_FEATURES + CATEGORICAL_FEATURES


def main() -> int:
    if not DB_PATH.exists():
        print("Missing storage/workout.db. Run scripts/ingest_datasets.py first.")
        return 1

    rows = load_training_rows()
    if len(rows) < 100:
        print(f"Not enough training rows: {len(rows)}")
        return 1

    x = [model_features(row) for row in rows]
    y = [str(row["plan_code"]) for row in rows]

    x_train, x_test, y_train, y_test = train_test_split(
        x,
        y,
        test_size=0.2,
        random_state=42,
        stratify=y,
    )

    model = build_pipeline()
    model.fit(x_train, y_train)
    pred = model.predict(x_test)

    metrics = {
        "rows": len(rows),
        "train_rows": len(x_train),
        "test_rows": len(x_test),
        "accuracy": accuracy_score(y_test, pred),
        "features": FEATURES,
        "classification_report": classification_report(y_test, pred, output_dict=True),
    }

    joblib.dump({"model": model, "features": FEATURES, "metrics": metrics}, MODEL_PATH)
    METRICS_PATH.write_text(json.dumps(metrics, indent=2), encoding="utf-8")

    print("Plan prediction model trained.")
    print(f"  rows     : {len(rows)}")
    print(f"  accuracy : {metrics['accuracy']:.4f}")
    print(f"  model    : {MODEL_PATH}")
    print(f"  metrics  : {METRICS_PATH}")
    return 0


def build_pipeline() -> Pipeline:
    classifier = RandomForestClassifier(
        n_estimators=220,
        max_depth=14,
        min_samples_leaf=3,
        random_state=42,
        class_weight="balanced",
        n_jobs=-1,
    )
    return Pipeline(
        steps=[
            ("vectorize", DictVectorizer(sparse=False)),
            ("classifier", classifier),
        ]
    )


def model_features(row: dict) -> dict:
    """Convert raw DB row into stable ML features."""
    features = {}
    for name in NUMERIC_FEATURES:
        value = row.get(name)
        features[name] = -1.0 if value is None else float(value)
    for name in CATEGORICAL_FEATURES:
        value = row.get(name)
        features[name] = str(value or "unknown")
    return features


def load_training_rows() -> list[dict]:
    with sqlite3.connect(DB_PATH) as conn:
        conn.row_factory = sqlite3.Row
        rows = conn.execute(
            """
            SELECT age, height_cm, weight_kg, bmi, body_fat_pct, gender, bmi_case, bfp_case, plan_code
            FROM recommendation_cases
            WHERE plan_code IS NOT NULL AND plan_code != ''
            """
        ).fetchall()
    return [dict(row) for row in rows]


if __name__ == "__main__":
    raise SystemExit(main())
