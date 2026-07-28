# AI Platform Comparison — Comparison analysis of major artificial intelligence platforms
# Which platform wins where?, What is the reason?

---

## Overview / Overview

Currently, the world's most advanced artificial intelligence language models:

| platform | company | representative model | Public or not |
|--------|------|----------|----------|
| **GPT / ChatGPT** | OpenAI (united states) | GPT-4o, o3 | private |
| **Gemini** | Google DeepMind (united states) | Gemini 2.0 Ultra | private |
| **Copilot** | Microsoft (united states) | GPT-4o based | private |
| **Claude** | Anthropic (united states) | Claude Sonnet 4.6 | private |
| **DeepSeek** | DeepSeek AI (china) | DeepSeek-V3, R1 | **full disclosure** |
| **LLaMA** | Meta (united states) | LLaMA-3.3 70B | **full disclosure** |

---

## 1. OpenAI — GPT-4o, o1, o3

### core strengths

**reasoning ability (o1/o3 series)**This is the most advanced.

OpenAIThe main reason why it is ahead of other platforms:

#### 1-1. RLHF (Human feedback reinforcement learning)

```
general language model(GPT-3) + human preference data
    ↓
Map tweaks (SFT: Supervised Fine-Tuning)
    ↓
Learning reward model (Human response pair preference assessment)
    ↓
PPO Reinforcement learning (Maximize the score of the reward model)
    ↓
InstructGPT / ChatGPT
```

RLHFReason why it was revolutionary: The model "correct answer"Not this "Human's Favorite Answer"Let's learn.

#### 1-2. Thought chain reinforcement learning (o1/o3 — Chain-of-Thought RL)

```
problem → [internal thinking signs] → final answer
```

- o1Before answering, "accident ticket"Create and review yourself
- Improve reasoning ability only through pure reinforcement learning without human review
- math, science, Achieve human expert level in coding

#### 1-3. Mixed expert structure (MoE: Mixture of Experts)

GPT-4is probably MoE structure (unconfirmed, OpenAI private):
```
input → router(Router) → 8Choose 2 of our dog experts → processing
```
- full parameters: ~1.76Joe (estimate)
- Actual active parameters: ~220billion (per processing)
- result: Expressive power of large models + Speed of small models

#### 1-4. Summary of Strengths

| field | level |
|------|------|
| inference/logic (o3) | ★★★★★ highest level |
| math | ★★★★★ |
| coding | ★★★★☆ |
| follow directions | ★★★★★ |
| safety | ★★★☆☆ Controversial |
| cost | Expensive |

---

## 2. Google Gemini — 2.0 Ultra/Pro/Flash

### core strengths

**Long context processing**Wow **Multimedia Understanding(Multimodal)**The most advanced in.

#### 2-1. 1,000,000 tag context (1M Token Context)

general model: 4,096~200,000 stamp processing
Gemini 1.5 Pro: **1,000,000 tag** = about 750,000 word = 10 novels

Technical reason why this is possible:
```
general attention(Attention): Computation amount = O(n²)
  n=1,000,0001 trillion calculations → impossible

Geminisolution: Linear attention transformation + Special location encoding
  Computation amount = O(n) → possible
```

#### 2-2. Multimedia original learning (Native Multimodal)

GPT-4Add visual processing to the text mockup later.  
Geminiis the text from the beginning, image, sound, Learn videos together:

```
[text tag] [image patch] [audio spectrum] [video frame]
    ↓                ↓              ↓                ↓
                Converter integrated processing
```

#### 2-3. TPU v5 dedicated hardware

- GoogleThis self-designed TPU(Tensor Processing Unit)learn by
- H100In more specific matrix operations, 2~4ship fast
- Google For internal use only

#### 2-4. Summary of Strengths

| field | level |
|------|------|
| Long context processing | ★★★★★ highest level |
| Multimedia Understanding | ★★★★★ |
| Search integration | ★★★★★ (Google Search integration) |
| inference | ★★★★☆ |
| coding | ★★★★☆ |
| cost | Normal |

---

## 3. Microsoft Copilot

### entity

Copilotsilver **unique AI not a model**.  
OpenAIof GPT-4o model Microsoft It is integrated into the service.

```
user input
    ↓
Bing Search (Real-time web information retrieval) — RAG way
    ↓
Search results + user input GPT-4opassed to
    ↓
GPT-4o Generate response
    ↓
Microsoft display in format
```

#### CopilotThe actual strengths of

Not a reader algorithm **Ecosystem integration**this strength:

| integrated service | function |
|------------|------|
| Microsoft 365 | Word, Excel, PowerPoint automation |
| GitHub Copilot | Code autocompletion (GPT-4 based) |
| Windows 11 | Built-in operating system AI |
| Bing Search | Real-time web information |
| Azure | Enterprise Cloud Integration |

**technically GPT-4Same as**But, Leading the way in corporate environment integration.

---

## 4. Anthropic Claude — Sonnet 4.6 (This AI)

### core strengths

**safety**and **coding**ahead of, We use a unique learning method.

#### 4-1. Constitutional AI (constitutional artificial intelligence)

OpenAIof RLHF: **human**rate this response → Cost is high and scale is limited

Anthropicof RLAIF (AI Feedback Reinforcement Learning):
```
1. principles(Constitution) definition:
   "must not be harmful"
   "Be honest"
   "should be helpful"

2. AI The model evaluates the response itself:
   modelAgenerates a response
   → The same model responds and criticizes based on its principles.
   → Improve response based on criticism
   → Retrain with improved responses

3. Sort automatically without human evaluators(Alignment) achieve
```

Advantages of this method:
- No need for thousands of human evaluators → Significant cost savings
- Consistent action with clear principles
- Easy to scale

#### 4-2. long context (200K tag)

Claude 3 After: 200,000 Ticket processing possible (2 novel volumes)

#### 4-3. coding skills

SWE-bench(Real-world software bug fixing benchmarks)highest level in:
- Claude Sonnet 4.6 / Opus 4.7: industry leader

#### 4-4. Summary of Strengths

| field | level |
|------|------|
| safety/rejection quality | ★★★★★ highest level |
| coding | ★★★★★ |
| long context | ★★★★☆ |
| honesty | ★★★★★ |
| inference | ★★★★☆ |
| Korean | ★★★☆☆ (Korean language is weak) |

---

## 5. DeepSeek — V3, R1

### core strengths

**Algorithm Innovation**A platform that achieves the highest level of performance at low cost.  
2025year AI The company that made the most shocking announcement in the industry.

#### 5-1. MLA (Multi-head Latent Attention) — core innovation

existing MHA(Multi-Head Attention)the problem:
```
When inferring KV Cache(key-value store) = n_head × d_head × sequence_length × 2
Hundreds in long context GB need to remember
```

DeepSeekof MLA solution:
```
KV Cachecompresses into a low-dimensional latent vector.:
  existing: KV Cache = 100% use memory
  MLA: KV Cache = 7% use memory (93% savings!)

formula:
  c_KV = W_DKV × h   (h: Hidden vector, W_DKV: compression matrix)
  K = W_UK × c_KV    (restore)
  V = W_UV × c_KV    (restore)
```

This allows much longer context processing with the same memory..

#### 5-2. MoE + efficient routing

DeepSeek-V3: 6,710billion parameters, But only 37 billion active per processing

```
full parameters: 671B
number of experts: 256dog
Active Experts Per Processing: 8dog (Top-8 routing)
Actual usage parameters per processing: 37B (5.5%)

result: 37B 671 at the cost of calculating sizeB Use your knowledge
```

Routing innovation — load balance(Load Balancing) Evenly activated by experts without:
```
existing: Router selects only certain experts → Some experts overload
DeepSeek: Automatically achieves balance without secondary losses
```

#### 5-3. DeepSeek-R1 — Pure reinforcement learning inference

```
OpenAI o1: SFT(Supervised learning) → RLHF → Improved reasoning skills
DeepSeek-R1: SFT None → pure RLDeveloping reasoning skills just by(Emergence)!
```

result: the model itself "chain of accidents"found.  
Even if humans do not teach "Think first and then answer"The behavior appears naturally.

This is an amazing reason:  
"Even if you don't teach a specific behavior, just give a reward signal. AIdiscovers the optimal strategy on its own"is the evidence.

#### 5-4. Compare learning costs

| model | Estimated learning cost |
|------|---------------|
| GPT-4 | $60,000,000+ |
| Gemini Ultra | $40,000,000+ |
| Claude 3 Opus | $20,000,000+ |
| **DeepSeek-V3** | **$5,576,000** |

Same performance as 1/10 Why achieve it at cost:
1. MLAMemory usage 93% savings
2. FP8 Use learning precision
3. efficient MoE routing
4. Reduce labor and power costs in China

#### 5-5. full disclosure (Open Source)

- model weights: hugging face(Hugging Face)Free to public
- How to learn: Detailed disclosure in thesis
- Anyone can run it on their own server

#### 5-6. Summary of Strengths

| field | level |
|------|------|
| math/coding | ★★★★★ OpenAI o3equivalent to |
| cost-effective | ★★★★★ highest level |
| Algorithm Innovation | ★★★★★ |
| openness | ★★★★★ |
| safety | ★★☆☆☆ (Insufficient censorship by US standards) |
| Korean/Joseon language | ★★★☆☆ |

---

## 6. Summary of key differences by platform

### what AIDoes it lead the way??

```
performance determinants (In order of importance):

1above. Size and quality of learning materials          ████████████ 40%
2above. algorithm/structural innovation             ████████     30%
3above. computational resources(GPU/TPU number)          ██████       20%
4above. sort(Alignment) method           ████         10%
```

### When the algorithm is ahead vs. material/When calculations are ahead

| Reasons for being ahead | Applicable platform | core innovation |
|------------|------------|----------|
| **algorithm** | DeepSeek | MLA (KV Cache 93% savings), MoE Routing innovation, pure RL inference |
| **algorithm** | OpenAI | RLHF First commercialization, o1 chain of accidents RL |
| **algorithm** | Anthropic | Constitutional AI (RLAIF), safety alignment |
| **data scale** | Google | full internet resources + YouTube + book |
| **computational resources** | Google | TPU v5 self-designed, most computing |
| **context length** | Google | 1M Fish mark linear attention |
| **ecosystem** | Microsoft | GPT-4 + Office/Windows/GitHub integration |

### Chronology of major structural innovations

```
2017  Transformer (google) — modern AIthe basis of
2018  GPT-1 (OpenAI) — Application to language models
2019  GPT-2 — Proving the power of scale-up
2020  GPT-3 (175B) — The era of large-scale language models begins
2022  InstructGPT + RLHF — Learning human-desired responses
2022  ChatGPT — general public AI The beginning of an era
2023  GPT-4 — MoE estimate, multimodal
2023  LLaMA (Meta) — open source revolution
2023  Claude 2 — Constitutional AI maturity
2024  Gemini 1.5 — 1M tag context
2024  DeepSeek-V2 — MLA innovation, KV Cache 93% savings
2024  LLaMA-3 — The best in open source
2025  DeepSeek-V3 — GPT-4class performance, 1/10 cost
2025  DeepSeek-R1 — pure RLby o1class inference
2025  Claude Sonnet 4.6 — The best in coding (Currently this AI)
2025  GPT o3 — highest level inference
```

---

## 7. our project(Joseon language AI)comparison with

### How our model differs from the above platforms

| item | our model | GPT-4/Claude etc. |
|------|----------|----------------|
| learning materials | Korean language only | Mixing languages around the world |
| number of parameters | 30M~350M | 7B~1,760B |
| learning cost | power consumption | millions~tens of millions of dollars |
| Korean language expertise | **specialization** | general |
| Public or not | completely self-contained | outside API dependence |
| offline use | **possible** | internet required |
| Privacy protection | **completely** | External server transfer |

### Areas where our model can lead

1. **Specialization in Joseon language**: Other platforms have very little data on the Joseon language.
2. **completely offline**: No internet connection required
3. **full ownership**: Model weights are on your computer
4. **Personal information**: Conversations don't go out
5. **Free fine tuning**: You can re-study at any time with additional materials

### 100Expected level after learning with Giga Joseon language materials

| Compare | level |
|------|------|
| Understanding Korean Grammar | GPT-3.5 level possible |
| food/Exercise Area Answers | Professional level available |
| normal conversation | GPT-2~3 level |
| reasoning ability | limited (RLHF None) |
| Safety Alignment | Not applied (You can add more if you want) |

### Technologies that can be added to improve performance

| technology | Description | Difficulty level |
|------|------|--------|
| RLHF | Humans choose good responses → Reinforcement learning | high |
| DPO | preference/Direct learning with non-preferred pairs | middle |
| Thought chain learning | "question: X\nthink: ...\nanswer: Y" format data | low |
| Flash Attention | Attention calculation speed 2~4belly enhancement | middle |
| Quantization(4-bit) | model size 75% savings, CPUCan also run in | low |

---

## conclusion

> **Is it an algorithm?, Is it data?, Is it a computational resource??**

All three are important, but current trends in 2025:

- **DeepSeek**this proof: smart algorithm > Calculate resources recklessly
- **LLaMA**prove: High-quality, small-volume data > Low-quality, bulk data
- **Claude**prove: Sorting method(Constitutional AI) = Determination of behavioral quality
- **Gemini**prove: Specific structural innovations(linear attention) → make the impossible possible

**Key advantages of our project:**  
No platform in the world is specialized for Korean language only..  
100Giga's Korean language materials are an unrivaled asset in this field..
