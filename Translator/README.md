# Translator

`Translator`는 조선민주주의인민공화국 국어, 영어, 중어, 로어 사이의 **쌍방향 오프라인 번역봉사**를 만들기 위한 프로젝트입니다.

이 프로젝트의 중심은 조선말자료기지입니다. 조선말자료는 사용자가 직접 구축할수 있다고 전제합니다. 그러므로 이 프로젝트는 조선말 원문자료와 조선말 병렬번역자료를 직접 모아 번역기를 키우는 방향으로 갑니다.

## 무엇을 만들것인가

최종 목표는 다음 3가지 입력을 모두 처리하는 쌍방향번역기입니다.

1. 문자번역  
   사용자가 글을 입력하면 조선말, 영어, 중어, 로어 사이에서 번역합니다.

2. 음성번역  
   사용자가 말하면 오프라인 음성인식으로 글을 만들고, 그 글을 번역합니다.

3. 이미지번역  
   사용자가 사진이나 그림을 넣으면 오프라인 문자추출/OCR로 글을 뽑고, 그 글을 번역합니다.

현재 첫 실행대상은 손전화앱이 아니라 **국부 웹대면부**입니다. 열람기에서 바로 시험하고, 이후 같은 번역엔진을 손전화앱이나 다른 봉사에 붙일수 있게 만듭니다.

## 번역방향

기본언어코드는 다음과 같습니다.

| 이름 | 내부코드 | 설명 |
|---|---:|---|
| 조선말 | `ko_kp` | 조선민주주의인민공화국 국어 |
| 영어 | `en` | English |
| 중어 | `zh` | Chinese |
| 로어 | `ru` | Russian |

목표 번역방향:

```text
ko_kp <-> en
ko_kp <-> zh
ko_kp <-> ru
en    <-> zh
en    <-> ru
zh    <-> ru
```

가장 중요한 방향은 `ko_kp <-> en`, `ko_kp <-> zh`, `ko_kp <-> ru`입니다.

## 기본동작 원리

이 프로젝트는 처음부터 완전한 대형모델만 기다리지 않습니다. 사람이 만든 자료가 곧바로 번역기에 반영되도록 다음 순서로 동작합니다.

```text
사용자 입력
  -> 입력형식 판정: 문자 / 음성 / 이미지
  -> 음성인식 또는 이미지문자인식
  -> 번역메모리 검색
  -> 오프라인 신경망 번역모델
  -> 조선말 용어사전
  -> 조선말 후처리규칙
  -> 번역결과
```

### 번역메모리란 무엇인가

번역메모리는 사람이 이미 검토한 번역쌍입니다. 례를 들면:

```tsv
source_language	target_language	source_text	target_text
en	ko_kp	Good morning.	좋은 아침입니다.
ko_kp	en	좋은 아침입니다.	Good morning.
```

사용자가 같은 문장을 다시 번역하면 인공지능모델을 부르기 전에 번역메모리에서 정확한 결과를 먼저 돌려줍니다. 이것은 초기에 번역품질을 빨리 높이는 가장 현실적인 방법입니다.

## 프로젝트 구조

```text
Translator/
  web_service/
    열람기에서 시험하는 국부 웹대면부와 /api/translate 접속점.

  python_lab/
    번역메모리, 후처리, 모델시험을 위한 파이썬 작업공간.

  scripts/
    자료수입, 자료검사, 모델준비, 학습자료 생성을 위한 파이썬 스크립트.

  data/
    조선말자료기지, 번역메모리, 용어사전, 평가자료.

  models/
    오프라인 번역, 음성인식, OCR, 음성합성 모델을 넣는 곳.

  docs/
    설계문서, DPRK-BERT 분석, 모델학습계획.

  mobile/
    나중에 손전화앱을 만들 때 사용할수 있는 보관된 골격.
```

## 자료기지 구조

```text
data/corpus/
  raw/
    조선말 원문자료를 넣는 곳.

  processed/
    정리된 문자자료가 만들어지는 곳.

  parallel/inbox/
    사용자가 직접 만든 병렬번역쌍을 넣는 곳.

  parallel/
    검증된 병렬번역자료가 만들어지는 곳.

  speech/
    음성자료와 받아쓴 글을 넣는 곳.

  image/
    이미지자료와 OCR 정답글을 넣는 곳.

  licenses/
    자료출처와 사용허가를 적는 곳.
```

## URL을 주어 자료를 넣는 방법

URL에 있는 글을 조선말자료기지에 넣으려면 다음 명령을 씁니다.

```powershell
python scripts\learn_from_url.py "https://example.com/page" --language ko_kp
```

웹대면부에서도 할수 있습니다.

1. `python scripts\run_web_service.py`를 실행합니다.
2. 열람기에서 `http://127.0.0.1:8765`를 엽니다.
3. `Corpus` 부분에 URL을 넣습니다.
4. 언어를 고릅니다.
5. `Import URL`을 누릅니다.
6. `Rebuild / Train local models`를 누릅니다.

이 흐름은 다음 파일들을 갱신합니다.

```text
data/corpus/raw/
data/corpus/source_registry.tsv
data/corpus/processed/monolingual_sentences.tsv
data/training/mlm/
models/text/ko_kp_char_lm.json
```

번역쌍 자료가 있으면 다음 파일들도 갱신됩니다.

```text
data/corpus/parallel/reviewed_parallel.tsv
data/translation_memory.json
data/training/translation/
data/training/neural/
```

## 조선말 URL과 영어 URL을 한쌍으로 넣는 방법

조선말 문서 URL과 그에 해당한 영어 문서 URL이 있을 때에는 아래 명령을 씁니다.

```powershell
python scripts\learn_from_url_pair.py --source-url "조선말_URL" --target-url "영어_URL"
```

이 명령은 두 문서를 내려받고 문장을 나누어 후보 병렬자료를 만듭니다.

```text
data/corpus/url_pairs/candidates/
```

후보자료는 자동정렬이므로 반드시 사람이 확인해야 합니다. 좋은 줄만 다음 등록부에 TSV로 넣으면 학습자료에 들어갑니다.

```text
data/corpus/url_pairs/approved/
```

두 URL의 문장순서가 완전히 같다는것을 이미 알고 있을 때에만 다음 선택을 쓸수 있습니다.

```powershell
python scripts\learn_from_url_pair.py --source-url "조선말_URL" --target-url "영어_URL" --approve-exact-order
```

이 경우 후보정렬이 곧바로 승인자료로 들어가며 `bootstrap_data_pipeline.py`가 번역메모리와 학습자료를 갱신합니다.

## 1. 문자자료 넣기

조선말 원문자료는 여기에 넣습니다.

```text
data/corpus/raw/
```

지원하는 형식:

```text
.corpus .md .json .jsonl .tsv .csv
```

례:

```text
data/corpus/raw/article_001.corpus
data/corpus/raw/book_notes.jsonl
data/corpus/raw/sentences.tsv
```

자료를 넣은 다음 실행:

```powershell
python scripts\bootstrap_data_pipeline.py
```

그러면 다음 파일이 만들어집니다.

```text
data/corpus/processed/monolingual_sentences.tsv
```

이 파일은 나중에 조선말 언어모델, 문체판정, 용어추출에 쓰입니다.

## 2. 병렬번역자료 넣기

사람이 직접 만든 번역쌍은 여기에 넣습니다.

```text
data/corpus/parallel/inbox/
```

파일형식은 TSV입니다. 첫 줄은 반드시 다음과 같아야 합니다.

```tsv
source_language	target_language	source_text	target_text
```

례:

```tsv
source_language	target_language	source_text	target_text
en	ko_kp	Good morning.	좋은 아침입니다.
ko_kp	en	좋은 아침입니다.	Good morning.
zh	ko_kp	你好	안녕하십니까.
ko_kp	ru	안녕하십니까.	Здравствуйте.
```

자료를 넣은 다음 실행:

```powershell
python scripts\bootstrap_data_pipeline.py
```

그러면 다음 파일들이 만들어집니다.

```text
data/corpus/parallel/reviewed_parallel.tsv
data/translation_memory.json
data/training/translation/train.tsv
data/training/translation/dev.tsv
data/training/translation/test.tsv
```

## 3. 음성자료 넣기

음성번역을 만들려면 두가지 자료가 필요합니다.

1. 음성파일
2. 그 음성을 사람이 받아쓴 정답글

권장 구조:

```text
data/corpus/speech/audio/
  speech_0001.wav
  speech_0002.wav

data/corpus/speech/transcripts.tsv
```

`transcripts.tsv` 형식:

```tsv
audio_path	language	text
data/corpus/speech/audio/speech_0001.wav	ko_kp	안녕하십니까.
data/corpus/speech/audio/speech_0002.wav	en	Good morning.
```

음성번역의 실제 흐름:

```text
음성파일
  -> 오프라인 음성인식모델
  -> 문자
  -> 문자번역파이프라인
  -> 번역결과
```

처음에는 Whisper.cpp 또는 Vosk 같은 오프라인 음성인식엔진을 붙일수 있습니다. 그러나 조선말 음성인식품질을 높이려면 사용자가 만든 조선말 음성자료와 정답 받아쓰기자료가 필요합니다.

## 4. 이미지자료 넣기

이미지번역을 만들려면 두가지 자료가 필요합니다.

1. 글자가 들어있는 이미지
2. 이미지에서 사람이 확인한 정답글

권장 구조:

```text
data/corpus/image/files/
  image_0001.png
  image_0002.jpg

data/corpus/image/ocr_labels.tsv
```

`ocr_labels.tsv` 형식:

```tsv
image_path	language	text
data/corpus/image/files/image_0001.png	ko_kp	조선말
data/corpus/image/files/image_0002.jpg	en	Translator
```

이미지번역의 실제 흐름:

```text
이미지
  -> 오프라인 OCR
  -> 문자
  -> 문자번역파이프라인
  -> 번역결과
```

처음에는 PaddleOCR 같은 오프라인 OCR엔진을 붙일수 있습니다. 조선말 인식품질을 높이려면 조선말 글자가 들어있는 이미지와 정답글을 꾸준히 모아야 합니다.

## 처음 실행하는 방법

먼저 프로젝트 등록부로 들어갑니다.

```powershell
cd Translator
```

환경을 확인합니다.

```powershell
python scripts\check_environment.py
```

자료작업공간을 준비하고, 들어온 자료를 처리합니다.

```powershell
python scripts\bootstrap_data_pipeline.py
```

웹대면부를 실행합니다.

```powershell
python scripts\run_web_service.py
```

열람기에서 엽니다.

```text
http://127.0.0.1:8765
```

## 매일 작업순서

1. 조선말 원문자료를 `data/corpus/raw/`에 넣습니다.
2. 직접 검토한 번역쌍을 `data/corpus/parallel/inbox/`에 넣습니다.
3. 음성자료가 있으면 `data/corpus/speech/audio/`와 `transcripts.tsv`를 채웁니다.
4. 이미지자료가 있으면 `data/corpus/image/files/`와 `ocr_labels.tsv`를 채웁니다.
5. 아래 명령을 실행합니다.

```powershell
python scripts\bootstrap_data_pipeline.py
```

6. 웹대면부에서 번역결과를 시험합니다.

```powershell
python scripts\run_web_service.py
```

7. 틀린 번역은 새 번역쌍, 용어사전, 후처리규칙으로 고칩니다.

## 현재 구현된것

현재 이미 들어있는 기능:

1. 국부 웹대면부
2. `/api/translate` 번역접속점
3. 번역메모리 우선검색
4. 조선말 용어사전
5. 조선말 후처리규칙
6. 조선말 원문자료 수입
7. 병렬번역자료 수입
8. 학습용 train/dev/test 자료 생성
9. DPRK-BERT 분석문서
10. 조선말자료기지 구축계획
11. URL로부터 원문자료 수입
12. 국부 조선말 문자언어모델 학습
13. 웹대면부에서 URL 수입과 국부모델 재학습
14. 자체 SentencePiece 토크나이저 학습
15. 자체 PyTorch Transformer 번역모델을 0부터 학습
16. 자체모델 추론 backend

아직 붙여야 하는 기능:

1. 오프라인 음성인식모델
2. 오프라인 OCR모델
3. 음성합성모델
4. 번역품질 자동평가

현재의 학습은 `translation_memory.json`과 `models/text/ko_kp_char_lm.json`을 만드는 국부학습입니다. 완전한 신경망 번역모델 학습은 자료가 충분히 쌓인 다음 `data/training/translation/` 및 `data/training/neural/` 자료를 가지고 진행합니다.

## 자체 번역모델을 처음부터 학습하기

이 절은 미세조정을 쓰지 않는 길입니다. 이미 학습된 NLLB, Marian, mBART 같은 모델을 부르지 않고, 이 프로젝트안의 자료만으로 토크나이저와 Transformer 번역모델을 새로 만듭니다.

먼저 수집자료를 train/dev/test와 neural JSONL로 다시 만듭니다.

```powershell
python scripts\bootstrap_data_pipeline.py
```

그 다음 자체 SentencePiece BPE 토크나이저를 학습합니다.

```powershell
python scripts\train_tokenizer.py --vocab-size 16000
```

특정 방향 자료만으로 토크나이저를 만들고 싶으면:

```powershell
python scripts\train_tokenizer.py --direction ko_kp__en --vocab-size 16000
```

이제 자체 Transformer 번역모델을 0부터 학습합니다. 현재 컴퓨터의 12GB VRAM에서는 아래와 같은 작은 설정부터 시작하는것이 안전합니다.

```powershell
python scripts\train_transformer_from_scratch.py --direction ko_kp__en --epochs 20 --batch-size 8 --d-model 256 --encoder-layers 4 --decoder-layers 4
```

영어에서 조선말 방향도 따로 학습해야 합니다.

```powershell
python scripts\train_transformer_from_scratch.py --direction en__ko_kp --epochs 20 --batch-size 8 --d-model 256 --encoder-layers 4 --decoder-layers 4
```

학습이 끝나면 다음 파일들이 생깁니다.

```text
models/text/from_scratch/tokenizer/tokenizer.model
models/text/from_scratch/ko_kp__en/best.pt
models/text/from_scratch/ko_kp__en/config.json
models/text/active_backend.json
```

`active_backend.json`의 `backend`가 `from_scratch`이면 웹대면부의 번역기는 이 자체모델을 먼저 사용합니다. 번역메모리에 정확히 같은 문장이 있으면 번역메모리가 우선입니다.

주의할 점:

1. 이 경로는 미세조정이 아닙니다.
2. 기성 번역모델 가중치를 쓰지 않습니다.
3. 기성 토크나이저를 쓰지 않습니다.
4. 품질은 자료량과 승인품질에 좌우됩니다.
5. 처음 몇만 문장쌍에서는 실험품질이고, 실전품질은 보통 수십만 문장쌍부터 좋아집니다.

처음부터 학습하는 경로의 명령상태를 확인하려면 다음 명령들을 씁니다.

```powershell
python scripts\train_tokenizer.py --help
python scripts\train_transformer_from_scratch.py --help
```

학습에 필요한 작은 패키지들을 오프라인용으로 준비하려면:

```powershell
python scripts\prepare_offline_packages.py
python scripts\install_offline_packages.py
python scripts\check_training_dependencies.py
```

이 등록부에 오프라인 설치용 wheel 파일들이 보관됩니다.

```text
third_party/wheels/
```

`torch` 같은 CUDA 학습패키지는 이미 현재 Python 환경에 설치되어 있습니다. 다른 오프라인 콤퓨터로 옮기려면 그 콤퓨터의 Python/CUDA 판본에 맞는 `torch` wheel을 따로 준비해야 합니다.

기존 `scripts\train_neural_translation_model.py`는 기성모델 미세조정용으로 남아있는 유산 스크립트입니다. 이 프로젝트의 공식경로가 아니므로 기본상태에서는 실행되지 않고 `disabled` 상태를 돌려줍니다. 미세조정이 필요없다면 이 스크립트를 쓰지 마십시오.

공식 학습명령은 다음과 같습니다.

```powershell
python scripts\train_tokenizer.py --vocab-size 16000
python scripts\train_transformer_from_scratch.py --direction ko_kp__en
python scripts\train_transformer_from_scratch.py --direction en__ko_kp
```

이 명령들은 기성모델의 가중치나 토크나이저를 부르지 않습니다.

## 어떤 모델을 붙일것인가

문자번역:

```text
NLLB / Marian / OpenNMT / CTranslate2 계렬
```

음성인식:

```text
Whisper.cpp / Vosk
```

이미지문자인식:

```text
PaddleOCR
```

음성합성:

```text
Piper 또는 다른 오프라인 TTS
```

중요한 점은 모델보다 자료가 먼저라는것입니다. 사용자가 조선말자료기지를 구축할수 있다면 이 프로젝트는 자료가 쌓일수록 점점 더 조선말번역기에 가까와집니다.

## DPRK-BERT는 어디에 쓰는가

DPRK-BERT는 번역모델이 아니라 조선말 언어모델입니다. 그러므로 직접 번역문을 생성하는 기본엔진으로 쓰지 않습니다.

대신 다음 일에 쓸수 있습니다.

1. 번역결과가 조선말답게 나왔는지 점수화
2. 조선말 용어 추출
3. 후보번역 순위매기기
4. 조선말 문체판정
5. 자료기지 품질검사

분석문서:

```text
docs/DPRK_BERT_ANALYSIS.md
docs/URL_TO_TRAINING_WORKFLOW.md
docs/COMPLETE_PROJECT_BLUEPRINT.md
```

## 프로젝트 검사

전체 프로젝트 상태와 학습준비상태를 검사하려면:

```powershell
python scripts\project_health.py
```

번역품질 평가보고서는 여기에 만들어집니다.

```text
data/evaluation/translation_eval_report.tsv
```

## 개발원칙

1. 조선말자료기지를 기본으로 한다.
2. 사람이 검토한 번역쌍을 가장 믿는다.
3. 모든 자료수입과 변환은 `scripts/` 안의 파이썬 스크립트로 반복가능하게 만든다.
4. 큰 모델파일은 `models/`에 넣되 git에는 올리지 않는다.
5. 오프라인 동작을 기본으로 한다.
6. 문자번역을 먼저 안정화하고, 그 다음 음성인식과 이미지인식을 붙인다.

## 다음 단계

1. 실제 조선말 원문자료를 `data/corpus/raw/`에 넣습니다.
2. 직접 만든 번역쌍을 `data/corpus/parallel/inbox/`에 넣습니다.
3. `python scripts\bootstrap_data_pipeline.py`를 실행합니다.
4. 웹대면부에서 번역결과를 확인합니다.
5. 자료가 충분히 쌓이면 첫 오프라인 번역모델을 학습하거나 미세조정합니다.
