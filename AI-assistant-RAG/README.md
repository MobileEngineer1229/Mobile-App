# AI-assistant-RAG

`AI-assistant-RAG`reads documents on his computer, Offline to answer questions based on the contents of the document AI It's a chatbot.

internet APIWithout sending the question to an external server, Use models and databases stored in the project folder..

## 1. What this project does

The usage flow is simple.

```text
1. documents Put the document in the folder.
2. Run the index script.
3. The document is divided into small pieces and stored in the Vector database..
4. Ask a question on the web screen.
5. AILooking for related documentation, I will answer based on that evidence..
```

Current search/answer technology:

- Dense embedding retrieval: Search by changing the meaning of a sentence to vector..
- BM25 retrieval: Find documents with exact keywords.
- Hybrid search: Uses semantic search and keyword search together.
- HyDE-style expansion: Search better by expanding on evidence closer to your question.
- Graph-style expansion: Use word relationships that appear together in a document.
- Cross-encoder reranking: The candidate documents found are re-ranked precisely..
- Local generator: local LLMCreate an answer with, If the evidence is weak, you can safely return to the answer by extracting the evidence..

## 2. Folder Description

```text
AI-assistant-RAG/
  app/
    main.py
      with web server APIcontains.

    rag/
      models.py
        Load local model.

      store.py
        Vector Search Data Base, BM25, graph expansion, reranking It's logic.

      text.py
        Read the document, Crop document, Tokenization logic.

  documents/
    Here the user puts the document.

  frontend/
    This is the chat screen viewed in the browser..

  models/
    downloaded AI Models are saved.
    Created automatically when first installed.

  scripts/
    download_models.py
      Models required for offline execution models Download to folder.

    ingest_documents.py
      documents Create a vector database by reading the documents in the folder..

    run_server.py
      Run the web server.

  storage/
    vector_store.db
      created SQLite This is the Vector data base..
```

## 3. Install for the first time

The commands below are PowerShellRun from.

First go to your project folder.

```powershell
cd E:\Github\Mobile\Mobile-App\AI-assistant-RAG
```

Python 3.12 Create a virtual environment.

```powershell
py -3.12 -m venv .venv
```

necessary Python Install the package.

```powershell
.\.venv\Scripts\python.exe -m pip install -r requirements.txt
```

AI Download models into your project.

```powershell
.\.venv\Scripts\python.exe scripts\download_models.py
```

This command is `models/` Save the following models in the folder.

```text
models/embedding   A model for semantic search
models/reranker    Search result reordering model
models/generator   Local answer generation model
```

You only need to download the model once for the first time. Internet is required for this step.

## 4. Load document

Documents that will answer your questions `documents/` Put it in the folder.

Supported file formats:

- `.txt`
- `.md`
- `.json`
- `.jsonl`

yes:

```text
documents/company_guide.md
documents/food_rules.txt
documents/workout_plan.jsonl
```

You must re-index after adding or modifying documents.

## 5. Indexing documents

Run the command below.

```powershell
.\.venv\Scripts\python.exe scripts\ingest_documents.py
```

If successful, it will output something like this:.

```text
[embed] encoding 6 chunk(s) with local embedding model...
[ok] documents\sample_food.md -> 1 chunk(s)
[ok] documents\sample_guide.md -> 1 chunk(s)

Indexed documents : 4
Indexed chunks    : 6
Retrieval stack   : dense + BM25 + HyDE + graph + reranker
Vector database   : ...\storage\vector_store.db
```

In this process `storage/vector_store.db` The file is created.

## 6. Running the server

Run the command below.

```powershell
.\.venv\Scripts\python.exe scripts\run_server.py
```

Then open the address below in your browser.

```text
http://127.0.0.1:8010
```

The first question might be a little slow. This is because the model is first loaded into memory..

## 7. Run offline

After downloading the model and installing the package, you can run it without internet..

Before running offline PowerShellSet the environment variables below:.

```powershell
$env:HF_HUB_OFFLINE="1"
$env:TRANSFORMERS_OFFLINE="1"
```

Then run the server.

```powershell
.\.venv\Scripts\python.exe scripts\run_server.py
```

## 8. Collection of frequently used commands

Go to project folder:

```powershell
cd E:\Github\Mobile\Mobile-App\AI-assistant-RAG
```

Re-index documents:

```powershell
.\.venv\Scripts\python.exe scripts\ingest_documents.py
```

run server:

```powershell
.\.venv\Scripts\python.exe scripts\run_server.py
```

API Check status:

```powershell
Invoke-RestMethod -Uri "http://127.0.0.1:8010/api/health"
```

question test:

```powershell
$body = @{ question = "How to add a new document?" } | ConvertTo-Json
Invoke-RestMethod `
  -Uri "http://127.0.0.1:8010/api/chat" `
  -Method Post `
  -ContentType "application/json; charset=utf-8" `
  -Body ([System.Text.Encoding]::UTF8.GetBytes($body))
```

## 9. API Description

### GET `/api/health`

Check server and database status.

Example response:

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

Send us a question and we'll send you back an answer and supporting documentation..

request:

```json
{
  "question": "How to add a new document?"
}
```

In response `answer`Wow `sources`contains.

### POST `/api/search`

Only check search results without generating answers.

request:

```json
{
  "query": "exercise plan"
}
```

## 10. How the answer is created

When a user asks a question, it is processed internally in the following order:.

```text
1. Convert question to vector.
2. Compare the document vector to find documents with similar meaning..
3. BM25Find documents matching keywords with.
4. Combines two search results.
5. Find more related documents using document word relationships.
6. cross-encoder rerankerReorder the candidate documents.
7. Create answers based on parent document fragments.
8. If the evidence is weak or the generated answer is unstable, it will be replaced with a document-extracted answer..
```

So, rather than making up random words like a regular chatbot,, Your answer will be closer to what is in the document.

## 11. problem solving

### `models/embedding` or `models/reranker`When it says there is no

Run the model download again.

```powershell
.\.venv\Scripts\python.exe scripts\download_models.py
```

### Even if I ask a question "The data base is empty."When you come out

Rerun document index.

```powershell
.\.venv\Scripts\python.exe scripts\ingest_documents.py
```

### When the browser does not open

Make sure the server is running.

```powershell
Invoke-RestMethod -Uri "http://127.0.0.1:8010/api/health"
```

If it's ok, open it directly in your browser.

```text
http://127.0.0.1:8010
```

### When port 8010 is already in use

You may already have a server running. Check in browser first.

```text
http://127.0.0.1:8010
```

If another program is using `app/main.py`of `DEFAULT_PORT` You can change the value and run it.

### When the first response is slow

It's normal. When first requested embedding, reranker, generator Load the model into memory. The second question is usually faster.

### When you don't like the answer

The most common cause is lack of documentation.. `documents/` Put more specific documents in the folder and index it again.

```powershell
.\.venv\Scripts\python.exe scripts\ingest_documents.py
```

## 12. Files for developers to view

To fix the search algorithm:

```text
app/rag/store.py
```

Reading documents and chunkingTo fix:

```text
app/rag/text.py
```

To fix model loading:

```text
app/rag/models.py
```

To fix the frontend:

```text
frontend/index.html
frontend/styles.css
frontend/app.js
```

## 13. current status

Current project is local offline RAGIt consists of.

```text
embedding  Ready
reranker   Ready
generator  Ready
database   storage/vector_store.db
server     http://127.0.0.1:8010
```

The actual quality is `documents/` It largely depends on the quantity and quality of the documents inside..
