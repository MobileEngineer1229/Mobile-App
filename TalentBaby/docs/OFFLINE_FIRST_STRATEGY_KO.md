# TalentBaby Offline-First Strategy

## 목표

TalentBaby 모바일 앱은 backend 서버가 없어도 핵심 기능이 동작해야 한다. 정적 컨텐츠는 앱 번들에 포함하고, 사용자 입력 데이터는 기기 내부 SQLite에 저장한다. 이후 서버가 다시 필요해지는 경우에도 같은 Repository 인터페이스 위에서 온라인 동기화를 붙일 수 있게 만든다.

## 현재 구조 요약

- Android 앱은 Java 기반 네이티브 앱이다.
- `mobile/app/src/main/java/com/talentbaby/app/network/ApiService.java`에 Retrofit API가 정의되어 있다.
- 여러 화면이 `ApiClient.getClient().create(ApiService.class)`를 직접 호출한다.
- 현재 서버 주소는 `mobile/app/src/main/java/com/talentbaby/app/utils/ApiClient.java`의 `http://10.0.2.2:3004/api/v1/`에 고정되어 있다.
- 백엔드는 Express + PostgreSQL + Prisma 구조이며, 시드 데이터는 `backend/database/migrations`와 `backend/database/generated-ui-*.json`에 있다.
- 이미지 원천은 크게 두 곳이다.
  - 모바일 번들: `mobile/app/src/main/res/drawable-nodpi`, 약 44개, 약 9MB
  - 백엔드 public assets: `backend/public/images`, 약 486개, 약 40MB
- 일부 데모 데이터는 이미 앱 내부 Java 코드로 존재한다.
  - `mobile/app/src/main/java/com/talentbaby/app/data/NutritionDemoData.java`
  - `mobile/app/src/main/java/com/talentbaby/app/data/StoryDemoData.java`

## 결론

권장 구조는 "SQLite + bundled assets" 혼합 방식이다.

SQLite에는 검색, 필터, 관계, 사용자 기록에 필요한 구조화 데이터를 넣는다. 이미지, 오디오, 비디오 같은 큰 바이너리는 SQLite에 넣지 않고 `assets/` 또는 `res/drawable-nodpi`에 파일로 둔다. SQLite에는 `asset_path` 또는 `image_asset_name`만 저장한다.

## 두 비전 동시 지원

이 프로젝트는 BabyCenter식 컨텐츠/트래커 비전과 Kinedu식 활동/마일스톤/데일리 플랜 비전을 동시에 가져가야 한다.

이를 위해 데이터 모델을 두 갈래로 나누되 같은 로컬 저장소에서 관리한다.

1. BabyCenter 계열
   - articles
   - stories
   - nutrition guides
   - recipes
   - growth records
   - feeding, sleep, diaper tracker
   - pregnancy/checklist/calculator 계열, 필요 시 확장

2. Kinedu 계열
   - activities
   - daily activities
   - milestone definitions
   - baby milestones
   - talent categories
   - talent assessments
   - activity completion history
   - milestone/activity video attachment metadata

앱 화면은 두 비전을 별도 DB로 나누지 말고, 하나의 `LocalTalentBabyDatabase`에서 Repository별로 읽게 한다. 그래야 홈, 데일리 플랜, 마일스톤, 기사, 영양 화면이 같은 아기 프로필과 나이 계산을 공유할 수 있다.

## 데이터 저장 원칙

### SQLite에 넣을 것

- user profile, local auth placeholder
- babies, active baby
- growth_records
- feeding_records
- sleep_sessions
- diaper_changes
- activities
- daily_activities
- activity_completion
- milestone_definitions
- baby_milestones
- articles
- stories
- recipes
- nutrition_categories
- nutrition_foods
- recipe_ingredients
- bookmarks/favorites
- local media metadata

### assets 또는 res에 넣을 것

- activity thumbnails
- nutrition/recipe images
- story images
- onboarding/home/library images
- bundled video thumbnails
- short offline videos, 필요 시 `assets/videos/`
- prebuilt seed JSON 또는 SQLite DB 파일

### SQLite에 넣지 말 것

- 큰 이미지 blob
- 동영상 blob
- 오디오 blob
- 임시 캐시 파일

## Android 구현 방향

### 1단계: 로컬 DB 계층 추가

`app/build.gradle`에 Room을 추가한다.

```gradle
implementation 'androidx.room:room-runtime:2.6.1'
annotationProcessor 'androidx.room:room-compiler:2.6.1'
```

권장 패키지 구조:

```text
mobile/app/src/main/java/com/talentbaby/app/local/
  LocalTalentBabyDatabase.java
  dao/
  entity/
  mapper/
  seed/
  repository/
```

### 2단계: API 직접 호출 제거

화면에서 `ApiService`를 직접 쓰지 않게 한다. 예를 들어:

```text
ActivityRepository
BabyRepository
TrackerRepository
MilestoneRepository
ContentRepository
NutritionRepository
StoryRepository
```

Repository가 내부에서 Local DB를 읽는다. 나중에 서버 동기화가 필요하면 Repository 안에 RemoteDataSource를 추가하면 된다.

### 3단계: 백엔드 시드 데이터 변환

현재 백엔드 마이그레이션 SQL과 JSON을 앱용 seed로 변환한다.

권장 산출물:

```text
mobile/app/src/main/assets/offline/talent_baby_seed.db
mobile/app/src/main/assets/offline/manifest.json
mobile/app/src/main/assets/images/activities/*.png
mobile/app/src/main/assets/images/nutrition/**/*.png
```

처음 앱 실행 시 Room prepackaged database로 복사하거나, JSON seed를 읽어 insert한다. 데이터가 많고 관계가 있으므로 최종적으로는 prepackaged SQLite DB가 더 안정적이다.

### 4단계: 이미지 경로 통일

현재 백엔드 데이터의 `image_url`은 `/images/activities/xxx.png` 같은 서버 경로다. 오프라인에서는 다음처럼 변환한다.

```text
/images/activities/Balancing.png
assets://images/activities/Balancing.png
```

또는 SQLite에는 상대 경로만 둔다.

```text
images/activities/Balancing.png
```

Glide 로딩 유틸을 하나 만들어 서버 URL 조립 코드를 제거한다.

```text
ImageLoader.load(imageView, imageRef)
```

이 유틸이 `localImageResId`, `assets/`, `file://`, `http://`를 모두 처리한다.

### 5단계: 로컬 로그인 모드

완전 오프라인 앱에서는 JWT 로그인이 의미가 약해진다. 다음 방식이 현실적이다.

- 첫 실행: 부모 이름, 이메일 또는 닉네임만 받아 local user 생성
- 비밀번호 로그인은 선택 기능으로 둔다
- 기존 `TokenManager`는 `SessionManager`로 역할을 줄인다
- active baby는 SQLite 또는 SharedPreferences에 저장한다

### 6단계: Daily Plan 로컬 알고리즘

서버 없이도 데일리 플랜은 만들 수 있다.

입력:

- active baby age in months
- 완료한 activities
- milestone 상태
- 선호 카테고리 또는 목표
- 날짜

규칙:

- 해당 월령의 activity 후보를 가져온다
- 최근 7일 완료한 활동은 낮은 우선순위로 둔다
- physical, cognitive, communication, social-emotional 균형을 맞춘다
- 하루 3-5개 slot을 생성한다
- `baby_daily_activities`에 저장해 같은 날짜에는 같은 계획을 보여준다

## 우선순위

### Phase 1: 앱이 backend 없이 켜지고 주요 탭이 보이게 만들기

- Room DB 추가
- prepackaged seed DB 또는 JSON seed 추가
- local user / baby 생성
- articles, stories, recipes, activities, milestones를 로컬에서 읽기
- 이미지 asset 경로 로딩
- 로그인/회원가입을 오프라인 onboarding으로 대체

### Phase 2: 사용자 기록 저장

- growth
- feeding
- sleep
- diaper
- activity completion
- baby milestone status
- bookmarks/favorites

### Phase 3: 로컬 개인화

- daily plan generation
- milestone report
- growth chart summary
- nutrition recommendation
- talent assessment scoring

### Phase 4: 선택적 동기화

서버를 완전히 버리지 않고 나중에 살릴 가능성이 있으면 sync queue를 둔다.

- local_changes table
- sync_status: pending, synced, conflict
- updated_at, deleted_at
- stable uuid primary key

## 구현상 주의점

- PostgreSQL 배열 타입은 SQLite에서 별도 join table 또는 JSON text로 바꾼다.
- 서버의 `SERIAL id`는 로컬에서 충돌 가능성이 있으므로 장기적으로 UUID를 같이 둔다.
- 이미지 파일을 모두 APK에 넣으면 APK가 커진다. 현재 백엔드 public 이미지만 약 40MB이므로 압축, WebP 변환, feature pack 분리 여부를 검토한다.
- 사용자 촬영 동영상은 앱 assets가 아니라 app-private storage에 저장하고 SQLite에는 파일 경로와 메타데이터만 둔다.
- 웹 어드민은 오프라인 앱 런타임에는 필요 없다. 다만 컨텐츠 제작 도구로 유지하고, export script로 SQLite seed를 만드는 방식이 좋다.

## 추천 결정

지금 바로 갈 방향은 다음이 가장 좋다.

1. 백엔드는 컨텐츠 제작/관리용으로 유지한다.
2. 모바일 앱 런타임은 backend 없이 동작하도록 Room + bundled assets로 전환한다.
3. `ApiService`를 화면에서 직접 호출하는 구조를 Repository 계층으로 감싼다.
4. 모든 정적 컨텐츠는 앱 seed DB와 assets로 포함한다.
5. 사용자 데이터는 SQLite에 저장한다.
6. 향후 온라인 기능은 선택적 sync로 붙인다.

이렇게 하면 BabyCenter식 정보/트래커와 Kinedu식 활동/데일리 플랜을 한 앱 안에서 모두 살리면서도, 서버 없는 배포가 가능하다.

## 구현된 Delta Sync 골격

이번 단계에서 backend에는 공용 delta sync API를 추가했다.

```text
GET /api/v1/content-sync/manifest
GET /api/v1/content-sync/delta?since=<iso_timestamp>&include_assets=true
```

`manifest`는 서버의 현재 컨텐츠 판본과 asset 판본을 따로 돌려준다. `delta`는 앱이 가진 `since` 판본 이후 변경된 rows와 `asset_since` 판본 이후 변경된 이미지 asset 목록을 돌려준다.

Android 앱에는 다음 로컬 저장소를 추가했다.

```text
talent_baby_offline.db
  offline_content_rows
  offline_assets
  offline_sync_state
```

앱 시작 시 네트워크가 있으면 `ContentSyncManager`가 서버의 `contentVersion`, `assetVersion`을 로컬 판본과 각각 비교한다. 서버 판본이 더 높을 때만 해당 rows 또는 assets를 내려받고, 네트워크가 없으면 이 과정은 조용히 건너뛰므로 offline 사용자도 앱 실행이 막히지 않는다.

현재 이 단계는 "자료를 받기 위한 기반"이다. 다음 단계에서는 각 화면의 Retrofit 직접 호출을 Repository로 감싸고, 먼저 `offline_content_rows`를 읽은 뒤 online 기능이 필요한 경우에만 backend API를 호출하도록 바꾸면 된다.
