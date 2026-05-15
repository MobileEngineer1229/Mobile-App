"use client";

import { AlertTriangle, BadgeCheck, Ban, CheckCircle2, HeartPulse, Pencil, Plus, RefreshCw, Search, ShieldAlert, SlidersHorizontal, X } from "lucide-react";
import { FormEvent, useEffect, useMemo, useState } from "react";
import Modal from "@/components/Modal";
import PageHeader from "@/components/PageHeader";
import { apiFetch } from "@/lib/api";

type DietRule = {
  _id: string;
  ruleKey: string;
  conditionKey: string;
  conditionLabel: string;
  ruleType: string;
  priority?: number;
  nutrientKey?: string;
  comparator?: "eq" | "lt" | "lte" | "gt" | "gte" | "range" | "avoid" | "prefer";
  targetValue?: number;
  targetMin?: number;
  targetMax?: number;
  unit?: string;
  recommendationKo: string;
  foodTagsPrefer?: string[];
  foodTagsAvoid?: string[];
  cautionTags?: string[];
  dataSource: string;
  doctor_verified?: boolean;
};

type ApiList = {
  items: DietRule[];
  total: number;
  page: number;
  limit: number;
};

type ConditionOption = {
  _id: string;
  conditionKey: string;
  conditionLabel: string;
  descriptionKo?: string;
  sortOrder?: number;
  dataSource?: string;
  doctor_verified?: boolean;
};

type ConditionApiList = {
  items: ConditionOption[];
  total: number;
  page: number;
  limit: number;
};

type RuleMode = "all" | "avoid" | "prefer" | "limit";
type Comparator = NonNullable<DietRule["comparator"]>;

const conditionNames: Record<string, string> = {
  hypertension: "고혈압",
  hyperlipidemia: "고지혈증",
  diabetes: "당뇨병",
  adult_obesity: "성인 비만",
  child_obesity: "아동청소년 비만",
  gout_hyperuricemia: "고뇨산혈증/통풍",
  ckd: "만성콩팥병",
  stroke: "뇌졸중",
  cancer: "악성종양",
  elderly: "고령자",
  gestational_diabetes: "임신성 당뇨병",
  infant_complementary_feeding: "이유보충식"
};

const nutrientNames: Record<string, string> = {
  sodium: "나트륨",
  potassium: "칼륨",
  carbs: "탄수화물",
  addedSugar: "첨가당",
  saturatedFat: "포화지방",
  cholesterol: "콜레스테롤",
  fiber: "식이섬유",
  energyKcal: "열량",
  protein: "단백질",
  phosphorus: "린",
  calcium: "칼시움",
  iron: "철분"
};

const tagNames: Record<string, string> = {
  low_sodium: "싱겁게",
  high_sodium: "짠 음식",
  processed_food: "가공식품",
  pickled_food: "절임식품",
  high_potassium: "칼륨 많은 식품",
  high_fiber: "식이섬유 많은 식품",
  whole_grain: "통곡",
  vegetable: "남새",
  fruit: "과일",
  legume: "콩류",
  low_gi: "낮은 혈당지수",
  sweet_drink: "단 음료",
  added_sugar: "첨가당",
  sweet_snack: "단 간식",
  fried_food: "튀긴 음식",
  high_saturated_fat: "포화지방 많은 식품",
  high_cholesterol: "콜레스테롤 많은 식품",
  plant_food: "식물성 식품",
  unsaturated_fat: "불포화지방",
  fish: "물고기",
  low_energy_density: "낮은 열량밀도",
  high_energy_density: "높은 열량밀도",
  high_protein: "단백질 많은 식품",
  vegetable_fruit_priority: "남새와 과일",
  lean_protein: "살코기 단백질",
  fast_food: "속성음식",
  balanced_meal: "균형식",
  milk: "젖제품",
  regular_meal: "규칙식사",
  activity: "활동 늘리기",
  late_night_snack: "밤참",
  low_purine: "저퓨린 식품",
  high_purine_seafood: "퓨린 많은 해산물",
  organ_meat: "내장고기",
  alcohol: "술",
  water: "물",
  low_sugar: "저당",
  fructose: "과당",
  high_quality_protein: "질 좋은 단백질",
  egg: "닭알",
  tofu: "두부",
  low_potassium_vegetable: "저칼륨 남새",
  banana: "바나나",
  orange: "오렌지",
  potato: "감자",
  fresh_food: "신선식품",
  fresh_meat: "신선한 고기",
  phosphate_additive: "린 첨가물",
  cola: "콜라",
  balanced_fluid: "수분 균형",
  excess_fluid: "수분 과다",
  plant_oil: "식물성 기름",
  nuts: "견과류",
  smoked_food: "훈제식품",
  processed_meat: "가공육",
  dairy: "젖제품",
  small_fish: "잔물고기",
  soft_food: "부드러운 음식",
  small_meal: "소량식",
  hard_food: "딱딱한 음식",
  small_frequent: "소량 자주 먹기",
  balanced_weight_gain: "체중증가 균형",
  iron_fortified: "철분 강화식",
  red_meat: "붉은 고기",
  liver: "간",
  natural_food: "자연식품",
  salt: "소금",
  honey: "꿀",
  age_appropriate_texture: "월령맞춤 질감",
  single_introduction: "한가지씩 도입",
  multiple_allergen_at_once: "여러 알레르기 식품 동시도입"
};

const modeOptions: Array<{ value: RuleMode; label: string }> = [
  { value: "all", label: "전체 규칙" },
  { value: "avoid", label: "제한/금지" },
  { value: "prefer", label: "권장" },
  { value: "limit", label: "수치 기준" }
];

const comparatorOptions: Array<{ value: Comparator; label: string }> = [
  { value: "avoid", label: "제한/금지" },
  { value: "prefer", label: "권장" },
  { value: "lte", label: "이하" },
  { value: "lt", label: "미만" },
  { value: "gte", label: "이상" },
  { value: "gt", label: "초과" },
  { value: "range", label: "범위" },
  { value: "eq", label: "같음" }
];

const dataSourceDefault = "Chinese national food therapy and diet guidelines";

function labelCondition(rule: DietRule) {
  return conditionNames[rule.conditionKey] ?? rule.conditionLabel ?? rule.conditionKey;
}

function conditionLabelFromOptions(conditions: ConditionOption[], conditionKey: string) {
  return conditions.find((item) => item.conditionKey === conditionKey)?.conditionLabel ?? conditionNames[conditionKey] ?? conditionKey;
}

function labelTag(tag: string) {
  return tagNames[tag] ?? tag.replaceAll("_", " ");
}

function ruleMode(rule: DietRule): Exclude<RuleMode, "all"> {
  if (rule.comparator === "avoid" || (rule.foodTagsAvoid?.length ?? 0) > 0 || (rule.cautionTags?.length ?? 0) > 0) return "avoid";
  if (rule.comparator === "prefer") return "prefer";
  return "limit";
}

function modeLabel(mode: Exclude<RuleMode, "all">) {
  if (mode === "avoid") return "제한";
  if (mode === "prefer") return "권장";
  return "기준";
}

function targetText(rule: DietRule) {
  const nutrient = rule.nutrientKey ? nutrientNames[rule.nutrientKey] ?? rule.nutrientKey : "음식";
  const unit = rule.unit ? ` ${rule.unit}` : "";
  if (rule.comparator === "avoid") return "피할것";
  if (rule.comparator === "prefer") return "우선";
  if (rule.comparator === "lt") return `${nutrient} < ${rule.targetValue}${unit}`;
  if (rule.comparator === "lte") return `${nutrient} <= ${rule.targetValue ?? "기준"}${unit}`;
  if (rule.comparator === "gt") return `${nutrient} > ${rule.targetValue}${unit}`;
  if (rule.comparator === "gte") return `${nutrient} >= ${rule.targetValue}${unit}`;
  if (rule.comparator === "range") {
    const min = rule.targetMin != null ? rule.targetMin : "";
    const max = rule.targetMax != null ? rule.targetMax : "";
    return `${nutrient} ${min}-${max}${unit}`.replace(" -", " ");
  }
  return nutrient;
}

function modeIcon(mode: Exclude<RuleMode, "all">) {
  if (mode === "avoid") return Ban;
  if (mode === "prefer") return CheckCircle2;
  return SlidersHorizontal;
}

function modeClass(mode: Exclude<RuleMode, "all">) {
  if (mode === "avoid") return "condition-rule-card is-avoid";
  if (mode === "prefer") return "condition-rule-card is-prefer";
  return "condition-rule-card is-limit";
}

function groupByCondition(items: DietRule[], conditions: ConditionOption[]) {
  const groups = new Map<string, DietRule[]>();
  for (const item of items) {
    const key = item.conditionKey || "other";
    groups.set(key, [...(groups.get(key) ?? []), item]);
  }
  return [...groups.entries()].map(([conditionKey, rules]) => ({
    conditionKey,
    label: conditionLabelFromOptions(conditions, conditionKey) ?? rules[0]?.conditionLabel ?? conditionKey,
    rules: [...rules].sort((a, b) => (a.priority ?? 0) - (b.priority ?? 0))
  }));
}

function visibleByMode(rule: DietRule, mode: RuleMode) {
  return mode === "all" || ruleMode(rule) === mode;
}

function tagsToText(tags?: string[]) {
  return (tags ?? []).join(", ");
}

function parseTags(value: FormDataEntryValue | null) {
  return String(value ?? "")
    .split(",")
    .map((tag) => tag.trim())
    .filter(Boolean);
}

function optionalNumber(value: FormDataEntryValue | null) {
  const text = String(value ?? "").trim();
  if (!text) return undefined;
  const number = Number(text);
  return Number.isFinite(number) ? number : undefined;
}

function blankRule(conditions: ConditionOption[]): Partial<DietRule> {
  const first = conditions[0] ?? { conditionKey: "hypertension", conditionLabel: conditionNames.hypertension };
  return {
    conditionKey: first.conditionKey,
    conditionLabel: first.conditionLabel,
    comparator: "avoid",
    priority: 1,
    dataSource: dataSourceDefault,
    doctor_verified: false,
    foodTagsPrefer: [],
    foodTagsAvoid: [],
    cautionTags: []
  };
}

function slugifyConditionKey(value: string) {
  return value
    .trim()
    .toLowerCase()
    .replace(/[^a-z0-9]+/g, "_")
    .replace(/^_+|_+$/g, "");
}

export default function ConditionDietRulesManager() {
  const [items, setItems] = useState<DietRule[]>([]);
  const [conditions, setConditions] = useState<ConditionOption[]>([]);
  const [query, setQuery] = useState("");
  const [condition, setCondition] = useState("");
  const [mode, setMode] = useState<RuleMode>("all");
  const [editing, setEditing] = useState<Partial<DietRule> | null>(null);
  const [editingCondition, setEditingCondition] = useState<Partial<ConditionOption> | null>(null);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState("");

  async function load() {
    setLoading(true);
    setError("");
    try {
      const params = new URLSearchParams({ page: "1", limit: "200", sort: "priority" });
      if (query.trim()) params.set("q", query.trim());
      if (condition) params.set("conditionKey", condition);
      const data = await apiFetch<ApiList>(`/condition-diet-rules?${params.toString()}`);
      setItems(data.items);
    } catch (err) {
      setError(err instanceof Error ? err.message : "식사규칙을 불러오지 못했습니다.");
    } finally {
      setLoading(false);
    }
  }

  async function loadConditions() {
    const data = await apiFetch<ConditionApiList>("/condition-diet-conditions?page=1&limit=200&sort=sortOrder");
    setConditions(data.items);
  }

  useEffect(() => {
    load();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [condition]);

  useEffect(() => {
    loadConditions().catch((err) => setError(err instanceof Error ? err.message : "병명 목록을 불러오지 못했습니다."));
  }, []);

  const filtered = useMemo(() => items.filter((item) => visibleByMode(item, mode)), [items, mode]);
  const groups = useMemo(() => groupByCondition(filtered, conditions), [filtered, conditions]);
  const stats = useMemo(() => ({
    conditions: conditions.length || new Set(items.map((item) => item.conditionKey)).size,
    avoid: items.filter((item) => ruleMode(item) === "avoid").length,
    prefer: items.filter((item) => ruleMode(item) === "prefer").length,
    limit: items.filter((item) => ruleMode(item) === "limit").length
  }), [items]);

  async function submitRule(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (!editing) return;

    const formData = new FormData(event.currentTarget);
    const conditionKey = String(formData.get("conditionKey") ?? "").trim();
    const ruleType = String(formData.get("ruleType") ?? "").trim();
    const conditionLabel = conditionLabelFromOptions(conditions, conditionKey);
    const payload: Record<string, unknown> = {
      ruleKey: String(formData.get("ruleKey") ?? "").trim() || `${conditionKey}-${ruleType}`,
      conditionKey,
      conditionLabel,
      ruleType,
      priority: optionalNumber(formData.get("priority")) ?? 1,
      nutrientKey: String(formData.get("nutrientKey") ?? "").trim(),
      comparator: String(formData.get("comparator") ?? "avoid"),
      targetValue: optionalNumber(formData.get("targetValue")),
      targetMin: optionalNumber(formData.get("targetMin")),
      targetMax: optionalNumber(formData.get("targetMax")),
      unit: String(formData.get("unit") ?? "").trim(),
      recommendationKo: String(formData.get("recommendationKo") ?? "").trim(),
      foodTagsPrefer: parseTags(formData.get("foodTagsPrefer")),
      foodTagsAvoid: parseTags(formData.get("foodTagsAvoid")),
      cautionTags: parseTags(formData.get("cautionTags")),
      dataSource: String(formData.get("dataSource") ?? dataSourceDefault).trim() || dataSourceDefault,
      doctor_verified: formData.get("doctor_verified") === "true"
    };

    setSaving(true);
    setError("");
    try {
      const id = editing._id;
      await apiFetch<DietRule>(id ? `/condition-diet-rules/${id}` : "/condition-diet-rules", {
        method: id ? "PUT" : "POST",
        body: JSON.stringify(payload)
      });
      setEditing(null);
      await load();
    } catch (err) {
      setError(err instanceof Error ? err.message : "식사규칙 저장에 실패했습니다.");
    } finally {
      setSaving(false);
    }
  }

  async function submitCondition(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (!editingCondition) return;

    const formData = new FormData(event.currentTarget);
    const label = String(formData.get("conditionLabel") ?? "").trim();
    const conditionKey = String(formData.get("conditionKey") ?? "").trim() || slugifyConditionKey(label);
    const payload = {
      conditionKey,
      conditionLabel: label,
      category: "condition_diet",
      descriptionKo: String(formData.get("descriptionKo") ?? "").trim(),
      sortOrder: optionalNumber(formData.get("sortOrder")) ?? conditions.length + 1,
      dataSource: String(formData.get("dataSource") ?? dataSourceDefault).trim() || dataSourceDefault,
      tags: parseTags(formData.get("tags")),
      doctor_verified: formData.get("doctor_verified") === "true"
    };

    setSaving(true);
    setError("");
    try {
      const id = editingCondition._id;
      await apiFetch<ConditionOption>(id ? `/condition-diet-conditions/${id}` : "/condition-diet-conditions", {
        method: id ? "PUT" : "POST",
        body: JSON.stringify(payload)
      });
      setEditingCondition(null);
      await loadConditions();
    } catch (err) {
      setError(err instanceof Error ? err.message : "병명 저장에 실패했습니다.");
    } finally {
      setSaving(false);
    }
  }

  return (
    <section className="page condition-diet-page">
      <PageHeader
        title="병별 식사규칙"
        subtitle="병과 건강상태에 따라 제한할 음식, 권장할 음식, 영양소 기준값을 한눈에 점검합니다."
        action={(
          <div className="condition-page-actions">
            <button className="condition-add-button" type="button" onClick={() => setEditing(blankRule(conditions))}><Plus size={16} /> 새 규칙</button>
            <button className="condition-add-secondary" type="button" onClick={() => setEditingCondition({ sortOrder: conditions.length + 1, dataSource: dataSourceDefault, doctor_verified: false })}><Plus size={16} /> 병명 추가</button>
            <button className="condition-refresh-button" type="button" onClick={load} aria-label="새로고침"><RefreshCw size={16} /></button>
          </div>
        )}
      />

      <section className="condition-summary-strip">
        <div><HeartPulse size={18} /><strong>{stats.conditions}</strong><span>병/상태</span></div>
        <div><Ban size={18} /><strong>{stats.avoid}</strong><span>제한 규칙</span></div>
        <div><CheckCircle2 size={18} /><strong>{stats.prefer}</strong><span>권장 규칙</span></div>
        <div><SlidersHorizontal size={18} /><strong>{stats.limit}</strong><span>수치 기준</span></div>
      </section>

      <section className="condition-catalog-panel">
        <header>
          <div>
            <p>자료기지 병명</p>
            <h2>식사규칙에 쓸 병명을 먼저 등록합니다</h2>
          </div>
          <button type="button" onClick={() => setEditingCondition({ sortOrder: conditions.length + 1, dataSource: dataSourceDefault, doctor_verified: false })}>
            <Plus size={15} /> 병명 추가
          </button>
        </header>
        <div className="condition-catalog-list">
          {conditions.map((item) => (
            <button key={item.conditionKey} type="button" onClick={() => setEditingCondition(item)}>
              <span>{item.conditionLabel}</span>
              <small>{item.conditionKey}</small>
              <Pencil size={13} />
            </button>
          ))}
          {!conditions.length ? <span className="condition-catalog-empty">등록된 병명이 없습니다.</span> : null}
        </div>
      </section>

      <section className="panel">
        <div className="toolbar condition-toolbar">
          <label className="search-field">
            <Search size={16} />
            <input
              value={query}
              onChange={(event) => setQuery(event.target.value)}
              onKeyDown={(event) => event.key === "Enter" && load()}
              placeholder="병명, 영양소, 권고내용 검색"
            />
          </label>
          <button type="button" onClick={load}>검색</button>
          <select className="sort-select" value={condition} onChange={(event) => setCondition(event.target.value)}>
            <option value="">모든 병/상태</option>
            {conditions.map((option) => <option key={option.conditionKey} value={option.conditionKey}>{option.conditionLabel}</option>)}
          </select>
          <select className="sort-select" value={mode} onChange={(event) => setMode(event.target.value as RuleMode)}>
            {modeOptions.map((option) => <option key={option.value} value={option.value}>{option.label}</option>)}
          </select>
          <span>{loading ? "불러오는 중..." : `${filtered.length}개 규칙`}</span>
        </div>

        {error ? <p className="error">{error}</p> : null}

        <div className="condition-group-list">
          {groups.map((group) => (
            <section className="condition-group" key={group.conditionKey}>
              <header className="condition-group-head">
                <div>
                  <p>식사관리 대상</p>
                  <h2>{group.label}</h2>
                </div>
                <span>{group.rules.length}개 규칙</span>
              </header>

              <div className="condition-rule-grid">
                {group.rules.map((rule) => {
                  const currentMode = ruleMode(rule);
                  const Icon = modeIcon(currentMode);
                  return (
                    <article className={modeClass(currentMode)} key={rule._id}>
                      <header>
                        <span className="condition-rule-icon"><Icon size={17} /></span>
                        <div>
                          <p>{modeLabel(currentMode)}</p>
                          <h3>{targetText(rule)}</h3>
                        </div>
                        {rule.doctor_verified ? <span className="badge ok"><BadgeCheck size={14} /> 검증</span> : <span className="badge"><AlertTriangle size={14} /> 확인필요</span>}
                      </header>

                      <p className="condition-recommendation">{rule.recommendationKo}</p>

                      {rule.foodTagsAvoid?.length || rule.cautionTags?.length ? (
                        <div className="condition-tag-block danger">
                          <b><ShieldAlert size={14} /> 제한/주의</b>
                          <div>{[...(rule.foodTagsAvoid ?? []), ...(rule.cautionTags ?? [])].map((tag, index) => <span key={`${rule._id}-avoid-${tag}-${index}`}>{labelTag(tag)}</span>)}</div>
                        </div>
                      ) : null}

                      {rule.foodTagsPrefer?.length ? (
                        <div className="condition-tag-block good">
                          <b><CheckCircle2 size={14} /> 권장</b>
                          <div>{rule.foodTagsPrefer.map((tag, index) => <span key={`${rule._id}-prefer-${tag}-${index}`}>{labelTag(tag)}</span>)}</div>
                        </div>
                      ) : null}

                      <footer>
                        <div>
                          <span>{rule.ruleType.replaceAll("_", " ")}</span>
                          {rule.nutrientKey ? <span>{nutrientNames[rule.nutrientKey] ?? rule.nutrientKey}</span> : null}
                        </div>
                        <button className="condition-edit-button" type="button" onClick={() => setEditing(rule)} aria-label="식사규칙 수정"><Pencil size={14} /> 수정</button>
                      </footer>
                    </article>
                  );
                })}
              </div>
            </section>
          ))}

          {!loading && !groups.length ? <p className="empty-row">조건에 맞는 식사규칙이 없습니다.</p> : null}
        </div>
      </section>

      {editing ? (
        <Modal onClose={() => setEditing(null)}>
          <form className="editor condition-editor" onSubmit={submitRule}>
            <div className="modal-head">
              <h2>{editing._id ? "식사규칙 수정" : "새 식사규칙 추가"}</h2>
              <button type="button" onClick={() => setEditing(null)} aria-label="닫기"><X size={16} /></button>
            </div>

            <div className="editor-grid">
              <label>
                병/상태
                <select name="conditionKey" defaultValue={editing.conditionKey ?? conditions[0]?.conditionKey ?? "hypertension"} required>
                  {conditions.map((item) => <option key={item.conditionKey} value={item.conditionKey}>{item.conditionLabel}</option>)}
                </select>
              </label>

              <label>
                규칙 key
                <input name="ruleKey" defaultValue={editing.ruleKey ?? ""} placeholder="비우면 병명-규칙종류로 자동 생성" />
              </label>

              <label>
                규칙 종류
                <input name="ruleType" defaultValue={editing.ruleType ?? ""} placeholder="예: sodium_limit, alcohol_limit" required />
              </label>

              <label>
                구분
                <select name="comparator" defaultValue={editing.comparator ?? "avoid"}>
                  {comparatorOptions.map((option) => <option key={option.value} value={option.value}>{option.label}</option>)}
                </select>
              </label>

              <label>
                영양소
                <input name="nutrientKey" defaultValue={editing.nutrientKey ?? ""} placeholder="예: sodium, addedSugar, protein" />
              </label>

              <label>
                단위
                <input name="unit" defaultValue={editing.unit ?? ""} placeholder="예: mg/d, %E, g/kg/d" />
              </label>

              <label>
                목표값
                <input name="targetValue" type="number" step="any" defaultValue={editing.targetValue ?? ""} />
              </label>

              <label>
                우선순위
                <input name="priority" type="number" defaultValue={editing.priority ?? 1} />
              </label>

              <label>
                최소값
                <input name="targetMin" type="number" step="any" defaultValue={editing.targetMin ?? ""} />
              </label>

              <label>
                최대값
                <input name="targetMax" type="number" step="any" defaultValue={editing.targetMax ?? ""} />
              </label>

              <label className="wide-field">
                권고 내용
                <textarea name="recommendationKo" defaultValue={editing.recommendationKo ?? ""} placeholder="사용자가 이해할 수 있는 식사관리 문장" required />
              </label>

              <label className="wide-field">
                권장 음식태그
                <input name="foodTagsPrefer" defaultValue={tagsToText(editing.foodTagsPrefer)} placeholder="예: vegetable, whole_grain, fish" />
              </label>

              <label className="wide-field">
                제한 음식태그
                <input name="foodTagsAvoid" defaultValue={tagsToText(editing.foodTagsAvoid)} placeholder="예: alcohol, high_sodium, fried_food" />
              </label>

              <label className="wide-field">
                주의 태그
                <input name="cautionTags" defaultValue={tagsToText(editing.cautionTags)} placeholder="제한 태그와 같게 두어도 됩니다." />
              </label>

              <label>
                의사 검증
                <select name="doctor_verified" defaultValue={String(Boolean(editing.doctor_verified))}>
                  <option value="false">미검증</option>
                  <option value="true">검증</option>
                </select>
              </label>

              <label>
                자료 출처
                <input name="dataSource" defaultValue={editing.dataSource ?? dataSourceDefault} required />
              </label>
            </div>

            <div className="editor-actions">
              <button type="button" onClick={() => setEditing(null)}>취소</button>
              <button className="primary" type="submit" disabled={saving}>{saving ? "저장 중..." : "저장"}</button>
            </div>
          </form>
        </Modal>
      ) : null}

      {editingCondition ? (
        <Modal onClose={() => setEditingCondition(null)}>
          <form className="editor condition-editor" onSubmit={submitCondition}>
            <div className="modal-head">
              <h2>{editingCondition._id ? "병명 수정" : "새 병명 추가"}</h2>
              <button type="button" onClick={() => setEditingCondition(null)} aria-label="닫기"><X size={16} /></button>
            </div>

            <div className="editor-grid">
              <label>
                병명
                <input name="conditionLabel" defaultValue={editingCondition.conditionLabel ?? ""} placeholder="예: 간질환, 위염, 빈혈" required />
              </label>

              <label>
                병명 key
                <input name="conditionKey" defaultValue={editingCondition.conditionKey ?? ""} placeholder="비우면 병명에서 자동 생성" />
              </label>

              <label>
                표시 순서
                <input name="sortOrder" type="number" defaultValue={editingCondition.sortOrder ?? conditions.length + 1} />
              </label>

              <label>
                의사 검증
                <select name="doctor_verified" defaultValue={String(Boolean(editingCondition.doctor_verified))}>
                  <option value="false">미검증</option>
                  <option value="true">검증</option>
                </select>
              </label>

              <label className="wide-field">
                설명
                <textarea name="descriptionKo" defaultValue={editingCondition.descriptionKo ?? ""} placeholder="이 병명에 대한 식사관리 목적을 적습니다." />
              </label>

              <label className="wide-field">
                태그
                <input name="tags" defaultValue={tagsToText((editingCondition as { tags?: string[] }).tags)} placeholder="예: liver, condition_diet" />
              </label>

              <label className="wide-field">
                자료 출처
                <input name="dataSource" defaultValue={editingCondition.dataSource ?? dataSourceDefault} required />
              </label>
            </div>

            <div className="editor-actions">
              <button type="button" onClick={() => setEditingCondition(null)}>취소</button>
              <button className="primary" type="submit" disabled={saving}>{saving ? "저장 중..." : "저장"}</button>
            </div>
          </form>
        </Modal>
      ) : null}
    </section>
  );
}
