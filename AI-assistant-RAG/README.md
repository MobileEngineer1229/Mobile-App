# AI-assistant-RAG

`AI-assistant-RAG`는 자기 콤퓨터 안의 문서들을 읽고, 그 문서 내용에 근거하여 질문에 답하는 오프라인 AI 챗봇입니다.

인터넷 API나 외부 서버에 질문을 보내지 않고, 프로젝트 폴더 안에 저장된 모델과 자료기지를 사용합니다.

## 1. 이 프로젝트가 하는 일

사용 흐름은 간단합니다.

```text
1. documents 폴더에 문서를 넣습니다.
2. 색인 스크립트를 실행합니다.
3. 문서가 작은 조각으로 나뉘고 벡토르자료기지에 저장됩니다.
4. 웹 화면에서 질문합니다.
5. AI가 관련 문서를 찾고, 그 근거를 바탕으로 답합니다.
```

현재 들어간 검색/답변 기술:

- Dense embedding retrieval: 문장의 의미를 벡토르로 바꾸어 검색합니다.
- BM25 retrieval: 키워드가 정확히 맞는 문서를 찾습니다.
- Hybrid search: 의미 검색과 키워드 검색을 함께 씁니다.
- HyDE-style expansion: 질문과 가까운 근거를 확장해 더 잘 검색합니다.
- Graph-style expansion: 문서 안에서 같이 나오는 단어 관계를 이용합니다.
- Cross-encoder reranking: 찾은 후보 문서들을 다시 정밀하게 순위 매깁니다.
- Local generator: 로컬 LLM으로 답변을 만들되, 근거가 약하면 안전하게 근거문 추출 답변으로 돌아갑니다.

## 2. 폴더 설명

```text
AI-assistant-RAG/
  app/
    main.py
      웹 서버와 API가 들어 있습니다.

    rag/
      models.py
        로컬 모델을 불러옵니다.

      store.py
        벡토르자료기지 검색, BM25, graph expansion, reranking 로직입니다.

      text.py
        문서 읽기, 문서 자르기, 토큰화 로직입니다.

  documents/
    여기에 사용자가 문서를 넣습니다.

  frontend/
    브라우저에서 보는 채팅 화면입니다.

  models/
    다운로드된 AI 모델들이 저장됩니다.
    처음 설치할 때 자동으로 만들어집니다.

  scripts/
    download_models.py
      오프라인 실행에 필요한 모델을 models 폴더에 다운로드합니다.

    ingest_documents.py
      documents 폴더의 문서를 읽어 벡토르자료기지를 만듭니다.

    run_server.py
      웹 서버를 실행합니다.

  storage/
    vector_store.db
      생성된 SQLite 벡토르자료기지입니다.
```

## 3. 처음 설치하기

아래 명령들은 PowerShell에서 실행합니다.

먼저 프로젝트 폴더로 이동합니다.

```powershell
cd E:\Github\Mobile\Mobile-App\AI-assistant-RAG
```

Python 3.12 가상환경을 만듭니다.

```powershell
py -3.12 -m venv .venv
```

필요한 Python 패키지를 설치합니다.

```powershell
.\.venv\Scripts\python.exe -m pip install -r requirements.txt
```

AI 모델들을 프로젝트 안에 다운로드합니다.

```powershell
.\.venv\Scripts\python.exe scripts\download_models.py
```

이 명령은 `models/` 폴더 안에 다음 모델들을 저장합니다.

```text
models/embedding   의미 검색용 모델
models/reranker    검색 결과 재정렬 모델
models/generator   로컬 답변 생성 모델
```

모델 다운로드는 처음 한 번만 하면 됩니다. 이 단계에서는 인터넷이 필요합니다.

## 4. 문서 넣기

질문에 답하게 만들 문서는 `documents/` 폴더에 넣습니다.

지원하는 파일 형식:

- `.txt`
- `.md`
- `.json`
- `.jsonl`

예:

```text
documents/company_guide.md
documents/food_rules.txt
documents/workout_plan.jsonl
```

문서를 추가하거나 수정한 뒤에는 반드시 다시 색인해야 합니다.

## 5. 문서 색인하기

아래 명령을 실행합니다.

```powershell
.\.venv\Scripts\python.exe scripts\ingest_documents.py
```

성공하면 이런 식으로 출력됩니다.

```text
[embed] encoding 6 chunk(s) with local embedding model...
[ok] documents\sample_food.md -> 1 chunk(s)
[ok] documents\sample_guide.md -> 1 chunk(s)

Indexed documents : 4
Indexed chunks    : 6
Retrieval stack   : dense + BM25 + HyDE + graph + reranker
Vector database   : ...\storage\vector_store.db
```

이 과정에서 `storage/vector_store.db` 파일이 만들어집니다.

## 6. 서버 실행하기

아래 명령을 실행합니다.

```powershell
.\.venv\Scripts\python.exe scripts\run_server.py
```

그 다음 브라우저에서 아래 주소를 엽니다.

```text
http://127.0.0.1:8010
```

첫 질문은 조금 느릴 수 있습니다. 모델을 처음 메모리에 올리기 때문입니다.

## 7. 오프라인으로 실행하기

모델 다운로드와 패키지 설치가 끝난 뒤에는 인터넷 없이 실행할 수 있습니다.

오프라인 실행 전 PowerShell에서 아래 환경변수를 설정합니다.

```powershell
$env:HF_HUB_OFFLINE="1"
$env:TRANSFORMERS_OFFLINE="1"
```

그 다음 서버를 실행합니다.

```powershell
.\.venv\Scripts\python.exe scripts\run_server.py
```

## 8. 자주 쓰는 명령 모음

프로젝트 폴더로 이동:

```powershell
cd E:\Github\Mobile\Mobile-App\AI-assistant-RAG
```

문서 다시 색인:

```powershell
.\.venv\Scripts\python.exe scripts\ingest_documents.py
```

서버 실행:

```powershell
.\.venv\Scripts\python.exe scripts\run_server.py
```

API 상태 확인:

```powershell
Invoke-RestMethod -Uri "http://127.0.0.1:8010/api/health"
```

질문 테스트:

```powershell
$body = @{ question = "새 문서는 어떻게 추가하는가?" } | ConvertTo-Json
Invoke-RestMethod `
  -Uri "http://127.0.0.1:8010/api/chat" `
  -Method Post `
  -ContentType "application/json; charset=utf-8" `
  -Body ([System.Text.Encoding]::UTF8.GetBytes($body))
```

## 9. API 설명

### GET `/api/health`

서버와 자료기지 상태를 확인합니다.

응답 예:

```json
{
  "ok": true,
  "ready": true,
  "chunks": 6,
  "embedding": true,
  "reranker": true,
  "generator": true,
  "retrieval": "dense+bm25+hyde+graph+cross_encoder"
}
```

### POST `/api/chat`

질문을 보내면 답변과 근거 문서를 돌려줍니다.

요청:

```json
{
  "question": "새 문서는 어떻게 추가하는가?"
}
```

응답에는 `answer`와 `sources`가 들어 있습니다.

### POST `/api/search`

답변 생성 없이 검색 결과만 확인합니다.

요청:

```json
{
  "query": "운동 계획"
}
```

## 10. 답변이 어떻게 만들어지는가

사용자가 질문하면 내부에서는 다음 순서로 처리됩니다.

```text
1. 질문을 벡토르로 변환합니다.
2. 문서 벡토르와 비교해 의미가 가까운 문서를 찾습니다.
3. BM25로 키워드가 맞는 문서를 찾습니다.
4. 두 검색 결과를 합칩니다.
5. 문서 단어 관계를 이용해 관련 문서를 더 찾습니다.
6. cross-encoder reranker가 후보 문서를 다시 정렬합니다.
7. 상위 문서 조각을 근거로 답변을 만듭니다.
8. 근거가 약하거나 생성 답변이 불안하면 문서 추출 답변으로 대체합니다.
```

그래서 일반 챗봇처럼 아무 말이나 지어내기보다, 문서 안에 있는 내용에 더 가깝게 답하게 됩니다.

## 11. 문제 해결

### `models/embedding` 또는 `models/reranker`가 없다고 나올 때

모델 다운로드를 다시 실행합니다.

```powershell
.\.venv\Scripts\python.exe scripts\download_models.py
```

### 질문해도 "자료기지가 비었다"고 나올 때

문서 색인을 다시 실행합니다.

```powershell
.\.venv\Scripts\python.exe scripts\ingest_documents.py
```

### 브라우저가 열리지 않을 때

서버가 실행 중인지 확인합니다.

```powershell
Invoke-RestMethod -Uri "http://127.0.0.1:8010/api/health"
```

정상이라면 브라우저에서 직접 엽니다.

```text
http://127.0.0.1:8010
```

### 포트 8010이 이미 사용 중일 때

이미 서버가 실행 중일 수 있습니다. 브라우저에서 먼저 확인합니다.

```text
http://127.0.0.1:8010
```

다른 프로그램이 쓰고 있다면 `app/main.py`의 `DEFAULT_PORT` 값을 바꿔 실행할 수 있습니다.

### 첫 답변이 느릴 때

정상입니다. 첫 요청 때 embedding, reranker, generator 모델을 메모리에 올립니다. 두 번째 질문부터는 보통 더 빠릅니다.

### 답변이 마음에 들지 않을 때

대부분 원인은 문서 부족입니다. `documents/` 폴더에 더 구체적인 문서를 넣고 다시 색인하십시오.

```powershell
.\.venv\Scripts\python.exe scripts\ingest_documents.py
```

## 12. 개발자가 볼 파일

검색 알고리듬을 고치려면:

```text
app/rag/store.py
```

문서 읽기와 chunking을 고치려면:

```text
app/rag/text.py
```

모델 로딩을 고치려면:

```text
app/rag/models.py
```

프론트엔드를 고치려면:

```text
frontend/index.html
frontend/styles.css
frontend/app.js
```

## 13. 현재 상태

현재 프로젝트는 로컬 오프라인 RAG로 구성되어 있습니다.

```text
embedding  준비됨
reranker   준비됨
generator  준비됨
database   storage/vector_store.db
server     http://127.0.0.1:8010
```

실제 품질은 `documents/` 안에 들어가는 문서의 양과 품질에 크게 좌우됩니다.
