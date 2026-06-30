"""모형 설정값 관리 모듈 — YAML 파일을 읽어 설정 객체를 만든다.

【초보자 안내】
  이 파일은 프로그람 전체의 "설정판" 이다.
  `config/model_config.yaml` 파일에 있는 모든 수치를 읽어와
  Python 객체로 변환해준다.

  설정 구조:
    FullConfig
      ├── ModelConfig    — 신경망 구조 설정 (층 수, 차원 등)
      ├── TrainConfig    — 훈련 설정 (학습률, 반복 횟수 등)
      ├── TokenizerConfig — 토크나이저 설정
      ├── DataConfig     — 자료 경로 설정
      └── InferenceConfig — 글 생성 설정

  사용 방법:
    from src.model.config import load_config
    cfg = load_config("config/model_config.yaml")
    print(cfg.model.n_layer)  # → 8
"""

from __future__ import annotations

from dataclasses import dataclass, field
from pathlib import Path
from typing import Any

import yaml


@dataclass
class ModelConfig:
    """신경망 구조 설정.

    【초보자 안내】
      여기 있는 수치들이 트랜스포메르의 "몸집" 을 결정한다.
      숫자가 클수록 모형이 커지고 더 많이 배울 수 있지만,
      더 많은 현현기억기(VRAM)와 훈련 시간이 필요하다.
    """

    # 트랜스포메르 블로크(층)의 수
    # 기본값 8: 글자들 사이의 관계를 8번 정제함
    # 더 많을수록 복잡한 패턴을 배울 수 있지만 현현기억기 소비 증가
    n_layer: int = 8

    # 주의 기제의 머리(Head) 수
    # 기본값 8: 8가지 다른 관점으로 동시에 관계를 분석
    # n_embd 가 n_head 로 나뉘어야 함 (512 ÷ 8 = 64)
    n_head: int = 8

    # 임베딩 차원 수 (모형의 "폭")
    # 기본값 512: 각 글자를 512개 숫자의 벡토르로 표현
    # 클수록 표현력이 높아지지만 현현기억기 소비 증가
    n_embd: int = 512

    # 전달 망의 내부 차원 수
    # 기본값 2048: n_embd × 4 = 512 × 4
    # 전달 망은 이 넓은 공간에서 지식을 처리하고 다시 512로 압축함
    ffn_dim: int = 2048

    # 최대 문맥 길이 (한 번에 처리할 수 있는 최대 글자 수)
    # 기본값 1024: 1024개 글자까지의 맥락을 한꺼번에 볼 수 있음
    # GPU 현현기억기가 허락하는 한도 내에서 클수록 좋음
    block_size: int = 1024

    # 어휘 크기 (토크나이저가 만드는 단어 번호의 최대값 + 1)
    # 기본값 16384: 조선말 단어와 형태소를 16,384개 단위로 나눔
    # 훈련 시 meta.json 에서 실제 어휘 크기로 덮어써짐
    vocab_size: int = 16384

    # 드롭아웃 비율 (훈련 시 무작위로 끄는 뉴런의 비율)
    # 기본값 0.1: 10%의 연결을 무작위로 끊어 과적합 방지
    # 추론 시에는 자동으로 0이 됨
    dropout: float = 0.1

    # 편향(bias) 사용 여부
    # 기본값 False: 편향 없이도 잘 작동하며 매개변수 수를 줄일 수 있음
    bias: bool = False

    # 가중치 공유 여부 (입력 임베딩 ↔ 출력 사영)
    # 기본값 True: 같은 행렬을 공유하여 매개변수 수를 약 800만 개 줄임
    tie_weights: bool = True

    def num_params_estimate(self) -> int:
        """총 매개변수 수 추정값을 반환.

        가중치 공유(tie_weights)를 고려하여 계산.
        실제 값과 약간 차이날 수 있음 (LayerNorm 등 소규모 제외).
        """
        # 토큰 임베딩: vocab_size × n_embd
        emb = self.vocab_size * self.n_embd
        # 위치 임베딩: block_size × n_embd
        pos = self.block_size * self.n_embd
        # 블로크 1개당 매개변수:
        #   주의 기제: QKV(3×n_embd×n_embd) + 출력사영(n_embd×n_embd) = 4×n_embd²
        #   전달 망: 확장(n_embd×ffn_dim) + 축소(ffn_dim×n_embd) = 2×n_embd×ffn_dim
        per_block = (
            4 * self.n_embd * self.n_embd      # 주의 기제 (QKV + 출력)
            + 2 * self.n_embd * self.ffn_dim   # 전달 망 (확장 + 축소)
        )
        blocks = per_block * self.n_layer  # n_layer개 블로크
        # LM 머리: 가중치 공유 시 0, 미공유 시 vocab_size × n_embd
        head = 0 if self.tie_weights else self.vocab_size * self.n_embd
        return emb + pos + blocks + head


@dataclass
class TrainConfig:
    """훈련 과정 설정.

    【초보자 안내】
      이 설정들이 "얼마나 오래, 어떻게 훈련할지" 를 결정한다.
    """

    # 체크포인트와 로그를 저장할 폴더
    out_dir: str = "checkpoints"

    # 한 번에 처리하는 문서 수 (묶음 크기)
    # 기본값 16: 16개 문서를 동시에 처리 → 기울기 평균이 더 안정적
    # 현현기억기가 허락하는 한 크게 설정할수록 훈련 안정성이 높아짐
    batch_size: int = 16

    # 기울기 누적 단계 수
    # 실질 묶음 크기 = batch_size × grad_accum_steps = 16 × 4 = 64
    # 현현기억기가 부족할 때 실질 묶음 크기를 늘리는 방법
    grad_accum_steps: int = 4

    # 최고 학습률 (피크 학습률)
    # AdamW 최적화기가 가중치를 한 번에 얼마나 조정할지
    # 너무 크면 발산, 너무 작으면 학습이 느림
    learning_rate: float = 3.0e-4

    # 최저 학습률 (코사인 감쇠의 최솟값)
    # max_steps 끝에 가까워질수록 학습률이 이 값까지 줄어듦
    min_lr: float = 3.0e-5

    # 워밍업 단계 수
    # 처음 100단계는 학습률을 0에서 peak까지 선형으로 올림
    # 초반 불안정한 훈련 방지
    warmup_steps: int = 100

    # 총 훈련 단계 수
    # 기본값 5000: 5,000번 기울기를 계산하고 가중치를 조정
    max_steps: int = 5000

    # 가중치 감쇠 계수 (과적합 방지)
    # 가중치가 너무 커지는 것을 막는 정규화 항
    weight_decay: float = 0.1

    # AdamW 베타 1: 기울기의 이동 평균 계수 (단기 기억)
    beta1: float = 0.9

    # AdamW 베타 2: 기울기 제곱의 이동 평균 계수 (장기 기억)
    beta2: float = 0.95

    # 기울기 자르기 임계값
    # 기울기의 노름이 이 값을 넘으면 자름 → 폭발적 기울기 방지
    grad_clip: float = 1.0

    # 검증 및 체크포인트 저장 간격 (단계 수)
    # 250단계마다 검증 손실을 계산하고 체크포인트를 저장
    eval_interval: int = 250

    # 검증 시 사용하는 묶음 수
    # 50개 묶음의 평균 손실로 검증 손실 추정
    eval_iters: int = 50

    # 훈련 로그 출력 간격 (단계 수)
    # 10단계마다 손실과 학습률을 출력
    log_interval: int = 10

    # 글 생성 예시 출력 간격 (단계 수)
    # 500단계마다 현재 모형으로 짧은 글을 생성하여 진행 상황을 확인
    sample_interval: int = 500

    # 보관할 최근 체크포인트 수 (태그 없는 것만 해당)
    # _best, _final 태그가 붙은 것은 항상 보관
    ckpt_keep_last: int = 3

    # 훈련 시 사용할 부동소수점 형식
    # "bfloat16": RTX 30xx 이상에서 빠르고 안정적
    # "fp16": 구형 GPU 대체
    # "float32": 가장 느리지만 가장 안전
    dtype: str = "bfloat16"

    # torch.compile 사용 여부
    # True로 하면 약 15% 빨라지지만, Windows에서 지원이 제한적
    # 기본값 False: Windows 11 환경에서 안전하게 비활성화
    compile: bool = False


@dataclass
class TokenizerConfig:
    """토크나이저 훈련 설정.

    【초보자 안내】
      토크나이저는 조선말 글자를 정수 번호로 바꾸는 도구다.
      SentencePiece BPE 방식을 쓴다.
      BPE: 자주 나오는 글자 조합을 하나의 단위로 묶는 방식.
    """

    # 어휘 크기: 몇 개의 단위로 조선말을 나눌지
    # 자료가 적으면 자동으로 줄어듦 (train_tokenizer.py 참고)
    vocab_size: int = 16384

    # 토크나이저 모형 종류
    # "bpe": Byte Pair Encoding — 현재 프로그람에서 쓰는 방식
    model_type: str = "bpe"

    # 글자 포함 비율
    # 0.9995: 전체 말뭉치 글자의 99.95% 가 어휘에 포함되도록 설정
    character_coverage: float = 0.9995

    # 저장 경로 접두사
    # 훈련 후 dprk_sp.model 과 dprk_sp.vocab 파일이 생성됨
    output_prefix: str = "tokenizer/dprk_sp"


@dataclass
class DataConfig:
    """자료 경로 설정.

    【초보자 안내】
      원본 자료와 전처리된 자료의 경로를 설정한다.
    """

    # 원본 자료 폴더 (텍스트, JSON, PDF 등)
    raw_dir: str = "data/raw"

    # 전처리된 자료 폴더 (train.bin, val.bin, meta.json)
    processed_dir: str = "data/processed"

    # 훈련/검증 분할 비율
    # 0.95: 전체 자료의 95%를 훈련에, 5%를 검증에 사용
    train_split: float = 0.95


@dataclass
class InferenceConfig:
    """글 생성(추론) 기본 설정.

    【초보자 안내】
      Gradio UI에서 슬라이더로 조절하는 값들의 기본값이다.
      generate.py와 chat.py에서 이 값들을 사용한다.
    """

    # 한 번 생성할 최대 글자(어표) 수
    default_max_new_tokens: int = 256

    # 온도: 생성 다양성 조절 (높을수록 창의적, 낮을수록 반복적)
    default_temperature: float = 0.9

    # Top-K: 상위 K개 후보만 남기고 나머지 제거
    default_top_k: int = 50

    # Top-P (핵 표본추출): 누적 확률 P까지의 후보만 남김
    default_top_p: float = 0.95

    # 반복 벌점: 이미 생성한 글자의 확률을 낮춤 (1.0 = 벌점 없음)
    default_repetition_penalty: float = 1.15


@dataclass
class FullConfig:
    """모든 설정을 담는 최상위 설정 컨테이너.

    【초보자 안내】
      load_config() 함수가 이 객체를 반환한다.
      cfg = load_config("config/model_config.yaml") 로 불러온 뒤
      cfg.model.n_layer, cfg.train.batch_size 등으로 접근한다.
    """
    model: ModelConfig = field(default_factory=ModelConfig)
    train: TrainConfig = field(default_factory=TrainConfig)
    tokenizer: TokenizerConfig = field(default_factory=TokenizerConfig)
    data: DataConfig = field(default_factory=DataConfig)
    inference: InferenceConfig = field(default_factory=InferenceConfig)


def _build(cls, raw: dict[str, Any] | None):
    """사전(dict)에서 설정 클라스 인스턴스를 만드는 도우미 함수.

    YAML에 없는 항목은 클라스의 기본값을 사용한다.
    YAML에 있더라도 클라스에 없는 항목은 무시한다 (오류 방지).
    """
    if raw is None:
        return cls()  # YAML에 해당 섹션이 없으면 기본값으로 생성
    # 클라스에 정의된 필드 이름만 골라서 전달
    return cls(**{k: v for k, v in raw.items() if k in cls.__dataclass_fields__})


def load_config(path: str | Path) -> FullConfig:
    """YAML 파일을 읽어 FullConfig 객체를 만들어 반환.

    사용 예:
        cfg = load_config("config/model_config.yaml")
        print(cfg.model.n_layer)   # → 8
        print(cfg.train.max_steps) # → 5000

    인수:
        path: YAML 설정 파일의 경로

    반환값:
        FullConfig: 모든 설정이 담긴 객체
    """
    with open(path, "r", encoding="utf-8") as f:
        raw: dict[str, Any] = yaml.safe_load(f) or {}  # YAML 파일을 사전으로 읽음

    # 각 섹션별로 설정 객체를 만들어 FullConfig에 담아 반환
    return FullConfig(
        model=_build(ModelConfig, raw.get("model")),           # [model] 섹션
        train=_build(TrainConfig, raw.get("train")),           # [train] 섹션
        tokenizer=_build(TokenizerConfig, raw.get("tokenizer")),  # [tokenizer] 섹션
        data=_build(DataConfig, raw.get("data")),              # [data] 섹션
        inference=_build(InferenceConfig, raw.get("inference")),  # [inference] 섹션
    )


def save_config(cfg: FullConfig, path: str | Path) -> None:
    """FullConfig 객체를 YAML 파일로 저장.

    인수:
        cfg:  저장할 설정 객체
        path: 저장할 YAML 파일 경로
    """
    # 각 설정 객체를 사전으로 변환
    raw = {
        "model": cfg.model.__dict__,
        "train": cfg.train.__dict__,
        "tokenizer": cfg.tokenizer.__dict__,
        "data": cfg.data.__dict__,
        "inference": cfg.inference.__dict__,
    }
    with open(path, "w", encoding="utf-8") as f:
        # allow_unicode=True: 조선말 글자를 이스케이프 없이 저장
        yaml.safe_dump(raw, f, sort_keys=False, allow_unicode=True)
