# Reference Data Layer Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Fill gaps in the existing (uncommitted) reference data layer, add a typed filter API to `createCrudRouter`, wire filters per route, run the importer, and verify the six reference collections are populated.

**Architecture:** Extend existing `referenceGuidelineSeeds.ts` (don't rewrite) with the missing 9 PDFs in `referenceSources`, 6 secondary conditions in `conditionDietRules`, and ~50 nutrient terms in `nutritionTerminology`. Add a third `filterableFields?: string[]` parameter to `createCrudRouter` that whitelists exact-match and `_gte`/`_lte` filters from the querystring. No commits — user prefers continuous execution.

**Tech Stack:** Mongoose 8 + Express 4, TypeScript ESM (NodeNext, `.js` extensions on `.ts` imports), tsx for script runtime.

**Verification model:** Per `CLAUDE.md`, `npm run build` is the verification step. No test suite exists. We verify by build + running the importer + curl spot checks against a live backend.

---

### Task 1: Extend `createCrudRouter` with `filterableFields` parameter

**Files:**
- Modify: `backend/src/routes/crud.ts`

- [ ] **Step 1: Replace the body of `crud.ts` with the extended version**

Replace the entire file with:

```ts
import { Router } from "express";
import type { Model } from "mongoose";

const RANGE_SUFFIXES = { _gte: "$gte", _lte: "$lte" } as const;

function coerceValue(model: Model<any>, fieldName: string, raw: string): unknown {
  const path = model.schema.path(fieldName);
  const instance = path?.instance;
  if (instance === "Number") return Number(raw);
  if (instance === "Boolean") return raw === "true";
  return raw;
}

function buildFilters(model: Model<any>, query: Record<string, unknown>, allowed: Set<string>) {
  const filters: Record<string, unknown> = {};
  for (const [key, rawValue] of Object.entries(query)) {
    if (rawValue === undefined || rawValue === null || rawValue === "") continue;
    if (key === "q" || key === "page" || key === "limit") continue;
    const valueStr = String(rawValue);

    let suffixMatch: keyof typeof RANGE_SUFFIXES | null = null;
    for (const suffix of Object.keys(RANGE_SUFFIXES) as (keyof typeof RANGE_SUFFIXES)[]) {
      if (key.endsWith(suffix)) { suffixMatch = suffix; break; }
    }

    if (suffixMatch) {
      const baseField = key.slice(0, -suffixMatch.length);
      if (!allowed.has(baseField)) continue;
      const op = RANGE_SUFFIXES[suffixMatch];
      const coerced = coerceValue(model, baseField, valueStr);
      filters[baseField] = { ...(filters[baseField] as object || {}), [op]: coerced };
      continue;
    }

    if (!allowed.has(key)) continue;
    if (valueStr.includes(",")) {
      const values = valueStr.split(",").map((v) => v.trim()).filter(Boolean).map((v) => coerceValue(model, key, v));
      filters[key] = { $in: values };
    } else {
      filters[key] = coerceValue(model, key, valueStr);
    }
  }
  return filters;
}

export function createCrudRouter(model: Model<any>, searchFields: string[] = [], filterableFields: string[] = []) {
  const router = Router();
  const allowed = new Set(filterableFields);

  router.get("/", async (req, res, next) => {
    try {
      const page = Math.max(Number(req.query.page ?? 1), 1);
      const limit = Math.min(Math.max(Number(req.query.limit ?? 20), 1), 500);
      const q = String(req.query.q ?? "").trim();

      const filters = buildFilters(model, req.query as Record<string, unknown>, allowed);
      const textFilter = q
        ? { $or: searchFields.map((field) => ({ [field]: { $regex: q, $options: "i" } })) }
        : null;

      const filter = textFilter ? { $and: [filters, textFilter] } : filters;

      const [items, total] = await Promise.all([
        model.find(filter).sort({ createdAt: -1 }).skip((page - 1) * limit).limit(limit).lean(),
        model.countDocuments(filter)
      ]);

      res.json({ items, total, page, limit });
    } catch (error) {
      next(error);
    }
  });

  router.get("/:id", async (req, res, next) => {
    try {
      const item = await model.findById(req.params.id).lean();
      if (!item) { res.status(404).json({ message: "Resource not found" }); return; }
      res.json(item);
    } catch (error) { next(error); }
  });

  router.post("/", async (req, res, next) => {
    try {
      const item = await model.create({ doctor_verified: false, ...req.body });
      res.status(201).json(item);
    } catch (error) { next(error); }
  });

  router.put("/:id", async (req, res, next) => {
    try {
      const item = await model.findByIdAndUpdate(req.params.id, req.body, { new: true, runValidators: true });
      if (!item) { res.status(404).json({ message: "Resource not found" }); return; }
      res.json(item);
    } catch (error) { next(error); }
  });

  router.delete("/:id", async (req, res, next) => {
    try {
      const item = await model.findByIdAndDelete(req.params.id);
      if (!item) { res.status(404).json({ message: "Resource not found" }); return; }
      res.status(204).send();
    } catch (error) { next(error); }
  });

  return router;
}
```

- [ ] **Step 2: Verify TypeScript compiles**

Run from `backend/`: `npx tsc --noEmit`
Expected: exit 0, no errors.

---

### Task 2: Wire `filterableFields` per route in `routes/index.ts`

**Files:**
- Modify: `backend/src/routes/index.ts:84-96` (the `createCrudRouter` call sites)

- [ ] **Step 1: Add a third argument to each new-collection route**

Find each line and add the filter whitelist as the third arg. Example for `reference-sources`:

```ts
apiRouter.use("/reference-sources", createCrudRouter(ReferenceSource, ["sourceKey", "title", "standardCode", "category", "topic", "conditionKey", "dataSource"], ["category", "topic", "conditionKey", "year", "dataSource", "doctor_verified"]));
apiRouter.use("/nutrient-intake-rules", createCrudRouter(NutrientIntakeRule, ["ruleKey", "standardCode", "nutrientKey", "nutrientLabel", "referenceType", "ageGroup", "gender", "lifeStage", "dataSource"], ["standardCode", "nutrientKey", "referenceType", "ageGroup", "ageMin", "ageMax", "gender", "lifeStage", "populationGroup", "dataSource", "doctor_verified"]));
apiRouter.use("/condition-diet-rules", createCrudRouter(ConditionDietRule, ["ruleKey", "conditionKey", "conditionLabel", "ruleType", "nutrientKey", "recommendationKo", "dataSource"], ["conditionKey", "ruleType", "comparator", "nutrientKey", "dataSource", "doctor_verified"]));
apiRouter.use("/risk-assessment-rules", createCrudRouter(RiskAssessmentRule, ["ruleKey", "standardCode", "metricKey", "metricLabel", "populationGroup", "interpretationKo", "dataSource"], ["standardCode", "metricKey", "populationGroup", "ageMin", "ageMax", "gender", "dataSource", "doctor_verified"]));
apiRouter.use("/nutrition-terminology", createCrudRouter(NutritionTerminology, ["termKey", "category", "chineseTerm", "englishTerm", "koreanTerm", "abbreviation", "dataSource"], ["category", "dataSource", "doctor_verified"]));
apiRouter.use("/data-validation-rules", createCrudRouter(DataValidationRule, ["ruleKey", "targetCollection", "fieldPath", "ruleType", "messageKo", "dataSource"], ["targetCollection", "ruleType", "required", "dataSource", "doctor_verified"]));
```

Also add `["category", "foodGroup", "foodSubgroup", "doctor_verified", "dataSource"]` as third arg to the `foodsRouter` line (`backend/src/routes/index.ts:39`).

- [ ] **Step 2: Verify build still passes**

Run from repo root: `npm run build`
Expected: exit 0.

---

### Task 3: Add 9 WST 膳食指导 PDFs to `referenceSources`

**Files:**
- Modify: `backend/src/data/referenceGuidelineSeeds.ts:6-223` (the `referenceSources` array)

- [ ] **Step 1: Append 9 entries to `referenceSources`**

Just before the closing `] as const;` on line 223, insert these entries (keeping the same shape as existing entries — `${standardsRoot}/第三部分 膳食指导/...`):

```ts
  {
    sourceKey: "wst429-2013-adult-diabetes-diet-guide",
    title: "成人糖尿病患者膳食指导",
    standardCode: "WST429-2013",
    year: 2013,
    category: "clinical_diet_guidance",
    topic: "diabetes",
    conditionKey: "diabetes",
    filePath: `${standardsRoot}/第三部分 膳食指导/WST429-2013 成人糖尿病患者膳食指导.pdf`,
    dataSource: dietGuideDataSource,
    sourceNote: "Adult diabetes patient dietary guidance.",
    tags: ["diabetes", "clinical_diet"]
  },
  {
    sourceKey: "wst430-2013-hypertension-diet-guide",
    title: "高血压患者膳食指导",
    standardCode: "WST430-2013",
    year: 2013,
    category: "clinical_diet_guidance",
    topic: "hypertension",
    conditionKey: "hypertension",
    filePath: `${standardsRoot}/第三部分 膳食指导/WST430-2013 高血压患者膳食指导.pdf`,
    dataSource: dietGuideDataSource,
    sourceNote: "Hypertension patient dietary guidance.",
    tags: ["hypertension", "clinical_diet"]
  },
  {
    sourceKey: "wst556-2017-elderly-diet-guide",
    title: "老年人膳食指导",
    standardCode: "WST556-2017",
    year: 2017,
    category: "clinical_diet_guidance",
    topic: "elderly",
    conditionKey: "elderly",
    filePath: `${standardsRoot}/第三部分 膳食指导/WST556-2017 老年人膳food指导.pdf`.replace("膳food", "膳食"),
    dataSource: dietGuideDataSource,
    sourceNote: "Elderly dietary guidance.",
    tags: ["elderly", "clinical_diet"]
  },
  {
    sourceKey: "wst557-2017-ckd-diet-guide",
    title: "慢性肾脏病患者膳食指导",
    standardCode: "WST557-2017",
    year: 2017,
    category: "clinical_diet_guidance",
    topic: "ckd",
    conditionKey: "ckd",
    filePath: `${standardsRoot}/第三部分 膳食指导/WST557-2017 慢性肾脏病患者膳食指导.pdf`,
    dataSource: dietGuideDataSource,
    sourceNote: "Chronic kidney disease patient dietary guidance.",
    tags: ["ckd", "kidney", "clinical_diet"]
  },
  {
    sourceKey: "wst558-2017-stroke-diet-guide",
    title: "脑卒中患者膳食指导",
    standardCode: "WST558-2017",
    year: 2017,
    category: "clinical_diet_guidance",
    topic: "stroke",
    conditionKey: "stroke",
    filePath: `${standardsRoot}/第三部分 膳食指导/WST558-2017 脑卒中患者膳食指导.pdf`,
    dataSource: dietGuideDataSource,
    sourceNote: "Stroke patient dietary guidance.",
    tags: ["stroke", "clinical_diet"]
  },
  {
    sourceKey: "wst559-2017-cancer-diet-guide",
    title: "恶性肿瘤患者膳食指导",
    standardCode: "WST559-2017",
    year: 2017,
    category: "clinical_diet_guidance",
    topic: "cancer",
    conditionKey: "cancer",
    filePath: `${standardsRoot}/第三部分 膳食指导/WST559-2017 恶性肿瘤患者膳食指导.pdf`,
    dataSource: dietGuideDataSource,
    sourceNote: "Malignant tumor patient dietary guidance.",
    tags: ["cancer", "clinical_diet"]
  },
  {
    sourceKey: "wst560-2017-gout-diet-guide",
    title: "高尿酸血症与痛风患者膳食指导",
    standardCode: "WST560-2017",
    year: 2017,
    category: "clinical_diet_guidance",
    topic: "gout_hyperuricemia",
    conditionKey: "gout_hyperuricemia",
    filePath: `${standardsRoot}/第三部分 膳食指导/WST560-2017 高尿酸血症与痛风患者膳食指导.pdf`,
    dataSource: dietGuideDataSource,
    sourceNote: "Hyperuricemia and gout patient dietary guidance.",
    tags: ["gout", "hyperuricemia", "clinical_diet"]
  },
  {
    sourceKey: "wst601-2018-gestational-diabetes-diet-guide",
    title: "妊娠期糖尿病患者膳食指导",
    standardCode: "WST601-2018",
    year: 2018,
    category: "clinical_diet_guidance",
    topic: "gestational_diabetes",
    conditionKey: "gestational_diabetes",
    filePath: `${standardsRoot}/第三部分 膳食指导/WST601-2018 妊娠期糖尿病患者膳食指导.pdf`,
    dataSource: dietGuideDataSource,
    sourceNote: "Gestational diabetes patient dietary guidance.",
    tags: ["pregnancy", "gestational_diabetes", "clinical_diet"]
  },
  {
    sourceKey: "wst678-2020-infant-complementary-feeding",
    title: "婴幼儿辅食添加营养指南",
    standardCode: "WST678-2020",
    year: 2020,
    category: "clinical_diet_guidance",
    topic: "infant_complementary_feeding",
    conditionKey: "infant_complementary_feeding",
    filePath: `${standardsRoot}/第三部分 膳食指导/WST678-2020 婴幼儿辅食添加营养指南.pdf`,
    dataSource: dietGuideDataSource,
    sourceNote: "Infant complementary feeding nutrition guide.",
    tags: ["infant", "complementary_feeding", "clinical_diet"]
  }
```

(For the `wst556` entry, the trick `"...膳食指导.pdf"`.replace(...) is unnecessary — write the path directly with `膳食指导`. The replace was only an artifact of avoiding raw character escapes; remove it: `filePath: \`${standardsRoot}/第三部分 膳食指导/WST556-2017 老年人膳食指导.pdf\`,`)

- [ ] **Step 2: Update `import-reference-guidelines.ts` `replaceMany` filter for `referenceSources`**

The existing filter at `backend/src/scripts/import-reference-guidelines.ts:107-115` already covers `Chinese national food therapy and diet guidelines` (the `dietGuideDataSource`), which is what we used for the new 9. No change needed — they will be wiped+reinserted along with the existing condition guides.

---

### Task 4: Add 6 secondary conditions to `conditionDietRules`

**Files:**
- Modify: `backend/src/data/referenceGuidelineSeeds.ts:225-259` (the `conditionDietRules` array literal at lines 226-240, before the `.map(...)` transform)

- [ ] **Step 1: Append the rows to the array literal**

Just before line 240's closing `]`, append these rows (each row matches the existing tuple shape: `[conditionKey, conditionLabel, ruleType, nutrientKey, comparator, targetValue, unit, recommendationKo, prefer, avoid]`):

```ts
  ["ckd", "만성콩팥병", "protein_quality", "protein", "range", undefined, "g/kg/d", "신장 기능에 맞춘 단백질 양과 양질의 단백질 비중을 관리합니다.", "high_quality_protein,egg,milk", "processed_meat"],
  ["ckd", "만성콩팥병", "sodium_limit", "sodium", "lte", 2000, "mg/d", "나트륨을 제한하고 가공식품과 짠 양념을 피합니다.", "fresh_food", "high_sodium,processed_food"],
  ["ckd", "만성콩팥병", "potassium_management", "potassium", "lte", undefined, "mg/d", "고칼륨혈증 위험이 있는 경우 칼륨 함량이 높은 과일과 채소를 제한합니다.", "low_potassium_vegetable", "banana,orange,potato"],
  ["ckd", "만성콩팥병", "phosphorus_management", "phosphorus", "lte", undefined, "mg/d", "인 함량이 높은 가공식품과 인 첨가물을 줄입니다.", "fresh_meat", "phosphate_additive,cola"],
  ["ckd", "만성콩팥병", "fluid_balance", "", "prefer", undefined, "", "수분 섭취량은 신장 기능과 부종 정도에 맞춰 조절합니다.", "balanced_fluid", "excess_fluid"],
  ["stroke", "뇌졸중", "sodium_limit", "sodium", "lte", 2000, "mg/d", "나트륨 섭취를 줄여 혈압 관리에 도움이 되도록 합니다.", "low_sodium", "high_sodium,pickled_food"],
  ["stroke", "뇌졸중", "saturated_fat_limit", "saturatedFat", "lt", 10, "%E", "포화지방과 트랜스지방 섭취를 낮추고 생선과 식물성 지방 비중을 늘립니다.", "fish,plant_oil,nuts", "fried_food,high_saturated_fat"],
  ["stroke", "뇌졸중", "fiber_priority", "fiber", "gte", 25, "g/d", "통곡, 채소, 과일, 콩류로 식이섬유 섭취를 늘립니다.", "whole_grain,vegetable,fruit,legume", ""],
  ["stroke", "뇌졸중", "alcohol_limit", "", "avoid", undefined, "", "음주를 제한하고 식사를 규칙적으로 합니다.", "regular_meal", "alcohol"],
  ["cancer", "악성종양", "energy_protein_priority", "protein", "prefer", undefined, "g/kg/d", "치료기에는 충분한 에너지와 양질 단백질을 우선 공급합니다.", "high_quality_protein,egg,fish,milk", "low_energy_density"],
  ["cancer", "악성종양", "vegetable_fruit_priority", "", "prefer", undefined, "", "다양한 색의 채소와 과일을 매일 섭취해 미량영양소를 확보합니다.", "vegetable,fruit,whole_grain", ""],
  ["cancer", "악성종양", "processed_meat_limit", "", "avoid", undefined, "", "가공육과 훈제·염장 식품 섭취를 줄입니다.", "fresh_meat,fish", "processed_meat,smoked_food"],
  ["cancer", "악성종양", "alcohol_limit", "", "avoid", undefined, "", "음주를 제한합니다.", "", "alcohol"],
  ["elderly", "고령자", "protein_priority", "protein", "gte", 1.0, "g/kg/d", "근감소 예방을 위해 양질 단백질을 매 끼 분산하여 섭취합니다.", "egg,fish,legume,milk,tofu", ""],
  ["elderly", "고령자", "calcium_vitamin_d", "calcium", "prefer", undefined, "mg/d", "골다공증 예방을 위해 칼슘과 비타민D 섭취를 챙기고 햇빛 노출도 권합니다.", "milk,dairy,small_fish,vegetable", ""],
  ["elderly", "고령자", "fiber_fluid", "fiber", "gte", 25, "g/d", "변비 예방을 위해 식이섬유와 수분 섭취를 충분히 합니다.", "whole_grain,vegetable,fruit", ""],
  ["elderly", "고령자", "easy_to_chew_swallow", "", "prefer", undefined, "", "씹고 삼키기 쉬운 형태로 조리하고 식사 횟수를 적절히 분배합니다.", "soft_food,small_meal", "hard_food"],
  ["gestational_diabetes", "임신성 당뇨병", "carb_distribution", "carbs", "range", undefined, "%E", "탄수화물을 식사와 간식으로 분산해 식후 혈당 변동을 줄입니다.", "whole_grain,low_gi,vegetable", "added_sugar,sweet_drink"],
  ["gestational_diabetes", "임신성 당뇨병", "protein_priority", "protein", "gte", 1.0, "g/kg/d", "양질 단백질을 충분히 섭취하고 생선, 콩류, 달걀을 활용합니다.", "fish,legume,egg,low_fat_meat", ""],
  ["gestational_diabetes", "임신성 당뇨병", "added_sugar_avoid", "addedSugar", "lt", 5, "%E", "단 음료와 단 간식, 과당 시럽 식품을 피합니다.", "water,low_sugar", "sweet_drink,fructose,added_sugar"],
  ["gestational_diabetes", "임신성 당뇨병", "regular_meals", "", "prefer", undefined, "", "식사는 규칙적으로 하고 야식과 긴 공복을 피합니다.", "regular_meal,small_frequent", "late_night_snack"],
  ["gestational_diabetes", "임신성 당뇨병", "weight_gain_safe", "", "prefer", undefined, "", "임신 전 BMI에 맞춘 체중 증가 범위를 따릅니다 (WST801 기준).", "balanced_weight_gain", ""],
  ["infant_complementary_feeding", "이유보충식", "iron_priority", "iron", "prefer", undefined, "mg/d", "6개월부터 철분이 풍부한 이유식을 단계적으로 도입합니다.", "iron_fortified,red_meat,liver", ""],
  ["infant_complementary_feeding", "이유보충식", "salt_sugar_avoid", "", "avoid", undefined, "", "1세 미만 영아의 이유식에는 소금과 설탕을 추가하지 않습니다.", "natural_food", "salt,added_sugar,honey"],
  ["infant_complementary_feeding", "이유보충식", "texture_progression", "", "prefer", undefined, "", "월령에 따라 미음에서 죽, 진밥, 다진 음식 순으로 점진적으로 진행합니다.", "age_appropriate_texture", ""],
  ["infant_complementary_feeding", "이유보충식", "allergen_introduction", "", "prefer", undefined, "", "달걀, 생선, 견과류 등 알레르기 식품은 한 가지씩 단계적으로 도입합니다.", "single_introduction", "multiple_allergen_at_once"],
  ["infant_complementary_feeding", "이유보충식", "honey_avoid", "", "avoid", undefined, "", "보툴리누스 위험으로 12개월 미만에는 꿀을 주지 않습니다.", "", "honey"]
```

That's 27 new rows across 6 conditions, plus the 15 existing = 42 total `conditionDietRules`.

---

### Task 5: Expand `nutritionTerminology` with ~50 nutrient terms

**Files:**
- Modify: `backend/src/data/referenceGuidelineSeeds.ts:401-422` (the `nutritionTerminology` array)

- [ ] **Step 1: Append nutrient term rows to the array**

Before the closing `].map(...)` on line 410, insert:

```ts
  ["energy_kcal", "能量", "Energy", "에네르기", "kcal", "음식이 제공하는 화학 에너지로 보통 kcal 단위로 표시합니다."],
  ["protein", "蛋白质", "Protein", "단백질", "Pro", "아미노산으로 구성된 다량영양소로 g/d 단위로 평가합니다."],
  ["fat", "脂肪", "Fat", "지방", "", "지방산과 글리세롤로 구성된 다량영양소로 g/d 단위로 평가합니다."],
  ["saturated_fat", "饱和脂肪酸", "Saturated Fat", "포화지방", "SFA", "이중결합이 없는 지방산으로 과다 섭취 시 LDL 콜레스테롤을 올릴 수 있습니다."],
  ["unsaturated_fat", "不饱和脂肪酸", "Unsaturated Fat", "불포화지방", "UFA", "이중결합이 있는 지방산으로 단일·다가 불포화지방산을 포함합니다."],
  ["trans_fat", "反式脂肪酸", "Trans Fat", "트랜스지방", "TFA", "수소화로 생성되는 지방산으로 섭취량을 가능한 낮게 유지합니다."],
  ["cholesterol", "胆固醇", "Cholesterol", "콜레스테롤", "", "동물성 식품의 지질 성분으로 mg/d 단위로 평가합니다."],
  ["carbohydrate", "碳水化合物", "Carbohydrate", "탄수화물", "CHO", "에너지의 주요 공급원이며 단순당과 복합당을 포함합니다."],
  ["dietary_fiber", "膳食纤维", "Dietary Fiber", "식이섬유", "DF", "사람이 소화하지 못하는 식물성 다당으로 변통과 혈당 조절에 도움이 됩니다."],
  ["added_sugar", "添加糖", "Added Sugar", "첨가당", "", "가공이나 조리 과정에서 더해진 당으로 총 에너지의 10% 미만 권장합니다."],
  ["water", "水", "Water", "물", "", "체액 균형과 대사에 필수인 영양소로 ml/d 단위로 평가합니다."],
  ["sodium", "钠", "Sodium", "나트륨", "Na", "혈압 조절에 영향을 주는 무기질로 mg/d 단위로 평가합니다."],
  ["potassium", "钾", "Potassium", "칼리움", "K", "혈압과 근육 기능에 관여하는 무기질로 mg/d 단위로 평가합니다."],
  ["calcium", "钙", "Calcium", "칼시움", "Ca", "뼈와 치아 형성, 근수축에 관여하는 무기질입니다."],
  ["magnesium", "镁", "Magnesium", "마그네시움", "Mg", "효소 반응과 신경·근육 기능에 관여하는 무기질입니다."],
  ["phosphorus", "磷", "Phosphorus", "린", "P", "뼈와 ATP 구성에 관여하는 무기질로 mg/d 단위로 평가합니다."],
  ["chloride", "氯", "Chloride", "염소", "Cl", "체액 전해질 균형에 관여하는 무기질입니다."],
  ["iron", "铁", "Iron", "철", "Fe", "헤모글로빈 합성과 산소 운반에 필수인 미량원소입니다."],
  ["zinc", "锌", "Zinc", "아연", "Zn", "면역과 성장에 관여하는 미량원소입니다."],
  ["copper", "铜", "Copper", "구리", "Cu", "철 대사와 항산화 효소 활성에 관여하는 미량원소입니다."],
  ["selenium", "硒", "Selenium", "세렌", "Se", "항산화 효소 글루타티온과산화효소의 구성 성분입니다."],
  ["iodine", "碘", "Iodine", "옥소", "I", "갑상선 호르몬 합성에 필수인 미량원소입니다."],
  ["manganese", "锰", "Manganese", "망가니즈", "Mn", "효소 반응과 골격 형성에 관여하는 미량원소입니다."],
  ["chromium", "铬", "Chromium", "크롬", "Cr", "당대사 보조 인자로 작용하는 미량원소입니다."],
  ["molybdenum", "钼", "Molybdenum", "몰리브덴", "Mo", "여러 효소의 보조 인자로 작용하는 미량원소입니다."],
  ["fluoride", "氟", "Fluoride", "플루오르", "F", "치아 에나멜과 골격 형성에 영향을 주는 미량원소입니다."],
  ["vitamin_a", "维生素A", "Vitamin A", "비타민A", "VA", "시각, 상피세포, 면역에 관여하는 지용성 비타민입니다."],
  ["vitamin_d", "维生素D", "Vitamin D", "비타민D", "VD", "칼슘 흡수와 골 건강에 관여하는 지용성 비타민입니다."],
  ["vitamin_e", "维生素E", "Vitamin E", "비타민E", "VE", "세포막 보호와 항산화에 작용하는 지용성 비타민입니다."],
  ["vitamin_k", "维生素K", "Vitamin K", "비타민K", "VK", "혈액응고와 골대사에 관여하는 지용성 비타민입니다."],
  ["vitamin_b1", "维生素B1", "Thiamine", "비타민B1", "VB1", "탄수화물 대사에 필수인 수용성 비타민입니다."],
  ["vitamin_b2", "维生素B2", "Riboflavin", "비타민B2", "VB2", "에너지 대사 효소의 보조 인자로 작용합니다."],
  ["vitamin_b6", "维生素B6", "Pyridoxine", "비타민B6", "VB6", "아미노산 대사와 헤모글로빈 합성에 관여합니다."],
  ["vitamin_b12", "维生素B12", "Cobalamin", "비타민B12", "VB12", "DNA 합성과 적혈구 형성에 관여합니다."],
  ["niacin", "烟酸", "Niacin", "니코틴산", "NE", "에너지 대사와 NAD/NADP 보조효소 합성에 관여합니다."],
  ["folate", "叶酸", "Folate", "엽산", "DFE", "DNA 합성과 적혈구 형성에 관여하며 임신 전·중에 특히 중요합니다."],
  ["biotin", "生物素", "Biotin", "비오틴", "VB7", "지방·탄수화물·아미노산 대사에 관여하는 수용성 비타민입니다."],
  ["pantothenic_acid", "泛酸", "Pantothenic Acid", "판토텐산", "VB5", "코엔자임A 구성 성분으로 에너지 대사에 관여합니다."],
  ["choline", "胆碱", "Choline", "콜린", "", "세포막 인지질과 신경전달물질 아세틸콜린의 전구체입니다."],
  ["vitamin_c", "维生素C", "Vitamin C", "비타민C", "VC", "콜라겐 합성과 항산화 작용을 하는 수용성 비타민입니다."],
  ["omega_3", "n-3多不饱和脂肪酸", "n-3 PUFA", "오메가-3 지방산", "n-3", "EPA, DHA, ALA를 포함하는 다가 불포화지방산으로 심혈관 건강에 도움이 됩니다."],
  ["omega_6", "n-6多不饱和脂肪酸", "n-6 PUFA", "오메가-6 지방산", "n-6", "리놀레산을 포함하는 다가 불포화지방산입니다."],
  ["gi", "血糖生成指数", "Glycemic Index", "혈당지수", "GI", "표준 식품(보통 포도당) 대비 식후 혈당 반응을 비교한 지표입니다."],
  ["gl", "血糖生成负荷", "Glycemic Load", "혈당부하", "GL", "식품 1회 분량의 GI×탄수화물량을 반영한 지표입니다."],
  ["dv_percent", "营养素参考值百分比", "Daily Value Percent", "기준치 백분율", "%DV", "영양표시에서 1일 영양소 기준치 대비 함량 비율을 나타냅니다."],
  ["nrv", "营养素参考值", "Nutrient Reference Value", "영양소 기준치", "NRV", "영양표시 산정에 쓰는 1일 권장 기준값입니다."]
```

That brings `nutritionTerminology` from 8 to 54 entries.

- [ ] **Step 2: Verify build still passes**

Run from repo root: `npm run build`
Expected: exit 0.

---

### Task 6: Run the importer against local Mongo

**Files:**
- No code changes; existing `import-reference-guidelines.ts` runs end-to-end.

- [ ] **Step 1: Confirm Mongo is reachable**

Run from repo root: `docker compose up -d mongo`
(Or skip if Mongo is already running locally on `mongodb://127.0.0.1:27017/foodvisor`.)
Expected: container `mongo` is up.

- [ ] **Step 2: Run the importer**

Run from `backend/`: `npm run import:reference-guidelines`
Expected output (exact counts will vary slightly):
```
referenceSources: deleted N, inserted 27
nutrientIntakeRules: deleted N, inserted ~12000
conditionDietRules: deleted N, inserted 42
riskAssessmentRules: deleted N, inserted 5
nutritionTerminology: deleted N, inserted 54
dataValidationRules: deleted N, inserted 8
```

Process exits 0.

- [ ] **Step 3: Re-run the importer to confirm idempotency**

Run again from `backend/`: `npm run import:reference-guidelines`
Expected: same `inserted` counts; `deleted` counts equal previous `inserted` counts (replaceMany semantics — end state identical).

---

### Task 7: Spot-check the API

**Files:**
- No code changes.

- [ ] **Step 1: Start the backend**

Run from `backend/`: `npm run dev` in a terminal you can leave running.
Expected: server listens on `http://localhost:4000`.

- [ ] **Step 2: Curl the new filter API**

In another terminal:

```bash
# Reference sources by category
curl -s 'http://localhost:4000/api/reference-sources?category=clinical_diet_guidance' | grep -c '"_id"'
# Expected: 9

# Condition rules for hypertension
curl -s 'http://localhost:4000/api/condition-diet-rules?conditionKey=hypertension' | grep -c '"_id"'
# Expected: 3

# Range filter on nutrient rules
curl -s 'http://localhost:4000/api/nutrient-intake-rules?nutrientKey=protein&gender=male&ageMin_gte=18&ageMax_lte=29&limit=5' | head -c 200
# Expected: JSON with items array, each having nutrientKey="protein", gender="male", ageMin>=18, ageMax<=29

# Comma-separated $in
curl -s 'http://localhost:4000/api/condition-diet-rules?conditionKey=ckd,stroke' | grep -c '"_id"'
# Expected: ≥9

# Terminology page
curl -s 'http://localhost:4000/api/nutrition-terminology?category=nutrition_reference&limit=100' | grep -c '"_id"'
# Expected: ≥54
```

If any expected count is wrong, check `import-reference-guidelines.ts` ran cleanly and the route's `filterableFields` whitelist includes the field being queried.

---

### Task 8: Spot-check the admin UI

**Files:**
- No code changes.

- [ ] **Step 1: Start the admin**

Run from `web-admin/`: `npm run dev`
Expected: serves on `http://localhost:3000`.

- [ ] **Step 2: Visit each new page and confirm rows render**

Open in a browser:
- `http://localhost:3000/reference-sources` — should list 27 rows
- `http://localhost:3000/nutrient-intake-rules` — pagination over thousands of rows
- `http://localhost:3000/condition-diet-rules` — 42 rows
- `http://localhost:3000/risk-assessment-rules` — 5 rows
- `http://localhost:3000/nutrition-terminology` — 54 rows
- `http://localhost:3000/data-validation-rules` — 8 rows

Open the edit modal on one row in each, change `sourceNote` slightly, save, reload to confirm persistence.

- [ ] **Step 3: Final build verification**

Run from repo root: `npm run build`
Expected: exit 0, both backend `tsc` and `web-admin` `next build` succeed.

---

## Self-Review Notes

- All tasks have explicit file paths and line ranges.
- Code samples are complete (no `// implement here` or `// TODO`).
- Type signatures: `createCrudRouter` (Task 1) defined with three params; Task 2 calls it with three args. Consistent.
- Field names: `filterableFields` used everywhere (not aliased). Consistent.
- The `replace("膳food", "膳食")` callout in Task 3 is a documentation note — the actual file content uses `膳食指导` directly.
- Verification steps reference `npm run build` (per CLAUDE.md), not a non-existent test suite.
- Out-of-scope (admin polish, dashboard, charts, NK Korean conversion) is explicitly Sub-project B; not addressed here.

## Acceptance

This plan ships when:
- Tasks 1–2 land the typed filter API.
- Tasks 3–5 expand the seed data to the spec's targets.
- Tasks 6 verifies idempotent import.
- Tasks 7–8 verify API + admin work end-to-end.
- `npm run build` passes from repo root (Task 8 step 3).
