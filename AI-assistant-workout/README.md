# AI-assistant-workout

`AI-assistant-workout`takes the user's profile and creates a personalized exercise routine. Python backend + This is a simple web face-to-face project..

This project is currently `datas/` Several exercises in a folder/Utilizing health datasets. In the future, we have designed the mobile phone app to store feedback left by users so that we can continue to improve the quality of recommendations..

## 1. Service Goals

Users enter this information when signing up..

- age
- gender
- key
- weight
- body fat percentage
- exercise level
- exercise goals
- Number of days you can exercise per week
- Time available for exercise per day
- equipment
- injury/Restrictions
- sleep time
- recovery score

Based on this information, the server creates the following:.

- BMIand body type classification
- Search for recommendation codes from similar people in your dataset
- Choose activities that fit your goals and exercise level
- weekly exercise routine
- Warm-up for each session, main movement, clean-up exercise
- estimated calories
- Safety precautions
- Data basis for recommendations

## 2. How to use datasets

present `datas/` There are several folders CSV I have a dataset.

### `final_dataset.csv`

Contains profile and exercise recommendation code.

Where to use:

- key, weight, BMI, age, Find similar users by gender
- Refer to the exercise recommendation code given to these users.

### `final_dataset_BFP .csv`

This is a recommended dataset that includes body fat percentage..

Where to use:

- Improved recommendation accuracy for users with body fat percentage input
- BMICompensate for insufficient body type judgment

### `daily_gym_attendance_workout_data.csv`

gym attendance, exercise type, exercise time, Calorie data.

Where to use:

- Check average exercise time by exercise type
- Check average calories by exercise type
- Set realistic session lengths

### Fitbit export data

steps, activity time, Activity data such as sleep records.

Where to use:

- Understand the activity level standards of general users
- Can be connected to mobile phone app activity tracking function in the future

### `whoop_fitness_dataset_100k.csv`

sleep, recovery score, exercise intensity, exercise type, calories, Contains heart rate related data.

Where to use:

- Activity recommendations for each exercise level
- Adjust exercise intensity according to recovery score and sleep time
- Average by exercise type duration, calories, strain calculation

## 3. folder structure

```text
AI-assistant-workout/
  app/
    main.py
      HTTP API This is the server.

    services/
      recommender.py
        This is the core logic of exercise recommendation..

  datas/
    original CSV This is a dataset.

  frontend/
    index.html
    styles.css
    app.js
      This is a simple web screen where the user enters their profile..

  scripts/
    ingest_datasets.py
      original CSV read the dataset SQLite DBcreates.

    train_plan_model.py
      scikit-learn Train a machine learning model.

    run_server.py
      Run the server.

  storage/
    workout.db
      This is the created service database..
```

## 4. Run it for the first time

PowerShellGo to your project folder in.

```powershell
cd E:\Github\Mobile\Mobile-App\AI-assistant-workout
```

Currently outside Python No package needed. Still, you can run the command below for later deployment..

```powershell
python -m pip install -r requirements.txt
```

Service the dataset DBConvert to.

```powershell
python scripts\ingest_datasets.py
```

Train exercise recommendation code prediction machine learning model.

```powershell
python scripts\train_plan_model.py
```

Run the server.

```powershell
python scripts\run_server.py
```

Open in browser.

```text
http://127.0.0.1:8020
```

## 5. API

### Check status

```text
GET /api/health
```

Example response:

```json
{
  "ok": true,
  "database_ready": true,
  "feedback": {
    "count": 0,
    "avg_rating": 0
  }
}
```

### Create an exercise routine

```text
POST /api/routine
```

Request example:

```json
{
  "age": 32,
  "gender": "Male",
  "height_cm": 175,
  "weight_kg": 82,
  "body_fat_pct": 24,
  "fitness_level": "Beginner",
  "goal": "fat_loss",
  "days_per_week": 4,
  "session_minutes": 45,
  "equipment": "bodyweight",
  "injuries": "knee pain",
  "sleep_hours": 6.5,
  "recovery_score": 62
}
```

### Save Feedback

```text
POST /api/feedback
```

Request example:

```json
{
  "plan_id": 1,
  "rating": 4,
  "difficulty": "Good",
  "completed": true,
  "notes": "Walking day was good, HIIT was too hard."
}
```

## 6. How will you use feedback in the future?

Current feedback is `feedback` stored in table.

Information stored:

- which planIs it feedback on
- Ratings given by users
- Difficulty feeling
- Completed or not
- user notes
- Profile at the time
- routine at the time JSON

In the next steps, you can use this feedback to:.

- Reduce the intensity of exercises that are rated as too difficult for certain user groups
- Recommend more workout types with high completion rates
- Reduce low-rated routine patterns
- Train a personalization model based on app user data

## 7. Recommendation logic summary

```text
1. Get user profile.
2. BMICalculate body fat classification with.
3. scikit-learn ML model is recommended plan codepredicts.
4. Find similar user stories in your dataset.
5. ML The final result is a mixture of predictions and similar case votes. plan codeSelect.
6. Choose the type of exercise that suits your goals and exercise level.
7. Adjust exercise ranking and time with saved user feedback.
8. WHOOP/gym Adjust exercise time and calories by averaging data.
9. sleep, recovery score, injury, Reduce or adjust intensity based on age.
10. Create a weekly exercise routine.
11. results and evidence DBSave it to.
12. Saves feedback left by users.
```

## 8. caution

This service is an exercise routine recommendation service, not a medical diagnosis service..

Professional consultation is required in the following cases:.

- heart disease
- severe joint pain
- pregnancy
- recovering after surgery
- Dizziness or chest pain
- If your doctor gives you exercise restrictions

## 9. currently completed

- dataset ingest script
- scikit-learn plan prediction machine learning model
- SQLite database
- Personal profile-based exercise recommendation engine
- Python HTTP API server
- Simple web interface
- Structure for storing user feedback and reflecting recommendations
- Possible to connect with mobile app in the future JSON API
