# AI Model Setup for HealthCare & Agriculture

## Overview
This document focuses on the **offline environment setup** workflow:
download a pre-trained model → set it up locally with no internet → fine-tune it on your country's local language data → deploy it for use in healthcare and agriculture apps.

---

## Workflow Overview

```
[Online — one time]          [Offline — your local machine]
Download model weights   →   Set up environment   →   Prepare local dataset
& tokenizer/config           (Python + CUDA)           in your language
                                     ↓
                             Fine-tune the model
                             on your local data
                                     ↓
                             Evaluate & test
                                     ↓
                             Export (ONNX / TFLite)
                                     ↓
                             Deploy in app / server
```

---

## Step 1 — Choose a Model to Download

Pick a model that supports multilingual or is easy to fine-tune on a new language.

### For NLP (Text — Symptoms, Agriculture Advice)

| Model | Size | Why Use It |
|-------|------|------------|
| `bert-base-multilingual-cased` | ~700MB | Supports 104 languages including most Asian/African/European languages |
| `xlm-roberta-base` | ~1.1GB | Better than mBERT for low-resource languages |
| `xlm-roberta-large` | ~2.2GB | Higher accuracy; needs more GPU RAM |
| `sentence-transformers/paraphrase-multilingual-MiniLM-L12-v2` | ~470MB | Lightweight, good for sentence similarity tasks |

**Recommended starting point**: `xlm-roberta-base` — strong cross-lingual transfer, well-documented.

### For Image (Disease Detection — Leaf, Skin, X-Ray)

| Model | Why Use It |
|-------|------------|
| `EfficientNet-B0` (TensorFlow/PyTorch) | Small, fast, accurate — good for mobile |
| `MobileNetV3` | Optimized for on-device inference |
| `ResNet-50` | Reliable baseline for transfer learning |
| `YOLOv8n` (Ultralytics) | Best for object detection (pest/weed localization) |

Image models do not depend on language — fine-tune them on your local crop/skin photos.

---

## Step 2 — Download While Online (Before Going Offline)

### NLP Model — Download Everything Needed

```python
# Run this ONCE while connected to the internet
from transformers import AutoTokenizer, AutoModelForSequenceClassification

model_name = "xlm-roberta-base"
save_path = "./models/xlm-roberta-base"

# Download tokenizer and model weights to local folder
tokenizer = AutoTokenizer.from_pretrained(model_name)
tokenizer.save_pretrained(save_path)

model = AutoModelForSequenceClassification.from_pretrained(model_name, num_labels=5)
model.save_pretrained(save_path)

print("Downloaded to:", save_path)
```

Files saved to `./models/xlm-roberta-base/`:
```
config.json
tokenizer.json
tokenizer_config.json
sentencepiece.bpe.model
pytorch_model.bin   (or model.safetensors)
```

### Image Model — Download Weights

```python
# EfficientNet via TensorFlow
import tensorflow as tf
base_model = tf.keras.applications.EfficientNetB0(weights='imagenet', include_top=False)
base_model.save('./models/efficientnet_b0_imagenet')

# OR YOLOv8 via Ultralytics
from ultralytics import YOLO
model = YOLO('yolov8n.pt')   # downloads yolov8n.pt to current directory
# move yolov8n.pt to ./models/
```

### Disable Hugging Face Internet Calls (Offline Mode)

```bash
# Set these environment variables before running any script offline
export TRANSFORMERS_OFFLINE=1
export HF_DATASETS_OFFLINE=1
```

Or in Python:
```python
import os
os.environ["TRANSFORMERS_OFFLINE"] = "1"
os.environ["HF_DATASETS_OFFLINE"] = "1"
```

---

## Step 3 — Offline Environment Setup

### Required Software (install while online, carry installers offline)

```
Python 3.10+
PyTorch 2.x  (with CUDA if GPU available)
transformers
datasets
scikit-learn
pandas
numpy
matplotlib
onnx
onnxruntime
```

### Install from Local Wheels (fully offline)

```bash
# Download all wheels to a folder while online
pip download transformers datasets torch torchvision -d ./pip_cache

# Install offline
pip install --no-index --find-links=./pip_cache transformers datasets torch torchvision
```

### Verify GPU (CUDA) Available

```python
import torch
print(torch.cuda.is_available())          # True if GPU ready
print(torch.cuda.get_device_name(0))      # e.g. "NVIDIA GeForce RTX 3060"
```

If no GPU: training will be slow but still works for small datasets — use a smaller model.

---

## Step 4 — Prepare Your Local Language Dataset

### NLP Dataset Format (CSV)

```
text,label
"ຄົນເຈັບມີໄຂ້ ແລະ ເຈັບຫົວ",0
"ຕົ້ນຂ້າວມີຈຸດສີນ້ຳຕານຢູ່ໃບ",1
...
```

- `text` = input in your local language
- `label` = integer class (e.g., 0=fever, 1=crop disease, etc.)

**Minimum recommended**: 200+ examples per class for fine-tuning a pretrained model.
**Better**: 1,000+ per class.

### Label Mapping File

```json
{
  "0": "fever_symptom",
  "1": "crop_brown_spot",
  "2": "crop_leaf_blight",
  "3": "healthy"
}
```

### Image Dataset Structure

```
./dataset/
    train/
        healthy/        (200+ images)
        brown_spot/     (200+ images)
        leaf_blight/    (200+ images)
    val/
        healthy/        (50+ images)
        brown_spot/     (50+ images)
        leaf_blight/    (50+ images)
```

---

## Step 5 — Fine-Tune the NLP Model (Offline)

```python
import os
os.environ["TRANSFORMERS_OFFLINE"] = "1"

from transformers import (
    AutoTokenizer,
    AutoModelForSequenceClassification,
    TrainingArguments,
    Trainer
)
from datasets import Dataset
import pandas as pd
import torch

# --- Load local model ---
MODEL_PATH = "./models/xlm-roberta-base"
tokenizer = AutoTokenizer.from_pretrained(MODEL_PATH)
model = AutoModelForSequenceClassification.from_pretrained(MODEL_PATH, num_labels=4)

# --- Load your local language dataset ---
df = pd.read_csv("./data/local_dataset.csv")   # columns: text, label
dataset = Dataset.from_pandas(df)

def tokenize(batch):
    return tokenizer(batch["text"], padding="max_length", truncation=True, max_length=128)

dataset = dataset.map(tokenize, batched=True)
dataset = dataset.train_test_split(test_size=0.2)

# --- Training config ---
training_args = TrainingArguments(
    output_dir="./output/finetuned_model",
    num_train_epochs=5,
    per_device_train_batch_size=16,
    per_device_eval_batch_size=16,
    evaluation_strategy="epoch",
    save_strategy="epoch",
    load_best_model_at_end=True,
    logging_dir="./logs",
    fp16=torch.cuda.is_available(),    # use half precision if GPU available
    no_cuda=not torch.cuda.is_available()
)

trainer = Trainer(
    model=model,
    args=training_args,
    train_dataset=dataset["train"],
    eval_dataset=dataset["test"]
)

trainer.train()
trainer.save_model("./output/finetuned_model")
tokenizer.save_pretrained("./output/finetuned_model")
print("Fine-tuning complete.")
```

---

## Step 6 — Fine-Tune the Image Model (Offline)

```python
import tensorflow as tf
import os

# --- Load pretrained base ---
base_model = tf.keras.models.load_model("./models/efficientnet_b0_imagenet")
base_model.trainable = False    # freeze base layers first

# --- Build classifier head ---
x = base_model.output
x = tf.keras.layers.GlobalAveragePooling2D()(x)
x = tf.keras.layers.Dense(128, activation='relu')(x)
outputs = tf.keras.layers.Dense(3, activation='softmax')(x)  # 3 classes
model = tf.keras.Model(inputs=base_model.input, outputs=outputs)

# --- Load local dataset ---
IMG_SIZE = (224, 224)
BATCH_SIZE = 32

train_ds = tf.keras.utils.image_dataset_from_directory(
    "./dataset/train", image_size=IMG_SIZE, batch_size=BATCH_SIZE)
val_ds = tf.keras.utils.image_dataset_from_directory(
    "./dataset/val", image_size=IMG_SIZE, batch_size=BATCH_SIZE)

# Normalize
normalization = tf.keras.layers.Rescaling(1./255)
train_ds = train_ds.map(lambda x, y: (normalization(x), y))
val_ds   = val_ds.map(lambda x, y: (normalization(x), y))

# --- Phase 1: Train head only ---
model.compile(optimizer='adam', loss='sparse_categorical_crossentropy', metrics=['accuracy'])
model.fit(train_ds, validation_data=val_ds, epochs=10)

# --- Phase 2: Unfreeze top layers and fine-tune ---
base_model.trainable = True
for layer in base_model.layers[:-20]:   # keep early layers frozen
    layer.trainable = False

model.compile(optimizer=tf.keras.optimizers.Adam(1e-5),
              loss='sparse_categorical_crossentropy', metrics=['accuracy'])
model.fit(train_ds, validation_data=val_ds, epochs=10)

model.save("./output/finetuned_image_model")
print("Image model fine-tuning complete.")
```

---

## Step 7 — Export for Deployment

### Export NLP Model to ONNX

```python
from transformers import AutoTokenizer
from optimum.onnxruntime import ORTModelForSequenceClassification

# Load fine-tuned model and export to ONNX
model = ORTModelForSequenceClassification.from_pretrained(
    "./output/finetuned_model", export=True)
model.save_pretrained("./output/model_onnx")
print("ONNX export done.")
```

### Export Image Model to TFLite (for mobile)

```python
import tensorflow as tf

model = tf.keras.models.load_model("./output/finetuned_image_model")
converter = tf.lite.TFLiteConverter.from_keras_model(model)
converter.optimizations = [tf.lite.Optimize.DEFAULT]   # quantize for smaller size
tflite_model = converter.convert()

with open("./output/model.tflite", "wb") as f:
    f.write(tflite_model)

print("TFLite export done. Size:", len(tflite_model) / 1e6, "MB")
```

---

## Step 8 — Run Inference Offline

### NLP Inference

```python
from transformers import pipeline
import os
os.environ["TRANSFORMERS_OFFLINE"] = "1"

classifier = pipeline(
    "text-classification",
    model="./output/finetuned_model",
    tokenizer="./output/finetuned_model"
)

result = classifier("ຄົນເຈັບມີໄຂ້ ແລະ ເຈັບຫົວ")
print(result)
# [{'label': 'fever_symptom', 'score': 0.94}]
```

### TFLite Image Inference

```python
import tensorflow as tf
import numpy as np
from PIL import Image

interpreter = tf.lite.Interpreter(model_path="./output/model.tflite")
interpreter.allocate_tensors()

input_details  = interpreter.get_input_details()
output_details = interpreter.get_output_details()

img = Image.open("./test_leaf.jpg").resize((224, 224))
img_array = np.expand_dims(np.array(img) / 255.0, axis=0).astype(np.float32)

interpreter.set_tensor(input_details[0]['index'], img_array)
interpreter.invoke()

output = interpreter.get_tensor(output_details[0]['index'])
predicted_class = np.argmax(output)
print("Predicted class:", predicted_class)
```

---

## Challenges

| Challenge | Description |
|-----------|-------------|
| Low-Resource Language | Your language may have very little training data available; need to collect manually |
| Tokenizer Coverage | Some tokenizers don't handle your script well — check tokenization quality first |
| No GPU Offline | Training without a GPU is slow; use small model + small batch size |
| Dataset Collection | Labeling medical/agriculture data in your language requires domain experts |
| Script/Font Issues | Some languages need special font rendering or right-to-left handling in the UI |
| Model Size vs Accuracy | Smaller models fit on mobile but lose accuracy; find the right trade-off |
| Class Imbalance | Local datasets often have very few examples of rare conditions |
| Evaluation Metrics | Need native speakers to verify model output quality, not just accuracy numbers |

---

## Folder Structure (Final)

```
project/
├── models/                         # Downloaded pretrained weights (offline copy)
│   ├── xlm-roberta-base/
│   └── efficientnet_b0_imagenet/
├── data/
│   ├── local_dataset.csv           # NLP dataset in your language
│   └── dataset/                    # Image dataset (train/val split)
├── output/
│   ├── finetuned_model/            # Fine-tuned NLP model
│   ├── finetuned_image_model/      # Fine-tuned image model
│   ├── model_onnx/                 # ONNX export (for server)
│   └── model.tflite                # TFLite export (for mobile)
├── pip_cache/                      # Offline pip wheels
├── train_nlp.py
├── train_image.py
└── inference_test.py
```

---

## Recommended Models by Language Family

| Language Type | Recommended NLP Model |
|---------------|----------------------|
| Southeast Asian (Thai, Lao, Khmer, Burmese) | `xlm-roberta-base` |
| Arabic / Urdu / Persian | `xlm-roberta-base` or `aubmindlab/bert-base-arabertv2` |
| Chinese / Japanese / Korean | `bert-base-chinese` or `xlm-roberta-base` |
| Hindi / Bengali / Tamil | `ai4bharat/indic-bert` or `xlm-roberta-base` |
| African languages | `xlm-roberta-base` (best general option) |
| European languages | `bert-base-multilingual-cased` |
