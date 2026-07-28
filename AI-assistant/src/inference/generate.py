"""Sampling-based article generation module.

【Beginner's Guide】
  This file is trained GPT It contains functions that generate text one character at a time using the model..

  Creation process summary:
    1. Enter the prompt string as a word mark. ID convert to list
    2. Calculate the probability of the next letter by putting it in the model (16,384dog)
    3. Repetition Penalty in Probability, Top-K, Top-P, Adjust by temperature
    4. Randomly select the next letter with final probability
    5. Add selected letters to input and return to step 2
    6. EOScomes out max_new_tokensEnd when reached

  Creation Quality Control Tool:
    temperature (temperature):    The lower the letter, the more certain it is., The higher it is, the more diverse the letters are.
    Top-K:                 top probability KOnly dog candidates allowed
    Top-P (nuclear sampling):   cumulative probability P Only the following candidates are accepted
    repetition penalty:             Prevent repetition by lowering the probability of letters that have already appeared
"""

from __future__ import annotations

from typing import Iterator

import torch
import torch.nn.functional as F

from src.tokenizer.tokenizer import BOS_ID, EOS_ID, Tokenizer


def _apply_repetition_penalty(logits: torch.Tensor, generated_ids: torch.Tensor, penalty: float) -> torch.Tensor:
    """Suppresses repetition by lowering the probability of letters that have already been created (CTRL way).

    【Beginner's Guide】
      Without this penalty, the model repeats the same words infinitely..
      yes: "kimchi kimchi kimchi kimchi kimchi..."

      CTRL way:
        - Among the letters already created logit(score)If this is a positive number → penaltyShare by (lowered)
        - logitIf this is negative → penaltyMultiply by (lower, i.e. the probability decreases)
        - penalty=1.0: No penalty points (default behavior)
        - penalty=1.15 (default): about 13% reduced probability

    argument:
        logits:        Current stage score vector (vocabulary size)
        generated_ids: Tags created so far ID list
        penalty:       Repeat Penalty Factor (1.0 = None, >1.0 = repetition suppression)

    return value:
        penalty points applied logits
    """
    if penalty == 1.0 or generated_ids.numel() == 0:
        return logits  # No penalties or no tickets created yet → return as is

    # of already created tags logit Extract only values
    score = logits.gather(-1, generated_ids)
    # Divide positive numbers, Negative numbers are multiplied so that the probability always goes down.
    score = torch.where(score < 0, score * penalty, score / penalty)
    # Put adjusted values back in their original positions
    logits.scatter_(-1, generated_ids, score)
    return logits


def _top_k_top_p(logits: torch.Tensor, top_k: int, top_p: float) -> torch.Tensor:
    """Top-K Wow Top-P (nuclear sampling) Apply a filter.

    【Beginner's Guide】
      Without this function, even random letters with a very low probability may be selected..

      Top-K filter:
        top KAll but one candidate is left -∞made with.
        yes: top_k=50 → 50Choose only from among candidates

      Top-P (nuclear sampling) filter:
        The cumulative sum of probabilities is pOnly candidates left until ..
        yes: top_p=0.95 → Total 95%Select only the top few that are
        Dynamic filter with different number of candidates depending on the situation

      Apply both filters in order (Top-K first).

    argument:
        logits: Current stage score vector
        top_k:  top Knumber of candidates (0No backside filter)
        top_p:  Cumulative Probability Threshold (1.0No backside filter)

    return value:
        filter applied logits (-∞Including candidates blocked by)
    """
    if top_k > 0:
        # Setting it larger than the vocabulary size is meaningless, so adjust it.
        top_k = min(top_k, logits.size(-1))
        # top top_kFind the second value and use it as the reference value
        kth = torch.topk(logits, top_k).values[..., -1, None]
        # smaller than the reference value logitis all -∞ by → Probability 0 after softmax
        logits = torch.where(logits < kth, torch.full_like(logits, float("-inf")), logits)

    if 0.0 < top_p < 1.0:
        # Sort from highest to highest
        sorted_logits, sorted_idx = torch.sort(logits, descending=True)
        # Calculate probability with softmax and then calculate cumulative sum
        probs = F.softmax(sorted_logits, dim=-1)
        cum = probs.cumsum(dim=-1)

        # cumulative probability top_pShow location exceeding
        remove = cum > top_p
        # move one space to the right: To include boundary values,
        remove[..., 1:] = remove[..., :-1].clone()
        remove[..., 0] = False  # The first is always included

        # location marked for removal -∞ by
        sorted_logits = sorted_logits.masked_fill(remove, float("-inf"))
        # Return to original position before sorting
        logits = torch.empty_like(logits).scatter_(-1, sorted_idx, sorted_logits)

    return logits


@torch.no_grad()
def sample_token(
    model,
    idx: torch.Tensor,
    *,
    temperature: float,
    top_k: int,
    top_p: float,
    repetition_penalty: float,
) -> torch.Tensor:
    """Sample one of the following tickets:.

    【Beginner's Guide】
      This function "Select the next letter" This is the core function that does.
      Run the model once, After applying filters, Choose based on probability.

      @torch.no_grad(): Do not calculate slope → Memory savings when inferring

    argument:
        model:             GPT model instance
        idx:               Tickets to date ID list, form (B, T)
        temperature:       temperature (The lower, the more obvious the choice.)
        top_k:             Top-K number of candidates
        top_p:             Top-P Cumulative Probability Threshold
        repetition_penalty: Repeat Penalty Factor

    return value:
        Next selected quote ID, form (B, 1)
    """
    # block_size(context length) OK: torch.compile Consider packaging
    block_size = model.cfg.block_size if hasattr(model, "cfg") else model._orig_mod.cfg.block_size

    # input block_sizeIf it exceeds the most recent block_size Use only
    idx_cond = idx if idx.size(1) <= block_size else idx[:, -block_size:]

    # Run the model → logits: (B, 1, vocab_size)
    logits, _ = model(idx_cond)
    logits = logits[:, -1, :]  # in last position logituse only: (B, vocab_size)

    # 1. Repeat penalty points applied (Reduces the probability of already created tags)
    logits = _apply_repetition_penalty(logits, idx, repetition_penalty)

    if temperature <= 0.0:
        # greedy choice: Always choose the letter with the highest probability (no diversity)
        return logits.argmax(dim=-1, keepdim=True)

    # 2. Divide temperature: high temperature → Probability distribution becomes flat (more diverse)
    #                low temperature → Probability distribution becomes sharper (more certain)
    logits = logits / temperature

    # 3. Top-K + Top-P filter: Eliminate the wrong candidate
    logits = _top_k_top_p(logits, top_k=top_k, top_p=top_p)

    # 4. Random selection after calculating probability with softmax
    probs = F.softmax(logits, dim=-1)
    next_id = torch.multinomial(probs, num_samples=1)  # Random selection proportional to probability
    return next_id


@torch.no_grad()
def generate_stream(
    model,
    tokenizer: Tokenizer,
    prompt: str,
    *,
    max_new_tokens: int = 256,
    temperature: float = 0.9,
    top_k: int = 50,
    top_p: float = 0.95,
    repetition_penalty: float = 1.15,
    device: torch.device | str = "cuda",
    stop_on_eos: bool = True,
) -> Iterator[str]:
    """Streaming generator that exports newly created letters piece by piece.

    【Beginner's Guide】
      This function Gradio UICreates the effect of letters appearing in real time.
      Rather than returning the completed text all at once, Whenever a new character is created, it is immediately exported..

      "delta(delta)" way:
        At each stage, only the newly added parts are exported. (Not cumulative).
        SentencePiece BPESince several word marks can be combined to form one letter,
        Reinterpret the whole thing, subtract the previously exported part and calculate only the new part..

    argument:
        model:             GPT model instance
        tokenizer:         Tag Analyzer Instance
        prompt:            Prompt string to start generating
        max_new_tokens:    Maximum number of tickets to generate
        temperature:       temperature
        top_k:             Top-K number of candidates
        top_p:             Top-P threshold
        repetition_penalty: repetition penalty
        device:            execution device ("cuda" or "cpu")
        stop_on_eos:       EOS Whether to stop when creating a tag

    generated value:
        Newly created article fragment (delta string)
    """
    model.eval()  # inference mode: Disable dropout

    # prompt IDconvert to (BOS add)
    ids = tokenizer.encode(prompt, add_bos=True)
    idx = torch.tensor([ids], dtype=torch.long, device=device)  # (1, T)

    produced: list[int] = []  # Newly created tag ID list (Except prompt)
    last_text = ""             # Last exported string (For delta calculation)

    for _ in range(max_new_tokens):
        # Select the next ticket
        nxt = sample_token(
            model, idx,
            temperature=temperature, top_k=top_k, top_p=top_p,
            repetition_penalty=repetition_penalty,
        )
        token_id = int(nxt.item())

        # EOS Creation ends when a mark appears.
        if stop_on_eos and token_id == EOS_ID:
            break

        produced.append(token_id)  # Generated tag record
        idx = torch.cat([idx, nxt], dim=1)  # Add new mark to input (Tincreases by 1)

        # Convert all generated word tags to character strings (BPE Consider merging)
        # SentencePiece BPESeveral word marks must be combined to form one letter.
        # Therefore, the entire thing must be reinterpreted at each step to produce accurate letters.
        text = tokenizer.decode(produced)
        if len(text) > len(last_text):
            yield text[len(last_text):]  # Newly added part(delta)export only
            last_text = text


@torch.no_grad()
def generate(
    model,
    tokenizer: Tokenizer,
    prompt: str,
    *,
    max_new_tokens: int = 256,
    temperature: float = 0.9,
    top_k: int = 50,
    top_p: float = 0.95,
    repetition_penalty: float = 1.15,
    device: torch.device | str = "cuda",
    stop_on_eos: bool = True,
) -> str:
    """Create a non-streaming post — Returns the entire completed string at once.

    【Beginner's Guide】
      generate_stream() It is a non-streaming version of.
      Prompt after all characters are generated + Returns the completed response as a single string..
      in training sample_text() Used to see examples of creation in functions..

    argument:
        (generate_streamSame as)

    return value:
        prompt + generated string (Completed version)
    """
    # generate_streamCollect all deltas from and concatenate them.
    chunks = list(
        generate_stream(
            model, tokenizer, prompt,
            max_new_tokens=max_new_tokens,
            temperature=temperature, top_k=top_k, top_p=top_p,
            repetition_penalty=repetition_penalty,
            device=device, stop_on_eos=stop_on_eos,
        )
    )
    return prompt + "".join(chunks)  # prompt + Return the generated contents together
