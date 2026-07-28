# Transformers Complete Understanding Guide
## Transformer learning with the actual code of this program — For beginners

> **before reading:** This document was written so that even people who don't know math can read it..
> All explanations are `src/model/transformer.py`Wow `src/inference/generate.py`Based on the actual code of.

---

## turn

1. [Why do you need a transformer?](#1-Transformers-why-Is it necessary?)
2. [View the entire flow at a glance](#2-all-flow-At a glance-view)
3. [1step — Convert letters to numbers](#3-1step--letters-by numbers-change)
4. [2step — Add location information](#4-2step--location-information-Add)
5. [3step — attention mechanism Q K V](#5-3step--caution-Mechanism-q-k-v)
6. [4step — Calculate attention score matrix](#6-4step--caution-score-procession-calculation)
7. [5step — causal screen](#7-5step--causal-shade)
8. [6step — Multi-headed caution](#8-6step--multiple-head-caution)
9. [7step — transmission network](#9-7step--pass on-net)
10. [8step — Linking blocks and residuals](#10-8step--Block and-residual-connection)
11. [9step — 8Meaning of layer repetition](#11-9step--8floor-repetitive-meaning)
12. [10step — Character creation process](#12-10step--letters-create-course)
13. [Temperature and Sampling](#13-temperature and-sampling)
14. [Size and limitations of this model](#14-This-model-scale and-limit)

---

## 1. Why do you need a transformer?

### problem: Computers don't know letters

Computers only know 0 and 1. "kimchi"Even if you show the text, the computer doesn't know if it's food., Is it a verb?, I have no idea it's a person's name.

So how can we create an artificial intelligence that speaks Korean??

**core idea:** every word **list of numbers(vector)**change to. If you calculate those numbers very precisely,, The computer interprets the meaning and grammar of words. "understand"It behaves as if.

Transformer performs this calculation **neural network structure**is.

### Why Transformers are Special

Before Transformer, letters were processed one by one in order.. "How to make kimchi" To process "kimchi" → "to" → "dipping" → "method" I had to read them one by one in order..

Transformers are different. **all letters at the same time** While watching, Calculate how related each letter is to all other letters at once.

```
Conventional method:  kimchi → to → dipping → method  (in order, slow)

transformer:
  kimchi ←→ to
  kimchi ←→ dipping        (Compare them all at the same time)
  kimchi ←→ method
  to   ←→ dipping
  ...
```

This is the core reason why Transformers are strong..

---

## 2. View the entire flow at a glance

user "Please tell me how to make kimchi." If you type:

```
┌─────────────────────────────────────────────────────────────┐
│  input: "Please tell me how to make kimchi."                          │
└──────────────────────┬──────────────────────────────────────┘
                       │
                       ▼
┌─────────────────────────────────────────────────────────────┐
│  [tokenizer]  letters → list of integer numbers                          │
│  "kimchi" → 342,  "dipping" → 156,  "method" → 891  ...           │
└──────────────────────┬──────────────────────────────────────┘
                       │
                       ▼
┌─────────────────────────────────────────────────────────────┐
│  [Token Embedding]  number → 512vector of dog numbers                     │
│  342 → [0.12, -0.34, 0.89, ..., 0.05]  (512dog)              │
└──────────────────────┬──────────────────────────────────────┘
                       │
                       ▼
┌─────────────────────────────────────────────────────────────┐
│  [Location Embedding]  "What letter am I?" Add information              │
└──────────────────────┬──────────────────────────────────────┘
                       │
                       ▼
┌─────────────────────────────────────────────────────────────┐
│  [block × 8floor]                                              │
│   Every floor:  layer normalization → attention mechanism → Residual concatenation                   │
│            layer normalization → transmission network   → Residual concatenation                   │
└──────────────────────┬──────────────────────────────────────┘
                       │
                       ▼
┌─────────────────────────────────────────────────────────────┐
│  [LM head]  vector in last position → 16,384dog candidate probability          │
└──────────────────────┬──────────────────────────────────────┘
                       │
                       ▼
┌─────────────────────────────────────────────────────────────┐
│  [sampling]  Select the next letter based on probability → Append to input      │
│  If you repeat the above process up to 256 times, you will get a complete answer.             │
└─────────────────────────────────────────────────────────────┘
```

---

## 3. 1step — Convert letters to numbers

### What a tokenizer does

Our program first converts letters into integer numbers..

```python
# src/tokenizer/tokenizer.py processed in
"How to make kimchi" → [1, 342, 891, 156, 782, 2]
#                       BOS  letters...              EOS
```

- `1` = BOS (Post start indicator)
- `2` = EOS (Show end of post)
- the rest = word number (0~16,383 one of)

### Token Embedding — number to 512 digits

```python
# transformer.py 90row
self.tok_emb = nn.Embedding(cfg.vocab_size, cfg.n_embd)
#                           Vocabulary count=16,384  dimension=512
```

`nn.Embedding` silver **big ticket**Everything. where the line number is the word number and, There are 512 numbers in each row.

```
number  │  Number 1 Number 2 Number 3  ...  number 512
──────┼──────────────────────────────────────────
  0   │  0.00    0.00    0.00  ...  0.00   ← PAD
  1   │  0.31   -0.12    0.87  ...  0.44   ← BOS
  2   │ -0.55    0.23   -0.11  ...  0.09   ← EOS
 342  │  0.12   -0.34    0.89  ...  0.05   ← "kimchi"
 891  │ -0.22    0.71    0.03  ...  0.44   ← "method"
 ...  │  ...
16383 │  0.77   -0.41    0.12  ...  0.88
```

Initially, these 512 numbers are random.. **While going through training** Words with similar meanings have similar number patterns.

```
before training:   "kimchi" vector ↔ "cabbage" vector  →  far away
After training:   "kimchi" vector ↔ "cabbage" vector  →  the distance is close  ← Both vegetable ingredients
           "kimchi" vector ↔ "running" vector →  far away    ← Not relevant
```

---

## 4. 2step — Add location information

### Why do you need location information?

Transformers don't know their original order.. all letters **at the same time** Because I see.

"i eat rice"Wow "I eat rice" can't distinguish.

So the location embedding **It adds**:

```python
# transformer.py 91row
self.pos_emb = nn.Embedding(cfg.block_size, cfg.n_embd)
#                           maximum position=1,024  dimension=512
```

```python
# transformer.py 132~133row
pos = torch.arange(T, device=idx.device, dtype=torch.long)
x = self.drop(self.tok_emb(idx) + self.pos_emb(pos))
```

In reality, we add it like this::

```
"kimchi"is position 0  →  kimchi_vector + location0_vector  =  final_vector_A
"to"is the 1st position  →  to_vector   + location1_vector  =  final_vector_B
"Soak"is position 2  →  Soak_vector + location2_vector  =  final_vector_C
```

Even if the same word is in a different position, it becomes a different vector..

---

## 5. 3step — attention mechanism Q K V

code: `transformer.py` 25~57row (`CausalSelfAttention`)

### What is an attention mechanism?

**attention mechanism(attention mechanism)**is "What other letters should we refer to to explain this letter?" calculate.

yes: "it" When dealing with the pronoun → came before "kimchi"You should refer to.

### Q, K, V — three roles

Each word vector(512dimension)Convert into three:

```python
# transformer.py 34row
self.qkv = nn.Linear(cfg.n_embd, 3 * cfg.n_embd, bias=cfg.bias)
#           512Dimension input  →  1,536dimension output (512 × 3)
```

Divide this into three pieces:

```python
# transformer.py 41row
q, k, v = qkv.split(self.n_embd, dim=-1)
# q = 512 front, k = Medium 512, v = back 512
```

| symbol | Korean name | role |
|------|------------|------|
| **Q** | query | "What information do I need?" |
| **K** | key | "What information can I provide??" |
| **V** | value   | "What I will actually convey" |

### Understanding the library analogy

```
┌────────────────────────────────────────────┐
│  library search system                          │
│                                            │
│  Q (query)  = The title of the book I'm looking for              │
│             yes: "How to make kimchi"           │
│                                            │
│  K (key)  = Index tag attached to each book on the bookshelf      │
│             bookA: "kimchi, cabbage, fermentation"          │
│             bookB: "running, strength, exercise"        │
│             bookC: "soybean paste, soy sauce, fermentation"          │
│                                            │
│  QWow Kcompare → bookAis the best fit          │
│                                            │
│  V (value)    = bookABring out the actual contents of      │
└────────────────────────────────────────────┘
```

---

## 6. 4step — Calculate attention score matrix

"making kimchi" 4Let's say we're processing two letters..

### 1step: Q × Kᵀ Calculate score with

```python
# transformer.py 49~53row (Done internally)
# score = Q × Kᵀ ÷ √64
```

of each letter Qand all letters KMultiply by → (4×4) A score matrix appears.:

```
          of kimchiK  ofK  of soakingK  ofK
of kimchiQ  [ 8.2    1.1    3.4     0.8 ]
ofQ    [ 0.5    7.9    0.3     2.1 ]
of soakingQ  [ 4.1    0.7    9.5     1.2 ]
ofQ    [ 1.3    3.8    5.6     8.1 ]
```

the bigger the number "These two letters are very related to each other".

### 2step: Convert to probability with softmax

Set the number in each row to 0~1 change between, Makes the sum of one row equal to 1:

```
          of kimchiK  ofK  of soakingK  ofK   total
of kimchiQ  [ 0.83    0.03   0.12    0.02 ]  = 1.00
ofQ    [ 0.05    0.87   0.04    0.04 ]  = 1.00
of soakingQ  [ 0.08    0.01   0.88    0.03 ]  = 1.00
ofQ    [ 0.03    0.06   0.21    0.70 ]  = 1.00
```

→ "of soakingQ" If you look at the row: "dipping" is oneself(K)Pay most attention to, "kimchi" Also pay a little attention to.

### 3step: weight × V = output

```
output_Soak = 0.08 × kimchi_V + 0.01 × to_V + 0.88 × Soak_V + 0.03 × is_V
```

"dipping" new expressions of letters = mainly yourself, there "kimchi" a bit of information mixed up.

Perform this calculation in one line of code:

```python
# transformer.py 49~53row
y = F.scaled_dot_product_attention(
    q, k, v,
    dropout_p=self.dropout if self.training else 0.0,
    is_causal=True,    # ← future screen (Description below)
)
```

---

## 7. 5step — causal screen

### Why do you need covers?

When training, the model "Next Scrabble" do.

"making kimchi **[?]**" in `[?]` go "method" You have to get it right.

But if the model could see the future? "method" If you guess beforehand, you won't learn anything..

So `is_causal=True` makes a triangle shade:

```
          making kimchi
kimchi   [  ✓    ✗    ✗    ✗  ]  ← "kimchi" can only see itself
to     [  ✓    ✓    ✗    ✗  ]  ← "to" silver "kimchi"and only you can see it
Soak   [  ✓    ✓    ✓    ✗  ]  ← "Soak" Up to the first three
is     [  ✓    ✓    ✓    ✓  ]  ← "is" can be seen by all

✓ = can see  ✗ = blocked by a screen (-∞ Fill with)
```

This triangle shape **Lower triangle matrix screen**It is called.

Points are awarded for locations where there are blinds. `-∞` (minus infinity)made with → After going through softmax, it becomes 0. → The location is completely ignored.

---

## 8. 6step — Multi-headed caution

### The reason why one attention is not enough

There are many types of relationships between letters.:

```
"How to make kimchi"

grammatical relationships:  "method"  ←→  "silver" (nominative particle)
meaning relationship:  "dipping" ←→ "kimchi" (object of action)
referential relationship:  "it"  ←→  noun in previous sentence
```

It is difficult to catch all these types well at the same time with one attention..

### 8Simultaneous analysis with dog heads

```python
# transformer.py 29~31row
self.n_head  = cfg.n_head                   # = 8
self.head_dim = cfg.n_embd // cfg.n_head    # = 512 ÷ 8 = 64
```

5128 pieces of dimension(64Dimension by dimension)Divide into each head.:

```python
# transformer.py 44~46row
q = q.view(B, T, self.n_head, self.head_dim).transpose(1, 2)
#   form: (bundle, number of characters, 512) → (bundle, 8, number of characters, 64)
k = k.view(B, T, self.n_head, self.head_dim).transpose(1, 2)
v = v.view(B, T, self.n_head, self.head_dim).transpose(1, 2)
```

Each head has its own Q, K, V by weight **different things**learn:

```
head 1  →  grammatical relationships  ("given-predicate" connection)
head 2  →  meaning relationship  ("ingredients-act" connection)
head 3  →  location proximity  (letters that are close together)
head 4  →  referential relationship  ("it" ↔ noun)
head 5~8 → Various other patterns
```

8By combining the results of the dog head, it becomes 512 dimensions again.:

```python
# transformer.py 55~56row
y = y.transpose(1, 2).contiguous().view(B, T, C)  # 8Merge Dogs (B, T, 512)
y = self.resid_dropout(self.proj(y))               # 512→512 linear transformation
```

---

## 9. 7step — transmission network

code: `transformer.py` 60~68row

```python
class FeedForward(nn.Module):
    def __init__(self, cfg):
        self.fc1 = nn.Linear(cfg.n_embd, cfg.ffn_dim)   # 512 → 2,048
        self.fc2 = nn.Linear(cfg.ffn_dim, cfg.n_embd)   # 2,048 → 512

    def forward(self, x):
        return self.dropout(self.fc2(F.gelu(self.fc1(x))))
```

### why 2,048Do you increase it and then decrease it again?

The attention mechanism is between letters. **relationship**calculate.
The transmission network is **content**handle.

```
┌──────────────────────────────────────────────┐
│  The role of the transmission network                               │
│                                              │
│  Attention mechanism output: "kimchi"512 numbers about      │
│           ↓                                  │
│  fc1: 512 → 2,048  (4x expansion of thinking space)       │
│           ↓                                  │
│  GELU: Nonlinear processing  (Complex pattern learning)         │
│           ↓                                  │
│  fc2: 2,048 → 512  (Compress to the next layer)     │
└──────────────────────────────────────────────┘
```

**2,048Reason for increasing:** 512Complex relationships that cannot be expressed in one dimension are temporarily processed in a larger space.. in this wide space **factual knowledge**This is saved.

```
example: What the transmission network learns

"kimchi" + "soak" → material(cabbage, salt, red pepper powder)is needed
"protein" + "exercise" → Involved in muscle growth
"Joseon language" + "grammar" → investigation rules
```

### GELUWhat is

GELUis a function that almost ignores negative numbers and passes positive numbers almost as is..

```
input < 0:   output ≈ 0    (You don't need this information now)
input = 0:   output = 0
input > 0:   output ≈ input (This information is important)
```

**Reason why you can’t just have a linear transformation:** Even if you stack a linear function 100 times, it still equals one linear function.. nonlinear function(GELU)Complex knowledge can be expressed only when there is.

---

## 10. 8step — Linking blocks and residuals

code: `transformer.py` 71~82row

```python
class Block(nn.Module):
    def __init__(self, cfg):
        self.ln_1 = nn.LayerNorm(cfg.n_embd)   # layer normalization 1
        self.attn = CausalSelfAttention(cfg)    # attention mechanism
        self.ln_2 = nn.LayerNorm(cfg.n_embd)   # layer normalization 2
        self.ffn  = FeedForward(cfg)            # transmission network

    def forward(self, x):
        x = x + self.attn(self.ln_1(x))        # ← Residual concatenation 1
        x = x + self.ffn(self.ln_2(x))         # ← Residual concatenation 2
        return x
```

### layer normalization — stabilize the numbers

Training becomes unstable if numbers are too large or too small during calculation..

Layer normalization means that the numbers in each layer are **average 0, variance 1** adjust nearby.

```
Before layer normalization:  [0.0001, -523.0, 7823.5, -0.003, ...]  (The size is jagged)
After layer normalization:  [-0.12,   -1.54,   2.31,  -0.88, ...]  (size is stable)
```

### Residual concatenation — Prevents tilt loss

`x = x + self.attn(...)` in `+` is the key.

**When there is no:**
```
input → attention mechanism → transmission network → output
         ↑ The slope gets smaller here
         ↑ 8After passing the floor, it becomes almost 0. → not trained
```

**when there is (Residual concatenation):**
```
input ─────────────────────────────→ ⊕ → next floor
  └──→ layer normalization → attention mechanism ─────────┘

Gradient flows directly through residual path → 8Stable training on floors
```

metaphor: Every floor "original information + new thing learned" Combine. Add new information without losing the original information.

---

## 11. 9step — 8Meaning of layer repetition

code: `transformer.py` 135~136row

```python
for block in self.blocks:   # blocks = 8dog Block
    x = block(x)
```

8As you pass through each block one by one, the expression becomes more and more elaborate.:

```
input: "How to make kimchi"  →  (1, 5, 512) shape vector

┌──────────────────────────────────────────────────────────┐
│  Block 1st floor                                               │
│  → Understand basic relationships between adjacent letters                    │
│  → "to" This "kimchi" Recognize that this is an investigation that came later.                    │
└──────────────────┬───────────────────────────────────────┘
                   │
                   ▼ (1, 5, 512) — The shape is the same, Content has changed
┌──────────────────────────────────────────────────────────┐
│  Block 2nd floor                                               │
│  → Identify relationships between more distant letters                             │
│  → "method" This "dipping" Recognize that it is the object of                   │
└──────────────────┬───────────────────────────────────────┘
                   │
                   ▼
┌──────────────────────────────────────────────────────────┐
│  block 3~4floor                                             │
│  → Understanding Grammar Structures                                          │
│  → given-predicate-Identify object relationships                               │
└──────────────────┬───────────────────────────────────────┘
                   │
                   ▼
┌──────────────────────────────────────────────────────────┐
│  block 5~6floor                                             │
│  → Understanding Semantic Relationships                                        │
│  → "soak" = Recipes that require a fermentation process                      │
│  → "kimchi" = cabbage, red pepper powder, need salt                     │
└──────────────────┬───────────────────────────────────────┘
                   │
                   ▼
┌──────────────────────────────────────────────────────────┐
│  block 7~8floor                                             │
│  → Comprehensive context                                        │
│  → Create a final expression to determine the next letter           │
└──────────────────────────────────────────────────────────┘
                   │
                   ▼ final: (1, 5, 512)
```

---

## 12. 10step — Character creation process

code: `src/inference/generate.py`

### GPTHow to calculate next letter probability

```python
# transformer.py 148~149row (When inferring)
logits = self.lm_head(x[:, [-1], :])  # Last position only → (1, 1, 16384)
```

LM head(lm_head)receives a 512-dimensional vector of 16,384change to number.
this 16,384Convert numbers to probabilities using softmax..

```
vocabulary number  │  probability
──────────┼──────────────────────────────
  0 (PAD) │  0.0000
  1 (BOS) │  0.0001
  2 (EOS) │  0.0023
  ...
 342 (material) │  0.0345
 891 (first) │  0.2100   ← high
 156 (ready) │  0.1850   ← high
 782 (cabbage) │  0.1500   ← high
  ...
```

### Repeat by picking and adding one letter at a time

```python
# generate.py 111~128row
for _ in range(max_new_tokens):         # Repeat up to 256 times

    # 1. Run the model → 16,384dog odds
    nxt = sample_token(model, idx, ...)

    # 2. EOSWhen appears, the article ends
    if token_id == EOS_ID:
        break

    # 3. Add selected letters to the end of input
    idx = torch.cat([idx, nxt], dim=1)
    #     Input is now one character longer

    # 4. Print the text on the screen (gradually)
    yield text[len(last_text):]
```

Step-by-step example:

```
repeat 1:  input: "How to make kimchi"  →  output: "first"
         Entered: "How to make kimchi first"

repeat 2:  input: "How to make kimchi first"  →  output: "cabbage"
         Entered: "How to make kimchi: First, cut the cabbage."

repeat 3:  input: "How to make kimchi: First, cut the cabbage."  →  output: "Get ready"
         ...

repeat N:  EOS Stop when token appears
```

---

## 13. Temperature and Sampling

### temperature (Temperature)

```python
# generate.py 78row
logits = logits / temperature   # temperature = 0.9 default
```

Temperature is a probability distribution **sharpness**adjust.

```
temperature = 0.5 (low, more certain):
  candidate1 "first"    →  0.72   ← Almost always choose this
  candidate2 "First of all"    →  0.18
  candidate3 "at first"  →  0.07
  candidate4 guitar     →  0.03

temperature = 0.9 (default):
  candidate1 "first"    →  0.45
  candidate2 "First of all"    →  0.28
  candidate3 "at first"  →  0.17
  candidate4 guitar     →  0.10

temperature = 1.5 (high, more random):
  candidate1 "first"    →  0.28
  candidate2 "First of all"    →  0.25
  candidate3 "at first"  →  0.24
  candidate4 guitar     →  0.23
```

### Top-K sampling

```python
# generate.py 36~38row
# All but the top 50 candidates are left. -∞ made with
top_k = 50
kth = torch.topk(logits, top_k).values[..., -1, None]
logits = torch.where(logits < kth, float("-inf"), logits)
```

16,38416 of the least likely,334Get rid of the dog altogether..
Prevents the possibility of incorrect letters being selected.

### Top-P nuclear sampling

```python
# generate.py 40~52row
# The cumulative sum of probabilities is 95%Only candidates left until .
top_p = 0.95
```

```
Sort candidates in order of probability:
  "first"    →  0.45  cumulative: 0.45
  "First of all"    →  0.28  cumulative: 0.73
  "at first"  →  0.17  cumulative: 0.90
  "first"  →  0.05  cumulative: 0.95  ← I leave it here
  Afterwards, all candidates were eliminated.
```

### repetition penalty (Repetition Penalty)

```python
# generate.py 21~29row, penalty = 1.15
score = torch.where(score < 0, score * penalty, score / penalty)
```

The probability of a letter already created is 1.15Divide by → probability decreases.

**When there is no:**
```
"kimchi kimchi kimchi kimchi kimchi..." (repeating the same words endlessly)
```

**when there is (penalty = 1.15):**
```
"Kimchi is a representative fermented food of Joseon., Red pepper powder on cabbage..."
```

---

## 14. Size and limitations of this model

### Calculate number of parameters

```python
# config.py 25~33row of num_params_estimate() function

Token Embedding:    16,384 × 512  =  8,388,608
Location Embedding:     1,024 × 512  =    524,288

1 block:
  caution QKV:     512 × 1,536  =    786,432
  Attention output:    512 × 512    =    262,144
  Delivery network expansion:  512 × 2,048  =  1,048,576
  network reduction: 2,048 × 512   =  1,048,576
  Subtotal:                      =  3,145,728

8 blocks:  3,145,728 × 8  = 25,165,824

LM head: weight sharing(tie_weights=True) → 0 add

───────────────────────────────────────
total: about 34,078,720 ≈ 3,400Only parameters
───────────────────────────────────────
```

this 3,400The number of 10,000 contains Korean cooking methods and exercise knowledge..

### why 3,400Is it only a parameter?

Our graphics processing unit(RTX 5070, Manifestation memory 12 GB)It is a scale that can be learned in.
Creating larger models leads to manifest memory overflow errors..
For detailed explanation `explain.md` Note.

### Difference between training and inference

```
When training: Simultaneously calculate the loss of all letter positions
  logits = self.lm_head(x)            # (B, T, 16384) — all locations

When inferring: Count only the last position (memory saving)
  logits = self.lm_head(x[:, [-1], :])  # (B, 1, 16384) — Only the last one
```

---

## key summary table

| component | What to do | code location | Input/Output Form |
|----------|---------|----------|-----------|
| Token Embedding | number → 512dimension vector | `transformer.py:90` | (B,T) → (B,T,512) |
| Location Embedding | Add location information | `transformer.py:91` | (B,T) → (B,T,512) |
| layer normalization | Number size stabilization | `transformer.py:74` | (B,T,512) → (B,T,512) |
| QKV linear transformation | 512 → Q, K, V (512 each) | `transformer.py:34` | (B,T,512) → (B,T,1536) |
| multiple head separation | 512 → 8dog × 64 | `transformer.py:44` | (B,T,512) → (B,8,T,64) |
| causal attention | future screen + score calculation | `transformer.py:49` | (B,8,T,64) → (B,8,T,64) |
| merge heads | 8×64 → 512 | `transformer.py:55` | (B,8,T,64) → (B,T,512) |
| Residual concatenation | Prevent tilt loss | `transformer.py:80` | (B,T,512) + (B,T,512) |
| transmission network | 512 → 2048 → 512 | `transformer.py:60` | (B,T,512) → (B,T,512) |
| LM head | 512 → 16,384 probability | `transformer.py:95` | (B,1,512) → (B,1,16384) |
| temperature adjustment | Probability Sharpness Control | `generate.py:78` | Softmax input scale |
| Top-K/P | Eliminate bad candidates | `generate.py:33` | low probability → -∞ |
| repetition penalty | Suppress repeated letters | `generate.py:22` | Parasitic Token Probability ÷ 1.15 |

> **one line summary:** Transformer is "Every letter updates its expression by referencing all other letters simultaneously." is an eight-layered caution mechanism.. After fully understanding the context through this process,, Predict the most natural letter that will come next.
