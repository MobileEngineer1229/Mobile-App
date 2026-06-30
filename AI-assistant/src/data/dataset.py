"""메모리맵 기반 어표 자료집합 — 훈련용.

【초보자 안내】
  이 파일은 훈련 자료를 빠르게 읽어 모형에 공급하는 역할을 한다.

  메모리맵(memmap)이란:
    파일 전체를 기억기에 올리지 않고, 필요한 부분만 그때그때 읽는 방식.
    train.bin 파일이 수 GB 여도 기억기 걱정 없이 접근할 수 있다.

  (x, y) 쌍이란:
    x = 현재 글자들 [t, t+1, ..., t+block_size-1]
    y = 다음 글자들 [t+1, t+2, ..., t+block_size]
    모형은 x를 보고 y를 예측하도록 훈련된다.

    례:
      전체 자료: [BOS, 342, 156, 891, 782, 201, EOS, ...]
      t=1 에서 block_size=4 로 표본추출:
        x = [342, 156, 891, 782]  ← "김치 담그는 방법"
        y = [156, 891, 782, 201]  ← "담그는 방법 무엇"  (한 칸 이동)
      모형 목표: x[0]→y[0], x[1]→y[1], ... 즉 다음 글자를 예측

  nanoGPT 패턴을 사용하는 리유:
    - 매 단계마다 무작위 위치를 선택 → 전통적 에포크 개념 없음
    - DataLoader 없이 직접 numpy 배열에서 표본추출 → 더 빠름
    - 여러 작업자(worker) 프로세스가 각자 memmap 사본을 갖지 않아도 됨
"""

from __future__ import annotations

from pathlib import Path
from typing import Iterator

import numpy as np
import torch


class TokenDataset:
    """메모리맵 파일에서 어표 묶음을 표본추출하는 자료집합 클라스.

    【초보자 안내】
      이 클라스를 만들 때 train.bin 또는 val.bin 파일을 연결한다.
      sample() 메소드를 호출할 때마다 무작위 위치에서 block_size 길이의
      글자 련속을 뽑아 (x, y) 쌍으로 반환한다.
    """

    def __init__(self, bin_path: str | Path, block_size: int):
        """자료집합 초기화.

        인수:
            bin_path:   어표 이진 파일 경로 (train.bin 또는 val.bin)
            block_size: 한 번에 처리할 최대 글자 수 (모형의 문맥 길이)
        """
        self.bin_path = Path(bin_path)
        if not self.bin_path.exists():
            raise FileNotFoundError(
                f"어표 파일을 찾을 수 없습니다: {self.bin_path}\n"
                f"scripts/02_preprocess_data.ps1 을 먼저 실행하십시오."
            )
        self.block_size = block_size

        # uint16 형식으로 메모리맵 열기 (preprocess.py와 형식 일치)
        # uint16: 0~65535 범위의 정수 → 어휘 크기 16,384개에 충분
        # mode="r": 읽기 전용 (훈련 중 파일을 수정하지 않음)
        self.data = np.memmap(self.bin_path, dtype=np.uint16, mode="r")

        # 자료가 너무 적으면 (x, y) 쌍을 하나도 만들 수 없음
        # 최소 block_size + 2 개의 어표가 필요
        if len(self.data) <= block_size + 1:
            raise ValueError(
                f"자료집합에 어표가 {len(self.data)}개밖에 없습니다. "
                f"block_size={block_size}일 때 최소 {block_size + 2}개 필요합니다.\n"
                f"훈련 자료를 더 추가하십시오."
            )

    def __len__(self) -> int:
        """유효한 표본 시작 위치의 수를 반환.

        전체 어표 수에서 block_size+1 을 뺀 것이 표본 시작 위치의 수다.
        (마지막 위치에서 block_size 만큼 읽으면 파일 끝을 넘어가므로)
        """
        return max(0, len(self.data) - self.block_size - 1)

    def sample(self, batch_size: int, device: torch.device | str = "cpu") -> tuple[torch.Tensor, torch.Tensor]:
        """무작위 위치에서 batch_size 개의 련속을 표본추출.

        인수:
            batch_size: 한 묶음에 포함할 련속의 수
            device:     텐서를 올릴 장치 ("cpu" 또는 "cuda")

        반환값:
            (x, y) 쌍:
                x: 입력 어표, 형태 (batch_size, block_size)
                y: 목표 어표, 형태 (batch_size, block_size)
                   y[i][j] = x[i][j+1] (다음 글자)
        """
        # 무작위 시작 위치 batch_size 개 선택
        # 각 위치에서 block_size+1 개를 읽을 수 있어야 하므로
        # 유효 범위: [0, len-block_size-1)
        ix = np.random.randint(0, len(self.data) - self.block_size - 1, size=batch_size)

        # x: 각 위치에서 block_size 개 읽기 (현재 글자들)
        # y: 한 칸 이동한 위치에서 block_size 개 읽기 (다음 글자들)
        # astype(np.int64): PyTorch가 int64(Long) 텐서를 요구함
        x = np.stack([self.data[i : i + self.block_size].astype(np.int64) for i in ix])
        y = np.stack([self.data[i + 1 : i + 1 + self.block_size].astype(np.int64) for i in ix])

        # numpy 배열을 PyTorch 텐서로 변환
        x_t = torch.from_numpy(x)
        y_t = torch.from_numpy(y)

        # GPU로 전송 (CUDA 장치인 경우)
        if str(device).startswith("cuda"):
            # pin_memory(): CPU 고정 기억기에 올려 GPU 전송 속도 향상
            # non_blocking=True: GPU 전송과 다른 작업을 동시에 진행
            x_t = x_t.pin_memory().to(device, non_blocking=True)
            y_t = y_t.pin_memory().to(device, non_blocking=True)
        else:
            x_t = x_t.to(device)
            y_t = y_t.to(device)

        return x_t, y_t


def get_batch_iterator(
    dataset: TokenDataset, batch_size: int, device: torch.device | str
) -> Iterator[tuple[torch.Tensor, torch.Tensor]]:
    """무한 반복 묶음 생성기.

    【초보자 안내】
      훈련 반복문에서 `next(iterator)` 를 호출할 때마다
      새로운 무작위 묶음을 제공한다.
      `while True` 로 무한 반복하므로 자료가 부족해서 멈추는 일이 없다.

    인수:
        dataset:    TokenDataset 인스턴스
        batch_size: 묶음 크기
        device:     텐서를 올릴 장치

    생성값:
        (x, y) 묶음 — 무한히 계속
    """
    while True:
        yield dataset.sample(batch_size, device=device)  # 묶음을 뽑아 반환하고 다시 반복
