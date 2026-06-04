# AI-assistant-workout

`AI-assistant-workout`는 사용자의 프로필을 입력받아 개인 맞춤 운동 루틴을 만들어 주는 Python 백엔드 + 간단한 웹 대면부 프로젝트입니다.

이 프로젝트는 현재 `datas/` 폴더에 있는 여러 운동/건강 데이터셋을 활용합니다. 앞으로 손전화앱에서 사용자가 남기는 피드백도 저장하여 추천 품질을 계속 개선할 수 있게 설계했습니다.

## 1. 서비스 목표

사용자가 가입할 때 이런 정보를 입력합니다.

- 나이
- 성별
- 키
- 몸무게
- 체지방률
- 운동 수준
- 운동 목표
- 주당 운동 가능 일수
- 하루 운동 가능 시간
- 장비
- 부상/제한 사항
- 수면 시간
- 회복 점수

서버는 이 정보를 바탕으로 다음을 만듭니다.

- BMI와 체형 분류
- 데이터셋에서 비슷한 사람들의 추천 코드 검색
- 목표와 운동 수준에 맞는 활동 선택
- 주간 운동 루틴
- 세션별 준비운동, 본운동, 정리운동
- 예상 칼로리
- 안전 주의사항
- 추천에 쓰인 데이터 근거

## 2. 데이터셋을 어떻게 쓰는가

현재 `datas/` 폴더에는 여러 CSV 데이터셋이 있습니다.

### `final_dataset.csv`

프로필과 운동 추천 코드가 들어 있습니다.

사용처:

- 키, 몸무게, BMI, 나이, 성별 기준으로 비슷한 사용자 찾기
- 해당 사용자들에게 주어진 운동 추천 코드를 참고하기

### `final_dataset_BFP .csv`

체지방률까지 포함된 추천 데이터셋입니다.

사용처:

- 체지방률이 입력된 사용자의 추천 정확도 향상
- BMI만으로 부족한 체형 판단 보완

### `daily_gym_attendance_workout_data.csv`

헬스장 출석, 운동 종류, 운동 시간, 칼로리 데이터입니다.

사용처:

- 운동 종류별 평균 운동 시간 확인
- 운동 종류별 평균 칼로리 확인
- 현실적인 세션 길이 설정

### Fitbit export 데이터

걸음 수, 활동 시간, 수면 기록 같은 활동량 데이터입니다.

사용처:

- 일반 사용자의 활동량 기준 이해
- 앞으로 손전화앱 활동 추적 기능과 연결 가능

### `whoop_fitness_dataset_100k.csv`

수면, 회복 점수, 운동 강도, 운동 종류, 칼로리, 심박 관련 데이터가 들어 있습니다.

사용처:

- 운동 수준별 활동 추천
- 회복 점수와 수면 시간에 따른 운동 강도 조절
- 운동 종류별 평균 duration, calories, strain 계산

## 3. 폴더 구조

```text
AI-assistant-workout/
  app/
    main.py
      HTTP API 서버입니다.

    services/
      recommender.py
        운동 추천 핵심 로직입니다.

  datas/
    원본 CSV 데이터셋입니다.

  frontend/
    index.html
    styles.css
    app.js
      사용자가 프로필을 입력하는 간단한 웹 화면입니다.

  scripts/
    ingest_datasets.py
      원본 CSV 데이터셋을 읽고 SQLite DB를 만듭니다.

    train_plan_model.py
      scikit-learn 머신러닝 모델을 학습합니다.

    run_server.py
      서버를 실행합니다.

  storage/
    workout.db
      생성된 서비스 데이터베이스입니다.
```

## 4. 처음 실행하기

PowerShell에서 프로젝트 폴더로 이동합니다.

```powershell
cd E:\Github\Mobile\Mobile-App\AI-assistant-workout
```

현재는 외부 Python 패키지가 필요 없습니다. 그래도 나중 배포를 위해 아래 명령은 실행해도 됩니다.

```powershell
python -m pip install -r requirements.txt
```

데이터셋을 서비스 DB로 변환합니다.

```powershell
python scripts\ingest_datasets.py
```

운동 추천 코드 예측 머신러닝 모델을 학습합니다.

```powershell
python scripts\train_plan_model.py
```

서버를 실행합니다.

```powershell
python scripts\run_server.py
```

브라우저에서 엽니다.

```text
http://127.0.0.1:8020
```

## 5. API

### 상태 확인

```text
GET /api/health
```

응답 예:

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

### 운동 루틴 생성

```text
POST /api/routine
```

요청 예:

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

### 피드백 저장

```text
POST /api/feedback
```

요청 예:

```json
{
  "plan_id": 1,
  "rating": 4,
  "difficulty": "Good",
  "completed": true,
  "notes": "Walking day was good, HIIT was too hard."
}
```

## 6. 앞으로 피드백을 어떻게 활용할 것인가

현재 피드백은 `feedback` 테이블에 저장됩니다.

저장되는 정보:

- 어떤 plan에 대한 피드백인지
- 사용자가 준 평점
- 난이도 느낌
- 완료 여부
- 사용자 메모
- 당시 프로필
- 당시 루틴 JSON

다음 단계에서는 이 피드백을 이용해 다음을 할 수 있습니다.

- 특정 사용자군에서 너무 어렵다고 평가된 운동 강도 낮추기
- 완료율이 높은 운동 종류를 더 많이 추천하기
- 평점이 낮은 루틴 패턴 줄이기
- 앱 사용자 데이터를 기반으로 개인화 모델 학습하기

## 7. 추천 로직 요약

```text
1. 사용자 프로필을 받습니다.
2. BMI와 체지방 분류를 계산합니다.
3. scikit-learn ML 모델이 추천 plan code를 예측합니다.
4. 데이터셋에서 비슷한 사용자 사례를 찾습니다.
5. ML 예측과 유사사례 투표를 섞어 최종 plan code를 선택합니다.
6. 목표와 운동 수준에 맞는 운동 종류를 고릅니다.
7. 저장된 사용자 피드백으로 운동 순위와 시간을 조절합니다.
8. WHOOP/gym 데이터 평균으로 운동 시간과 칼로리를 조정합니다.
9. 수면, 회복 점수, 부상, 나이에 따라 강도를 낮추거나 조절합니다.
10. 주간 운동 루틴을 만듭니다.
11. 결과와 근거를 DB에 저장합니다.
12. 사용자가 남긴 피드백을 저장합니다.
```

## 8. 주의

이 서비스는 운동 루틴 추천 서비스이지 의학 진단 서비스가 아닙니다.

다음 경우에는 반드시 전문가 상담이 필요합니다.

- 심장 질환
- 심한 관절 통증
- 임신
- 수술 후 회복 중
- 어지러움 또는 흉통
- 의사가 운동 제한을 준 경우

## 9. 현재 완성된 것

- 데이터셋 ingest 스크립트
- scikit-learn plan prediction 머신러닝 모델
- SQLite 데이터베이스
- 개인 프로필 기반 운동 추천 엔진
- Python HTTP API 서버
- 간단한 웹 대면부
- 사용자 피드백 저장 및 추천 반영 구조
- 앞으로 모바일앱과 연결 가능한 JSON API
