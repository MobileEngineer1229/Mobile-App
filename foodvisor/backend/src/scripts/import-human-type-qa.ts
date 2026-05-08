import "dotenv/config";
import mongoose from "mongoose";
import { connectDatabase } from "../db.js";
import { HumanTypeQA } from "../models/human-type-qa.js";

type TypeScore = { TE?: number; SE?: number; SY?: number; TY?: number };

type Question = {
  questionId: string;
  order: number;
  categoryKey: string;
  categoryNameKo: string;
  categoryNameEn: string;
  promptKo: string;
  inputType: "single" | "scale" | "multi" | "text";
  scaleMin?: number;
  scaleMax?: number;
  scaleMinLabelKo?: string;
  scaleMaxLabelKo?: string;
  options: Array<{ value: string; labelKo: string; score: TypeScore; recommendationTags?: string[]; cautionTags?: string[] }>;
  answers?: Array<{ value: string; labelKo: string; score: TypeScore; recommendationTags?: string[]; cautionTags?: string[] }>;
  answerListKo?: string;
  weight?: number;
  isCore?: boolean;
  useForClassification?: boolean;
  useForRecommendation?: boolean;
  scoringNoteKo?: string;
  analysisNoteKo?: string;
  dataSource?: string;
  sourceNote?: string;
  sourceRefs?: string[];
  doctor_verified: boolean;
};

const dataSource = "Foodvisor curated Sasang questionnaire v1";
const sourceNote =
  "Curated constitution tendency questionnaire for app classification. This is not a medical diagnosis and must be clinically reviewed before treatment use.";

const refs = [
  "Sasang integrated diagnosis model: https://link.springer.com/article/10.1186/1472-6882-12-85",
  "KS-15 development and validation: https://koreascience.or.kr/article/JAKO201521839155753.page",
  "Sasang constitution and metabolic risk overview: https://pmc.ncbi.nlm.nih.gov/articles/PMC5581468/"
];

const categories = {
  body: ["body_shape", "체형·기상", "Body Shape & Temperament"],
  appearance: ["appearance_speech", "용모·사기", "Appearance & Speech"],
  personality: ["personality_talent", "성질·재간", "Personality & Talent"],
  symptoms: ["symptoms_habits", "병증·소증·식습관", "Disease, Fundamental Symptoms & Food Habits"]
} as const;

function option(value: string, labelKo: string, score: TypeScore, recommendationTags: string[] = [], cautionTags: string[] = []) {
  return { value, labelKo, score, recommendationTags, cautionTags };
}

function q(
  category: keyof typeof categories,
  questionId: string,
  order: number,
  promptKo: string,
  options: Question["options"],
  extra: Partial<Question> = {}
): Question {
  const [categoryKey, categoryNameKo, categoryNameEn] = categories[category];
  const answerListKo = options.map((item) => item.labelKo).join(", ");

  return {
    questionId,
    order,
    categoryKey,
    categoryNameKo,
    categoryNameEn,
    promptKo,
    inputType: "single",
    options,
    answers: options,
    answerListKo,
    weight: 1,
    isCore: false,
    useForClassification: true,
    useForRecommendation: false,
    dataSource,
    sourceNote,
    sourceRefs: refs,
    doctor_verified: false,
    ...extra
  };
}

function scaleOptions(labels: [string, string, string, string, string], scores: [TypeScore, TypeScore, TypeScore, TypeScore, TypeScore]) {
  return labels.map((label, index) => option(String(index + 1), label, scores[index]));
}

const questions: Question[] = [
  q("body", "body_shape_type", 1, "귀하의 체형에 가장 가까운 것은?", [
    option("lower_body", "하체 비만형", { TE: 2 }),
    option("upper_body", "상체 비만형", { SY: 2, TY: 1 }),
    option("abdominal", "복부 비만형", { TE: 3 }),
    option("balanced", "균등형", {})
  ], { analysisNoteKo: "체형 문항은 자가 인식 편향이 있으므로 체격·체중증가·땀·소화 문항과 함께 판단합니다." }),

  q("body", "body_frame_size", 2, "타인과 비교했을 때 전체적인 체격은 어느 쪽에 가깝습니까?", scaleOptions(
    ["매우 작은 편", "작은 편", "보통", "큰 편", "매우 큰 편"],
    [{ SE: 3 }, { SE: 2 }, {}, { TE: 2 }, { TE: 3 }]
  )),

  q("body", "natural_muscle_mass", 3, "타고난 근육량은 어느 정도라고 생각하십니까?", scaleOptions(
    ["매우 적은 편", "적은 편", "보통", "많은 편", "매우 많은 편"],
    [{ SE: 2 }, { SE: 1 }, {}, { SY: 1, TE: 1 }, { SY: 2, TE: 1 }]
  )),

  q("body", "bone_thickness", 4, "손목·발목·관절 등 골격의 굵기는 어느 쪽에 가깝습니까?", scaleOptions(
    ["매우 가는 편", "가는 편", "보통", "굵은 편", "매우 굵은 편"],
    [{ SE: 3 }, { SE: 2 }, {}, { TE: 2 }, { TE: 3 }]
  )),

  q("body", "weight_gain_after_overeating", 5, "과식 후 체중 증가가 잘 되는 편입니까?", scaleOptions(
    ["전혀 아니다", "아니다", "보통", "그렇다", "매우 그렇다"],
    [{ SE: 1, SY: 1 }, { SY: 1 }, {}, { TE: 2 }, { TE: 3 }]
  ), { useForRecommendation: true }),

  q("body", "upper_lower_balance", 6, "가슴·어깨 쪽과 골반·하체 쪽 중 어느 부위가 더 발달했다고 느끼십니까?", scaleOptions(
    ["하체가 훨씬 발달", "하체가 약간 발달", "비슷함", "상체가 약간 발달", "상체가 훨씬 발달"],
    [{ TE: 2 }, { TE: 1 }, {}, { SY: 1 }, { SY: 2, TY: 1 }]
  )),

  q("body", "shoulder_width", 7, "어깨 넓이는 전체 체격에 비해 어떤 편입니까?", scaleOptions(
    ["매우 좁음", "좁음", "보통", "넓음", "매우 넓음"],
    [{ SE: 2 }, { SE: 1 }, {}, { SY: 1, TY: 1 }, { SY: 2, TY: 1 }]
  )),

  q("body", "appetite_strength", 8, "평소 식욕은 어느 정도입니까?", scaleOptions(
    ["매우 약함", "약함", "보통", "좋은 편", "매우 왕성함"],
    [{ SE: 2 }, { SE: 1 }, {}, { TE: 1, SY: 1 }, { TE: 2 }]
  ), { useForRecommendation: true }),

  q("appearance", "face_shape", 9, "귀하의 얼굴형에 가장 가까운 것은?", [
    option("round", "둥근형", { TE: 2 }),
    option("angular", "각진형", { SY: 1, TE: 1 }),
    option("oval", "달걀형", { SE: 1 }),
    option("triangular", "삼각형", { TY: 2, SY: 1 })
  ], { analysisNoteKo: "얼굴형은 사진 분석이 없을 때 보조 문항으로만 사용합니다." }),

  q("appearance", "facial_skin_oiliness", 10, "얼굴 피부는 어떤 성질에 가깝습니까?", scaleOptions(
    ["매우 건성", "건성", "보통", "지성", "매우 지성"],
    [{ SE: 2 }, { SE: 1 }, {}, { TE: 1, SY: 1 }, { TE: 2 }]
  )),

  q("appearance", "voice_volume", 11, "타인이 평가하는 귀하의 목소리 크기는?", scaleOptions(
    ["매우 작은 편", "작은 편", "보통", "큰 편", "매우 큰 편"],
    [{ SE: 2 }, { SE: 1 }, {}, { SY: 1, TY: 1 }, { TY: 2, SY: 1 }]
  )),

  q("appearance", "speech_speed", 12, "말하는 속도는 어느 쪽에 가깝습니까?", scaleOptions(
    ["매우 느린 편", "느린 편", "보통", "빠른 편", "매우 빠른 편"],
    [{ TE: 1 }, { TE: 1, SE: 1 }, {}, { SY: 2 }, { SY: 2, TY: 1 }]
  )),

  q("appearance", "voice_pitch", 13, "목소리 높낮이는 어느 쪽에 가깝습니까?", scaleOptions(
    ["매우 낮음", "낮음", "보통", "높음", "매우 높음"],
    [{ TE: 1 }, { TE: 1 }, { SE: 1 }, { SY: 1 }, { SY: 2 }]
  ), { analysisNoteKo: "음성은 실제 음성 분석 없이 자가 응답으로만 쓰므로 낮은 가중치로 둡니다.", weight: 0.7 }),

  q("appearance", "facial_expression", 14, "주변에서 귀하의 인상에 대해 자주 듣는 평가는?", [
    option("gentle", "온화하다", { TE: 1, SE: 1 }),
    option("sharp", "날카롭다", { SY: 1, TY: 2 }),
    option("flat", "무표정하다", { SE: 1, TE: 1 }),
    option("bright", "밝고 활기차다", { SY: 2 })
  ]),

  q("appearance", "facial_flush", 15, "얼굴이 쉽게 붉어지는 편입니까?", scaleOptions(
    ["전혀 아니다", "아니다", "보통", "그렇다", "매우 그렇다"],
    [{ TE: 1 }, { TE: 1 }, { SE: 1 }, { SY: 1 }, { SY: 2 }]
  )),

  q("personality", "detail_orientation", 16, "성격이 대범한 편입니까, 섬세한 편입니까?", scaleOptions(
    ["매우 대범함", "대범한 편", "보통", "섬세한 편", "매우 섬세함"],
    [{ TE: 2, SY: 1 }, { TE: 1 }, {}, { SE: 2 }, { SE: 3 }]
  ), { isCore: true, weight: 2, scoringNoteKo: "KS-15 성격 축에 해당하므로 핵심 가중치를 적용합니다." }),

  q("personality", "activity_speed", 17, "행동이 빠른 편입니까, 느린 편입니까?", scaleOptions(
    ["매우 느린 편", "느린 편", "보통", "빠른 편", "매우 빠른 편"],
    [{ TE: 1, SE: 1 }, { SE: 1 }, {}, { SY: 2, TY: 1 }, { SY: 3, TY: 1 }]
  ), { isCore: true, weight: 2, scoringNoteKo: "KS-15 활동성 축에 해당하므로 핵심 가중치를 적용합니다." }),

  q("personality", "initiative", 18, "모든 일에 적극적인 편입니까, 소극적인 편입니까?", scaleOptions(
    ["매우 소극적", "소극적", "보통", "적극적", "매우 적극적"],
    [{ SE: 3 }, { SE: 2 }, { TE: 1 }, { SY: 2, TY: 1 }, { SY: 2, TY: 2 }]
  ), { isCore: true, weight: 2, scoringNoteKo: "KS-15 주도성 축에 해당하므로 핵심 가중치를 적용합니다." }),

  q("personality", "extraversion", 19, "성격이 외향적인 편입니까, 내성적인 편입니까?", scaleOptions(
    ["매우 내성적", "내성적", "보통", "외향적", "매우 외향적"],
    [{ SE: 3 }, { SE: 2 }, { TE: 1 }, { SY: 1, TY: 1 }, { SY: 3 }]
  ), { isCore: true, weight: 2, scoringNoteKo: "KS-15 외향성 축에 해당하므로 핵심 가중치를 적용합니다." }),

  q("personality", "emotional_reaction", 20, "일이 뜻대로 되지 않을 때 주된 반응은 어느 쪽입니까?", [
    option("very_hurt", "매우 상함", { SE: 2 }),
    option("slightly_hurt", "조금 상함", { SE: 1, TE: 1 }),
    option("neutral", "보통", {}),
    option("slightly_angry", "조금 화남", { SY: 1 }),
    option("very_angry", "매우 화남", { SY: 2, TY: 1 })
  ]),

  q("personality", "patience", 21, "인내심은 어느 정도라고 느끼십니까?", scaleOptions(
    ["매우 부족", "부족", "보통", "강한 편", "매우 강함"],
    [{ SY: 2 }, { SY: 1 }, { SE: 1 }, { TE: 1 }, { TE: 2 }]
  )),

  q("personality", "novelty_seeking", 22, "새로운 경험을 추구하는 정도는?", scaleOptions(
    ["매우 싫어함", "싫어함", "보통", "좋아함", "매우 좋아함"],
    [{ TE: 1 }, { SE: 1 }, {}, { TY: 1 }, { SY: 2 }]
  )),

  q("personality", "planning_style", 23, "계획성과 즉흥성 중 어느 쪽에 가깝습니까?", scaleOptions(
    ["매우 즉흥적", "즉흥적", "보통", "계획적", "매우 계획적"],
    [{ SY: 2 }, { SY: 1 }, {}, { SE: 1 }, { TE: 2, SE: 1 }]
  )),

  q("personality", "competition_drive", 24, "경쟁심은 어느 정도입니까?", scaleOptions(
    ["전혀 없음", "약함", "보통", "강함", "매우 강함"],
    [{ SE: 1 }, { TE: 1 }, {}, { SY: 1, TY: 1 }, { SY: 2, TY: 2 }]
  )),

  q("personality", "preferred_activity", 25, "여가 시간에 주로 어떤 활동을 즐기십니까?", [
    option("social", "사교/모임", { SY: 2, TE: 1 }, ["social_eating"]),
    option("reading_art", "독서/예술 감상", { SE: 1, TY: 1 }, ["quiet_routine"]),
    option("physical", "운동/신체 활동", { SY: 2 }, ["active_lifestyle"]),
    option("online_game", "게임/온라인 활동", { SE: 1 }, ["sedentary_risk"])
  ], { useForRecommendation: true }),

  q("symptoms", "digestion", 26, "평소 소화 상태는 어떻습니까?", scaleOptions(
    ["매우 나쁨", "나쁨", "보통", "좋음", "매우 좋음"],
    [{ SE: 3 }, { SE: 2 }, { TE: 1 }, { TE: 1, SY: 1 }, { SY: 2 }]
  ), { isCore: true, weight: 2, useForRecommendation: true, scoringNoteKo: "KS-15 소증 축에 해당하므로 핵심 가중치를 적용합니다." }),

  q("symptoms", "hand_foot_temperature", 27, "손발이 차가운 편입니까, 따뜻한 편입니까?", scaleOptions(
    ["매우 차가움", "차가움", "보통", "따뜻함", "매우 따뜻함"],
    [{ SE: 3 }, { SE: 2 }, { TE: 1 }, { SY: 1, TE: 1 }, { SY: 2, TY: 1 }]
  ), { useForRecommendation: true }),

  q("symptoms", "sweating", 28, "평소 땀을 많이 흘리는 체질입니까?", scaleOptions(
    ["전혀 아니다", "아니다", "보통", "그렇다", "매우 그렇다"],
    [{ SE: 1 }, { SE: 1 }, {}, { TE: 2 }, { TE: 3 }]
  ), { useForRecommendation: true }),

  q("symptoms", "stool_type", 29, "평소 대변 상태는 어느 쪽에 가깝습니까?", [
    option("constipation", "변비형", { SY: 1, TE: 1 }, ["fiber_priority"]),
    option("normal", "보통", {}),
    option("diarrhea", "설사형", { SE: 2 }, ["digestive_gentle"])
  ], { useForRecommendation: true }),

  q("symptoms", "sleep_latency", 30, "잠들기까지 시간이 오래 걸리는 편입니까?", scaleOptions(
    ["전혀 아니다", "아니다", "보통", "그렇다", "매우 그렇다"],
    [{ TE: 1 }, {}, {}, { SY: 1 }, { SY: 1, TY: 1 }]
  )),

  q("symptoms", "sleep_refresh", 31, "잠에서 깨어난 후 개운함을 느끼십니까?", scaleOptions(
    ["전혀 아니다", "아니다", "보통", "그렇다", "매우 그렇다"],
    [{ TE: 1, SE: 1 }, { TE: 1 }, {}, {}, { SY: 1 }]
  )),

  q("symptoms", "throat_chest_stuck", 32, "목이나 가슴에 무언가 걸린 듯한 답답함을 자주 느끼십니까?", scaleOptions(
    ["전혀 아니다", "아니다", "보통", "그렇다", "매우 그렇다"],
    [{}, {}, { SE: 1 }, { TY: 1, SY: 1 }, { TY: 2, SY: 1 }]
  )),

  q("symptoms", "cold_food_avoidance", 33, "찬 음식을 피하는 편입니까?", scaleOptions(
    ["전혀 아니다", "아니다", "보통", "그렇다", "매우 그렇다"],
    [{ SY: 1 }, {}, { TE: 1 }, { SE: 2 }, { SE: 3 }]
  ), { useForRecommendation: true, cautionTags: ["cold_food_sensitivity"] } as Partial<Question>),

  q("symptoms", "greasy_food_discomfort", 34, "기름진 음식을 먹은 뒤 불편감이 있습니까?", scaleOptions(
    ["전혀 없음", "거의 없음", "보통", "있음", "매우 심함"],
    [{ SY: 1 }, {}, { TE: 1 }, { SE: 1 }, { SE: 2 }]
  ), { useForRecommendation: true }),

  q("symptoms", "meat_preference", 35, "육류(소고기·돼지고기)를 얼마나 좋아하십니까?", scaleOptions(
    ["매우 싫어함", "싫어함", "보통", "좋아함", "매우 좋아함"],
    [{ SE: 1 }, {}, {}, { TE: 1 }, { TE: 1, SY: 1 }]
  ), { useForClassification: false, useForRecommendation: true, weight: 0.5, analysisNoteKo: "음식 선호는 체질 확정보다 추천 개인화에 더 적합하므로 분류 가중치를 낮춥니다." }),

  q("symptoms", "fish_seafood_preference", 36, "생선·해산물을 얼마나 좋아하십니까?", scaleOptions(
    ["매우 싫어함", "싫어함", "보통", "좋아함", "매우 좋아함"],
    [{ SE: 1 }, {}, {}, { SY: 1, TY: 1 }, { SY: 1, TY: 1 }]
  ), { useForClassification: false, useForRecommendation: true, weight: 0.5 }),

  q("symptoms", "chicken_preference", 37, "닭고기를 얼마나 좋아하십니까?", scaleOptions(
    ["매우 싫어함", "싫어함", "보통", "좋아함", "매우 좋아함"],
    [{}, {}, { SE: 1 }, { SE: 1 }, { SE: 1 }]
  ), { useForClassification: false, useForRecommendation: true, weight: 0.5 }),

  q("symptoms", "spicy_preference", 38, "매운맛을 얼마나 좋아하십니까?", scaleOptions(
    ["매우 싫어함", "싫어함", "보통", "좋아함", "매우 좋아함"],
    [{ SE: 1 }, {}, {}, { SY: 1 }, { SY: 1 }]
  ), { useForClassification: false, useForRecommendation: true, weight: 0.5 }),

  q("symptoms", "salty_preference", 39, "짠맛을 얼마나 좋아하십니까?", scaleOptions(
    ["매우 싫어함", "싫어함", "보통", "좋아함", "매우 좋아함"],
    [{ SY: 1 }, {}, {}, { TE: 1 }, { TE: 1 }]
  ), { useForClassification: false, useForRecommendation: true, weight: 0.5 }),

  q("symptoms", "sweet_preference", 40, "단맛을 얼마나 좋아하십니까?", scaleOptions(
    ["매우 싫어함", "싫어함", "보통", "좋아함", "매우 좋아함"],
    [{}, {}, {}, { SE: 1, TE: 1 }, { SE: 1, TE: 1 }]
  ), { useForClassification: false, useForRecommendation: true, weight: 0.5 }),

  q("symptoms", "sour_preference", 41, "신맛을 얼마나 좋아하십니까?", scaleOptions(
    ["매우 싫어함", "싫어함", "보통", "좋아함", "매우 좋아함"],
    [{}, {}, {}, { SE: 1, TY: 1 }, { SE: 1, TY: 1 }]
  ), { useForClassification: false, useForRecommendation: true, weight: 0.5 }),

  q("symptoms", "fishy_food_avoidance", 42, "비린 생선이나 해산물을 피하는 편입니까?", scaleOptions(
    ["전혀 아니다", "아니다", "보통", "그렇다", "매우 그렇다"],
    [{ SY: 1 }, {}, {}, { SE: 1 }, { SE: 1 }]
  ), { useForClassification: false, useForRecommendation: true, weight: 0.5 })
];

await connectDatabase();

const keepIds = questions.map((question) => question.questionId);
const deleteResult = await HumanTypeQA.deleteMany({ questionId: { $nin: keepIds } });

for (const question of questions) {
  await HumanTypeQA.updateOne({ questionId: question.questionId }, { $set: question }, { upsert: true, runValidators: true });
}

console.log(`Human type QA import complete. Upserted ${questions.length} curated questions. Removed ${deleteResult.deletedCount} obsolete questions.`);
console.log("Excluded from the original draft: seated height ratio, navel position, seated waist curve, eyelid form, exact nose/lip/eye-vessel/jaw measurements, occupation preference, memory type, and other low-reliability self-assessment items.");

await mongoose.disconnect();
