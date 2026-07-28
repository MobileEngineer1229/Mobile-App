# Translator

`Translator`is the national language of the Democratic People's Republic of Korea, english, Chinese, between lower **Interactive offline translation service**This is a project to create.

The center of this project is the Joseon Dynasty Data Base.. It is assumed that users can construct Joseon language data themselves.. Therefore, this project is aimed at developing a translator by directly collecting Korean language original text materials and Korean language parallel translation materials..

## what to make

The ultimate goal is an interactive translator that processes all three inputs:.

1. Text translation  
   When the user enters text, it is written in Korean, english, Chinese, Translate between Lore.

2. voice translation  
   When the user speaks, text is created using offline voice recognition., I translate the text.

3. Image translation  
   Offline text extraction when a user enters a photo or picture/OCRSelect the article with, I translate the text.

Currently, the first launch target is not the mobile phone app. **Ministry of National Affairs web face-to-face department**is. Test it right from the reading machine, Afterwards, the same translation engine can be attached to mobile phone apps or other services..

## Translation direction

The default language code is as follows:.

| name | Internal code | Description |
|---|---:|---|
| Joseon language | `ko_kp` | National language of the Democratic People's Republic of Korea |
| english | `en` | English |
| Chinese | `zh` | Chinese |
| lore | `ru` | Russian |

Target translation direction:

```text
ko_kp <-> en
ko_kp <-> zh
ko_kp <-> ru
en    <-> zh
en    <-> ru
zh    <-> ru
```

The most important direction is `ko_kp <-> en`, `ko_kp <-> zh`, `ko_kp <-> ru`is.

## Basic operating principle

This project doesn't just wait for a full-scale model from the start.. It operates in the following order so that human-created data is immediately reflected in the translator..

```text
user input
  -> Input format determination: character / voice / image
  -> Voice recognition or image text recognition
  -> Translation memory search
  -> Offline neural network translation model
  -> Korean terminology dictionary
  -> Post-processing rules in the late Joseon Dynasty
  -> Translation result
```

### What is translation memory?

Translation memories are pairs of translations that have already been reviewed by humans.. For example:

```tsv
source_language	target_language	source_text	target_text
en	ko_kp	Good morning.	good morning.
ko_kp	en	good morning.	Good morning.
```

If the user translates the same sentence again, the translation memory returns accurate results before calling the artificial intelligence model.. This is the most realistic way to quickly increase translation quality in the beginning..

## project structure

```text
Translator/
  web_service/
    Local wealth web face-to-face section tested in a reading machine /api/translate connection point.

  python_lab/
    translation memory, Post-processing, Python workspace for model testing.

  scripts/
    data import, Data inspection, Model preparation, Python script for creating learning materials.

  data/
    Joseon Dynasty Data Base, translation memory, Glossary, Evaluation data.

  models/
    offline translation, voice recognition, OCR, Where to put the voice synthesis model.

  docs/
    design document, DPRK-BERT analysis, Model learning plan.

  mobile/
    An archived skeleton that can be used later when creating a mobile phone app..
```

## Data base structure

```text
data/corpus/
  raw/
    A place to store original text materials from the Joseon Dynasty.

  processed/
    A place where organized text data is created.

  parallel/inbox/
    A place to insert parallel translation pairs created by the user.

  parallel/
    A place where verified parallel translation materials are created.

  speech/
    A place to put audio material and written text.

  image/
    image data and OCR Where to put the correct answer.

  licenses/
    A place to list data sources and permission to use.
```

## URLHow to insert data by giving

URLTo insert the text in the Joseon language data base, use the following command:.

```powershell
python scripts\learn_from_url.py "https://example.com/page" --language ko_kp
```

You can also do it online.

1. `python scripts\run_web_service.py`Run.
2. In the browser `http://127.0.0.1:8765`Open.
3. `Corpus` in part URLPut.
4. Choose a language.
5. `Import URL`Press.
6. `Rebuild / Train local models`Press.

This flow updates the following files:.

```text
data/corpus/raw/
data/corpus/source_registry.tsv
data/corpus/processed/monolingual_sentences.tsv
data/training/mlm/
models/text/ko_kp_char_lm.json
```

If translation pair data is available, the following files will also be updated..

```text
data/corpus/parallel/reviewed_parallel.tsv
data/translation_memory.json
data/training/translation/
data/training/neural/
```

## Joseon language URLand english URLHow to pair

Joseon language documents URLand corresponding English documents URLIn this case, write the command below.

```powershell
python scripts\learn_from_url_pair.py --source-url "Joseon language_URL" --target-url "english_URL"
```

This command downloads two documents and splits the sentences to create candidate parallel data..

```text
data/corpus/url_pairs/candidates/
```

Candidate data is automatically sorted, so it must be checked by a human.. Only good lines go to the next register TSVEnter it into the learning materials..

```text
data/corpus/url_pairs/approved/
```

two URLYou can use the next option only when you already know that the sentence order of is exactly the same..

```powershell
python scripts\learn_from_url_pair.py --source-url "Joseon language_URL" --target-url "english_URL" --approve-exact-order
```

In this case, the candidate sorting goes directly into the approval data. `bootstrap_data_pipeline.py`Update translation memory and learning materials.

## 1. Insert text data

The original text in the Joseon Dynasty is inserted here..

```text
data/corpus/raw/
```

Supported formats:

```text
.corpus .md .json .jsonl .tsv .csv
```

example:

```text
data/corpus/raw/article_001.corpus
data/corpus/raw/book_notes.jsonl
data/corpus/raw/sentences.tsv
```

Insert data and then run:

```powershell
python scripts\bootstrap_data_pipeline.py
```

This will create the following files.

```text
data/corpus/processed/monolingual_sentences.tsv
```

This file will later be used as a Korean language model, Writing style judgment, Used for term extraction.

## 2. Insert parallel translation data

Translation pairs created by humans are inserted here..

```text
data/corpus/parallel/inbox/
```

The file format is TSVis. The first line must be:.

```tsv
source_language	target_language	source_text	target_text
```

example:

```tsv
source_language	target_language	source_text	target_text
en	ko_kp	Good morning.	good morning.
ko_kp	en	good morning.	Good morning.
zh	ko_kp	你好	hello.
ko_kp	ru	hello.	Здравствуйте.
```

Insert data and then run:

```powershell
python scripts\bootstrap_data_pipeline.py
```

Then the following files will be created.

```text
data/corpus/parallel/reviewed_parallel.tsv
data/translation_memory.json
data/training/translation/train.tsv
data/training/translation/dev.tsv
data/training/translation/test.tsv
```

## 3. Insert audio data

To create a voice translation, you need two materials:.

1. voice file
2. The correct answer written by a person from that voice

Recommended Structure:

```text
data/corpus/speech/audio/
  speech_0001.wav
  speech_0002.wav

data/corpus/speech/transcripts.tsv
```

`transcripts.tsv` format:

```tsv
audio_path	language	text
data/corpus/speech/audio/speech_0001.wav	ko_kp	hello.
data/corpus/speech/audio/speech_0002.wav	en	Good morning.
```

The actual flow of voice translation:

```text
voice file
  -> Offline voice recognition model
  -> character
  -> Text translation pipeline
  -> Translation result
```

At first Whisper.cpp or Vosk You can attach the same offline voice recognition engine.. However, to improve the quality of Korean speech recognition, user-created Korean speech data and answer dictation data are needed..

## 4. Insert image data

To create an image translation, you need two materials:.

1. image containing text
2. The correct answer confirmed by a person in the image

Recommended Structure:

```text
data/corpus/image/files/
  image_0001.png
  image_0002.jpg

data/corpus/image/ocr_labels.tsv
```

`ocr_labels.tsv` format:

```tsv
image_path	language	text
data/corpus/image/files/image_0001.png	ko_kp	Joseon language
data/corpus/image/files/image_0002.jpg	en	Translator
```

Actual flow of image translation:

```text
image
  -> offline OCR
  -> character
  -> Text translation pipeline
  -> Translation result
```

At first PaddleOCR same offline OCRThe engine can be attached. In order to improve the recognition quality of the Korean language, you must consistently collect images and correct answers containing Korean characters..

## How to run it for the first time

First, go to the project register.

```powershell
cd Translator
```

Check your environment.

```powershell
python scripts\check_environment.py
```

Prepare a data workspace, Process incoming data.

```powershell
python scripts\bootstrap_data_pipeline.py
```

Run the web interface.

```powershell
python scripts\run_web_service.py
```

Open in browser.

```text
http://127.0.0.1:8765
```

## Daily work order

1. Original text materials from the Joseon Dynasty `data/corpus/raw/`Put it in.
2. Translation pairs that we personally reviewed `data/corpus/parallel/inbox/`Put it in.
3. If there is audio material `data/corpus/speech/audio/`Wow `transcripts.tsv`fill in.
4. If you have image data `data/corpus/image/files/`Wow `ocr_labels.tsv`fill in.
5. Run the command below.

```powershell
python scripts\bootstrap_data_pipeline.py
```

6. Test the translation results in the web-face-to-face section..

```powershell
python scripts\run_web_service.py
```

7. Wrong translation is a new translation pair, Glossary, Fix it with post-processing rules.

## What is currently implemented

Features already included:

1. Ministry of National Affairs web face-to-face department
2. `/api/translate` Translation connection point
3. Translation memory priority search
4. Korean terminology dictionary
5. Post-processing rules in the late Joseon Dynasty
6. Import of original text materials from the Joseon Dynasty
7. Import parallel translation data
8. For learning train/dev/test Data generation
9. DPRK-BERT analysis document
10. Plan to build a late Joseon data base
11. URLImport original data from
12. Learning the Korean language model for the father of the nation
13. In the web face-to-face section URL Retraining the income and national wealth model
14. self SentencePiece Tokenizer training
15. self PyTorch Transformer Learn the translation model from 0
16. Own model inference backend

Features that still need to be added:

1. Offline voice recognition model
2. offline OCRmodel
3. Voice synthesis model
4. Automatic evaluation of translation quality

Current learning is `translation_memory.json`and `models/text/ko_kp_char_lm.json`This is a national learning program that creates. Learning a complete neural network translation model begins after sufficient data has been accumulated. `data/training/translation/` and `data/training/neural/` We proceed with data.

## Learning your own translation model from scratch

This section does not use fine tuning.. already learned NLLB, Marian, mBART without calling the same model, The tokenizer and Transformer Create a new translation model.

First, collect data train/dev/testWow neural JSONLrecreate it with.

```powershell
python scripts\bootstrap_data_pipeline.py
```

then itself SentencePiece BPE Learn the tokenizer.

```powershell
python scripts\train_tokenizer.py --vocab-size 16000
```

If you want to create a tokenizer with only specific direction data,:

```powershell
python scripts\train_tokenizer.py --direction ko_kp__en --vocab-size 16000
```

now itself Transformer Learn the translation model from 0. 12 on your current computerGB VRAMIt is safe to start with a small setting like the one below:.

```powershell
python scripts\train_transformer_from_scratch.py --direction ko_kp__en --epochs 20 --batch-size 8 --d-model 256 --encoder-layers 4 --decoder-layers 4
```

You need to learn the directions from English to Korean separately..

```powershell
python scripts\train_transformer_from_scratch.py --direction en__ko_kp --epochs 20 --batch-size 8 --d-model 256 --encoder-layers 4 --decoder-layers 4
```

When learning is complete, the following files will be created:.

```text
models/text/from_scratch/tokenizer/tokenizer.model
models/text/from_scratch/ko_kp__en/best.pt
models/text/from_scratch/ko_kp__en/config.json
models/text/active_backend.json
```

`active_backend.json`of `backend`go `from_scratch`The translator in the web-face-to-face section uses this model first.. If there is an exact same sentence in the translation memory, the translation memory takes priority..

Things to watch out for:

1. This path is not a tweak.
2. Does not use ready-made translation model weights.
3. We do not use off-the-shelf tokenizers.
4. Quality depends on data volume and approval quality..
5. The first few thousand sentence pairs are of experimental quality., The actual quality usually improves from hundreds of thousands of sentence pairs..

To check the command status of the learning path from the beginning, use the following commands:.

```powershell
python scripts\train_tokenizer.py --help
python scripts\train_transformer_from_scratch.py --help
```

To prepare small packages needed for learning for offline use:

```powershell
python scripts\prepare_offline_packages.py
python scripts\install_offline_packages.py
python scripts\check_training_dependencies.py
```

For offline installation on this registry wheel Files are archived.

```text
third_party/wheels/
```

`torch` same CUDA The learning package is already Python It is installed in the environment. To move to another offline computer, Python/CUDA correct for the edition `torch` wheelmust be prepared separately.

existing `scripts\train_neural_translation_model.py`is a legacy script that remains for fine tuning of existing models.. Because it is not the official path of this project, it does not run in the default state. `disabled` returns the status. Don't use this script if you don't need any fine-tuning.

The official learning instructions are as follows:.

```powershell
python scripts\train_tokenizer.py --vocab-size 16000
python scripts\train_transformer_from_scratch.py --direction ko_kp__en
python scripts\train_transformer_from_scratch.py --direction en__ko_kp
```

These commands do not call off-the-shelf model weights or tokenizers..

## What model to use

Text translation:

```text
NLLB / Marian / OpenNMT / CTranslate2 series
```

voice recognition:

```text
Whisper.cpp / Vosk
```

Image character recognition:

```text
PaddleOCR
```

voice synthesis:

```text
Piper or other offline TTS
```

The important thing is that the data comes before the model.. If users can build a Joseon language data base, this project will become closer to a Korean language translation machine as the data accumulates..

## DPRK-BERTWhere is it used?

DPRK-BERTis not a translation model, but a Korean language model.. Therefore, it is not used as a basic engine to directly generate translations..

Instead, you can use it for the next task.

1. Scoring whether the translation result came out as Korean
2. Extracting Korean terminology
3. Ranking of candidate translations
4. Chosun language writing style judgment
5. Data base quality inspection

analysis document:

```text
docs/DPRK_BERT_ANALYSIS.md
docs/URL_TO_TRAINING_WORKFLOW.md
docs/COMPLETE_PROJECT_BLUEPRINT.md
```

## project inspection

To check overall project status and learning readiness:

```powershell
python scripts\project_health.py
```

The translation quality evaluation report is created here..

```text
data/evaluation/translation_eval_report.tsv
```

## Development principles

1. Based on the Joseon Dynasty data base.
2. Human-reviewed translation pairs are most trusted.
3. All data import and conversion `scripts/` Make it repeatable with the Python script inside.
4. Large model files `models/`Put it in gitDo not upload to.
5. Offline operation is the default.
6. Stabilize the text translation first, Then add voice recognition and image recognition..

## next steps

1. Actual Joseon language original text `data/corpus/raw/`Put it in.
2. A pair of translations you created yourself `data/corpus/parallel/inbox/`Put it in.
3. `python scripts\bootstrap_data_pipeline.py`Run.
4. Check the translation results in the web interface..
5. Once enough data is accumulated, the first offline translation model is trained or fine-tuned..
