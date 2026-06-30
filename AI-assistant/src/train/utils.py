"""훈련 도우미 함수들 — 학습률 계획, 체크포인트 저장/불러오기.

【초보자 안내】
  이 파일은 train.py에서 반복적으로 쓰이는 도구 함수들을 모아놓은 것이다.

  주요 기능:
    cosine_lr()            : 학습률을 단계에 따라 조절
    save_checkpoint()      : 훈련 상태를 파일로 저장
    cleanup_old_checkpoints(): 오래된 체크포인트 파일 정리
    load_checkpoint()      : 저장된 체크포인트에서 훈련 재개
"""

from __future__ import annotations

import math
import re
from pathlib import Path

import torch


def cosine_lr(step: int, *, warmup: int, max_steps: int, peak_lr: float, min_lr: float) -> float:
    """코사인 감쇠 학습률 계획 (선형 워밍업 포함).

    【초보자 안내】
      학습률이란 "가중치를 한 번에 얼마나 조정할지" 의 크기다.
      너무 크면 발산(훈련 실패), 너무 작으면 느리거나 가짜 최솟값에 갇힘.

      이 함수는 세 구간으로 학습률을 조절한다:

      구간 1 (워밍업, 0 ~ warmup 단계):
        학습률을 0에서 peak_lr까지 선형으로 올림.
        초반 불안정한 훈련 방지.

        예: warmup=100, peak_lr=0.0003
          단계 0: 학습률 = 0.000003  (1/100)
          단계 50: 학습률 = 0.00015  (50/100)
          단계 100: 학습률 = 0.0003  (peak)

      구간 2 (코사인 감쇠, warmup ~ max_steps):
        코사인 함수를 따라 peak_lr에서 min_lr까지 부드럽게 감소.
        직선으로 줄이는 것보다 끝에서 더 천천히 줄어들어 안정적.

      구간 3 (max_steps 이후):
        min_lr로 고정.

    인수:
        step:      현재 훈련 단계 번호 (0부터 시작)
        warmup:    워밍업 단계 수
        max_steps: 총 훈련 단계 수
        peak_lr:   최고 학습률
        min_lr:    최저 학습률

    반환값:
        현재 단계의 학습률
    """
    if step < warmup:
        # 워밍업 구간: 0에서 peak_lr까지 선형 증가
        # +1 을 더하는 리유: step=0일 때 0이 아닌 최솟값부터 시작하도록
        return peak_lr * (step + 1) / max(1, warmup)

    if step >= max_steps:
        # 훈련 종료 후: min_lr 고정
        return min_lr

    # 코사인 감쇠 구간
    # progress: 워밍업 이후 진행률 (0.0 ~ 1.0)
    progress = (step - warmup) / max(1, max_steps - warmup)
    # 코사인 함수: 0에서 π까지 → 1.0에서 -1.0
    # 0.5 × (1 + cos(π × progress)): 1.0에서 0.0으로 부드럽게 감소
    coeff = 0.5 * (1.0 + math.cos(math.pi * progress))
    return min_lr + coeff * (peak_lr - min_lr)


def save_checkpoint(
    out_dir: Path,
    step: int,
    model: torch.nn.Module,
    optimizer: torch.optim.Optimizer,
    cfg_dict: dict,
    tag: str = "",
) -> Path:
    """훈련 상태를 체크포인트 파일로 저장.

    【초보자 안내】
      체크포인트란 훈련 중간의 상태를 저장해두는 것이다.
      전원이 꺼지거나 오류가 나도 여기서부터 다시 시작할 수 있다.

      저장 내용:
        - step: 현재 훈련 단계 번호
        - model_state: 모형의 모든 가중치
        - optimizer_state: AdamW의 이동 평균(모멘트) 값들
        - config: 모형 구조 설정 (재개 시 올바른 구조로 복원하기 위해)

      파일 이름 형식:
        tag 없음: ckpt_step002500.pt
        tag 있음: ckpt_step002500_best.pt

    인수:
        out_dir:   저장할 폴더
        step:      현재 단계 번호
        model:     GPT 모형 인스턴스
        optimizer: AdamW 최적화기 인스턴스
        cfg_dict:  설정 사전 (FullConfig.__dict__ 형태)
        tag:       파일 이름 뒤에 붙일 태그 ("best", "final" 등)

    반환값:
        저장된 파일의 경로
    """
    out_dir.mkdir(parents=True, exist_ok=True)

    # 파일 이름 생성: ckpt_step002500.pt 또는 ckpt_step002500_best.pt
    name = f"ckpt_step{step:06d}{('_' + tag) if tag else ''}.pt"
    path = out_dir / name

    # 체크포인트 저장 (torch.save: Python 객체를 직렬화하여 저장)
    torch.save(
        {
            "step": step,                              # 현재 단계
            "model_state": model.state_dict(),         # 모든 가중치 값
            "optimizer_state": optimizer.state_dict(), # AdamW 내부 상태
            "config": cfg_dict,                        # 모형 구조 설정
        },
        path,
    )
    return path


def cleanup_old_checkpoints(out_dir: Path, keep_last: int) -> None:
    """오래된 체크포인트 파일을 삭제하여 디스크 공간을 확보.

    【초보자 안내】
      훈련이 진행될수록 체크포인트 파일이 쌓인다.
      각 파일이 수백 MB 여서 모두 보관하면 디스크가 가득 찬다.

      삭제 규칙:
        - 태그 없는 파일: 가장 최근 keep_last 개만 남기고 삭제
          예: keep_last=3 → 최근 3개만 보관
        - 태그 있는 파일 (_best, _final): 항상 보관 (삭제 안 함)

    인수:
        out_dir:    체크포인트 폴더
        keep_last:  보관할 최근 파일 수
    """
    # 파일 이름 패턴: ckpt_step숫자[_태그].pt
    pattern = re.compile(r"^ckpt_step(\d+)(?:_([a-zA-Z0-9]+))?\.pt$")
    untagged: list[tuple[int, Path]] = []  # (단계 번호, 파일 경로) 목록

    for p in out_dir.glob("ckpt_step*.pt"):
        m = pattern.match(p.name)
        if not m:
            continue                        # 패턴에 맞지 않는 파일은 건너뜀
        if m.group(2):                      # 태그가 있는 파일 (_best 등)
            continue                        # 태그 있는 파일은 항상 보관
        untagged.append((int(m.group(1)), p))  # 단계 번호와 경로 저장

    # 단계 번호 기준으로 오름차순 정렬 (작은 번호 = 오래된 파일)
    untagged.sort(key=lambda x: x[0])

    # 최근 keep_last 개를 제외하고 나머지 삭제
    for _, p in untagged[:-keep_last] if keep_last > 0 else untagged:
        try:
            p.unlink()  # 파일 삭제
        except OSError:
            pass        # 삭제 실패해도 훈련은 계속


def load_checkpoint(path: str | Path, model: torch.nn.Module, optimizer=None, map_location="cpu"):
    """체크포인트 파일에서 모형과 최적화기 상태를 복원.

    【초보자 안내】
      이 함수를 쓰면 이전에 저장한 훈련 상태에서 계속 훈련할 수 있다.
      또는 훈련된 모형을 불러와 추론에 사용할 수 있다.

    인수:
        path:         체크포인트 파일 경로 (.pt 파일)
        model:        복원할 GPT 모형 인스턴스 (구조가 일치해야 함)
        optimizer:    복원할 최적화기 인스턴스 (None이면 복원 안 함)
        map_location: 가중치를 올릴 장치 ("cpu", "cuda" 등)

    반환값:
        체크포인트 사전 전체 (step, model_state, optimizer_state, config)
    """
    # weights_only=False: config 사전도 함께 불러오기 위해 필요
    ckpt = torch.load(str(path), map_location=map_location, weights_only=False)

    # 모형 가중치 복원
    model.load_state_dict(ckpt["model_state"])

    # 최적화기 상태 복원 (AdamW의 이동 평균 값들)
    # optimizer가 None이면 복원 안 함 (추론 전용으로 불러올 때)
    if optimizer is not None and "optimizer_state" in ckpt:
        optimizer.load_state_dict(ckpt["optimizer_state"])

    return ckpt  # 호출자가 step 등 다른 정보를 꺼내 쓸 수 있도록 전체 반환
