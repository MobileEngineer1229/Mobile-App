"use client";

import { BadgeCheck, ClipboardCheck, Pencil, Plus, RefreshCw, Search, ShieldAlert, Target, Users, X } from "lucide-react";
import { FormEvent, useEffect, useMemo, useState } from "react";
import Modal from "@/components/Modal";
import PageHeader from "@/components/PageHeader";
import { apiFetch } from "@/lib/api";

type NutrientRule = {
  _id?: string;
  ruleKey?: string;
  standardCode: string;
  nutrientKey: string;
  nutrientLabel: string;
  referenceType: string;
  unit: string;
  rawValue: string;
  comparator?: "eq" | "lt" | "lte" | "gt" | "gte" | "range";
  value?: number;
  valueMin?: number;
  valueMax?: number;
  ageGroup: string;
  ageMin?: number | null;
  ageMax?: number | null;
  gender?: "all" | "male" | "female";
  lifeStage?: string;
  populationGroup?: string;
  physicalActivityLevel?: string;
  dataSource: string;
  sourceNote?: string;
  sourceRefs?: string[];
  tags?: string[];
  doctor_verified?: boolean;
};

type ApiList = {
  items: NutrientRule[];
  total: number;
  page: number;
  limit: number;
};

type NutrientSummary = {
  key: string;
  label: string;
  count: number;
  verified: number;
  referenceTypes: string[];
};

const referenceLabels: Record<string, string> = {
  EER: "에네르기 필요량",
  EAR: "평균필요량",
  RNI: "권장섭취량",
  AI: "충분섭취량",
  UL: "상한섭취량",
  AMDR: "에네르기비률",
  PI: "예방섭취량",
  SPL: "특정제한량"
};

const referenceTone: Record<string, string> = {
  RNI: "good",
  AI: "good",
  EER: "energy",
  EAR: "base",
  UL: "danger",
  AMDR: "range"
};

const populationLabels: Record<string, string> = {
  general: "일반",
  infant: "젖먹이",
  child: "어린이",
  adolescent: "청소년",
  adult: "성인",
  senior: "로인",
  pregnant: "임신부",
  lactating: "젖먹이는 녀성"
};

const genderLabels: Record<string, string> = {
  all: "전체",
  male: "남성",
  female: "녀성"
};

const lifeStageLabels: Record<string, string> = {
  general: "일반",
  pregnant: "임신",
  lactating: "수유"
};

const nutrientKoLabels: Record<string, string> = {
  biotin: "비오틴",
  calcium: "칼시움",
  carbs: "탄수화물",
  chloride: "염소",
  choline: "콜린",
  chromium: "크롬",
  copper: "동",
  energyKcal: "에네르기",
  fat: "지방",
  folate: "엽산",
  iodine: "요드",
  iron: "철분",
  magnesium: "마그네시움",
  molybdenum: "몰리브덴",
  niacin: "나이아신",
  niacinamide: "나이아신아미드",
  pantothenicAcid: "판토텐산",
  potassium: "칼리움",
  protein: "단백질",
  selenium: "셀렌",
  sodium: "나트리움",
  vitaminA: "비타민 A",
  vitaminB1: "비타민 B1",
  vitaminB2: "비타민 B2",
  vitaminB6: "비타민 B6",
  vitaminB12: "비타민 B12",
  vitaminC: "비타민 C",
  vitaminD: "비타민 D",
  vitaminE: "비타민 E",
  vitaminK: "비타민 K",
  zinc: "아연",
  n3PolyunsaturatedFattyAcid: "n-3 다가불포화지방산",
  n6PolyunsaturatedFattyAcid: "n-6 다가불포화지방산"
};

const defaultSource = "Chinese Dietary Reference Intakes WST578";

function normalizeUnit(unit: string) {
  return unit.replace("?g", "ug");
}

function formatNumber(value?: number | null) {
  if (value == null || Number.isNaN(value)) return "";
  return Number.isInteger(value) ? String(value) : String(value);
}

function valueText(rule: NutrientRule) {
  const unit = normalizeUnit(rule.unit);
  if (rule.comparator === "range") return `${formatNumber(rule.valueMin)}-${formatNumber(rule.valueMax)} ${unit}`.trim();
  if (rule.comparator === "lt") return `< ${formatNumber(rule.value)} ${unit}`.trim();
  if (rule.comparator === "lte") return `<= ${formatNumber(rule.value)} ${unit}`.trim();
  if (rule.comparator === "gt") return `> ${formatNumber(rule.value)} ${unit}`.trim();
  if (rule.comparator === "gte") return `>= ${formatNumber(rule.value)} ${unit}`.trim();
  return `${rule.rawValue || formatNumber(rule.value)} ${unit}`.trim();
}

function ageText(rule: NutrientRule) {
  if (rule.ageGroup) return rule.ageGroup.replace("～", "세부터");
  if (rule.ageMin != null && rule.ageMax != null) return `${rule.ageMin}-${rule.ageMax}세`;
  if (rule.ageMin != null) return `${rule.ageMin}세부터`;
  if (rule.ageMax != null) return `${rule.ageMax}세까지`;
  return "전체";
}

function splitTags(value: FormDataEntryValue | null) {
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

function blankRule(selectedNutrient: string, summaries: NutrientSummary[]): Partial<NutrientRule> {
  const nutrient = summaries.find((item) => item.key === selectedNutrient) ?? summaries[0];
  return {
    standardCode: "WST578",
    nutrientKey: nutrient?.key ?? "",
    nutrientLabel: nutrient?.label ?? "",
    referenceType: "RNI",
    unit: "mg/d",
    rawValue: "",
    comparator: "eq",
    ageGroup: "",
    gender: "all",
    lifeStage: "general",
    populationGroup: "general",
    dataSource: defaultSource,
    sourceRefs: [],
    tags: ["WST578", "DRI"],
    doctor_verified: false
  };
}

function summarizeNutrients(items: NutrientRule[]) {
  const map = new Map<string, NutrientSummary>();
  for (const item of items) {
    const current = map.get(item.nutrientKey) ?? {
      key: item.nutrientKey,
      label: item.nutrientLabel,
      count: 0,
      verified: 0,
      referenceTypes: []
    };
    current.count += 1;
    if (item.doctor_verified) current.verified += 1;
    if (!current.referenceTypes.includes(item.referenceType)) current.referenceTypes.push(item.referenceType);
    map.set(item.nutrientKey, current);
  }
  return [...map.values()].sort((a, b) => a.label.localeCompare(b.label));
}

function uniqueOptions(items: NutrientRule[], key: keyof NutrientRule) {
  return [...new Set(items.map((item) => String(item[key] ?? "")).filter(Boolean))].sort();
}

function nutrientDisplayName(item?: Pick<NutrientSummary, "key" | "label">) {
  if (!item) return "영양소를 선택하십시오";
  return nutrientKoLabels[item.key] ?? item.label;
}

type NutrientIntakeRulesManagerProps = {
  embedded?: boolean;
};

export default function NutrientIntakeRulesManager({ embedded = false }: NutrientIntakeRulesManagerProps) {
  const [items, setItems] = useState<NutrientRule[]>([]);
  const [selectedNutrient, setSelectedNutrient] = useState("");
  const [nutrientQuery, setNutrientQuery] = useState("");
  const [referenceType, setReferenceType] = useState("");
  const [populationGroup, setPopulationGroup] = useState("");
  const [gender, setGender] = useState("");
  const [lifeStage, setLifeStage] = useState("");
  const [editing, setEditing] = useState<Partial<NutrientRule> | null>(null);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState("");

  async function load() {
    setLoading(true);
    setError("");
    try {
      const first = await apiFetch<ApiList>("/nutrient-intake-rules?page=1&limit=500&sort=nutrientKey");
      const pages = Math.ceil(first.total / first.limit);
      const rest = await Promise.all(
        Array.from({ length: Math.max(pages - 1, 0) }, (_, index) =>
          apiFetch<ApiList>(`/nutrient-intake-rules?page=${index + 2}&limit=500&sort=nutrientKey`)
        )
      );
      const all = [first, ...rest].flatMap((page) => page.items);
      setItems(all);
      setSelectedNutrient((current) => current || (all.find((item) => item.nutrientKey === "energyKcal")?.nutrientKey ?? all[0]?.nutrientKey ?? ""));
    } catch (err) {
      setError(err instanceof Error ? err.message : "영양섭취기준을 불러오지 못했습니다.");
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => {
    load();
  }, []);

  const summaries = useMemo(() => summarizeNutrients(items), [items]);
  const nutrientOptions = useMemo(() => (
    summaries.filter((item) => {
      const q = nutrientQuery.trim().toLowerCase();
      const koLabel = nutrientDisplayName(item).toLowerCase();
      return !q || item.key.toLowerCase().includes(q) || item.label.toLowerCase().includes(q) || koLabel.includes(q);
    })
  ), [summaries, nutrientQuery]);

  const selectedSummary = summaries.find((item) => item.key === selectedNutrient);
  const selectedItems = useMemo(() => items
    .filter((item) => item.nutrientKey === selectedNutrient)
    .filter((item) => !referenceType || item.referenceType === referenceType)
    .filter((item) => !populationGroup || item.populationGroup === populationGroup)
    .filter((item) => !gender || item.gender === gender)
    .filter((item) => !lifeStage || item.lifeStage === lifeStage)
    .sort((a, b) => (a.ageMin ?? 999) - (b.ageMin ?? 999) || String(a.gender).localeCompare(String(b.gender)) || a.referenceType.localeCompare(b.referenceType)),
  [items, selectedNutrient, referenceType, populationGroup, gender, lifeStage]);

  const grouped = useMemo(() => {
    const map = new Map<string, NutrientRule[]>();
    for (const item of selectedItems) map.set(item.referenceType, [...(map.get(item.referenceType) ?? []), item]);
    return [...map.entries()].sort(([a], [b]) => a.localeCompare(b));
  }, [selectedItems]);

  const referenceOptions = uniqueOptions(items.filter((item) => item.nutrientKey === selectedNutrient), "referenceType");
  const populationOptions = uniqueOptions(items.filter((item) => item.nutrientKey === selectedNutrient), "populationGroup");
  const genderOptions = uniqueOptions(items.filter((item) => item.nutrientKey === selectedNutrient), "gender");
  const lifeStageOptions = uniqueOptions(items.filter((item) => item.nutrientKey === selectedNutrient), "lifeStage");
  const verifiedCount = items.filter((item) => item.doctor_verified).length;

  async function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (!editing) return;

    const formData = new FormData(event.currentTarget);
    const nutrientKey = String(formData.get("nutrientKey") ?? "").trim();
    const referenceTypeValue = String(formData.get("referenceType") ?? "").trim();
    const ageGroup = String(formData.get("ageGroup") ?? "").trim();
    const genderValue = String(formData.get("gender") ?? "all").trim();
    const payload: Record<string, unknown> = {
      ruleKey: String(formData.get("ruleKey") ?? "").trim() || `wst578-${nutrientKey}-${referenceTypeValue}-${ageGroup}-${genderValue}`.replaceAll(" ", "-"),
      standardCode: String(formData.get("standardCode") ?? "WST578").trim(),
      nutrientKey,
      nutrientLabel: String(formData.get("nutrientLabel") ?? "").trim(),
      referenceType: referenceTypeValue,
      unit: String(formData.get("unit") ?? "").trim(),
      rawValue: String(formData.get("rawValue") ?? "").trim(),
      comparator: String(formData.get("comparator") ?? "eq"),
      value: optionalNumber(formData.get("value")),
      valueMin: optionalNumber(formData.get("valueMin")),
      valueMax: optionalNumber(formData.get("valueMax")),
      ageGroup,
      ageMin: optionalNumber(formData.get("ageMin")),
      ageMax: optionalNumber(formData.get("ageMax")),
      gender: genderValue,
      lifeStage: String(formData.get("lifeStage") ?? "general").trim() || "general",
      populationGroup: String(formData.get("populationGroup") ?? "general").trim() || "general",
      physicalActivityLevel: String(formData.get("physicalActivityLevel") ?? "").trim(),
      dataSource: String(formData.get("dataSource") ?? defaultSource).trim() || defaultSource,
      sourceNote: String(formData.get("sourceNote") ?? "").trim(),
      sourceRefs: splitTags(formData.get("sourceRefs")),
      tags: splitTags(formData.get("tags")),
      doctor_verified: formData.get("doctor_verified") === "true"
    };

    setSaving(true);
    setError("");
    try {
      const id = editing._id;
      await apiFetch<NutrientRule>(id ? `/nutrient-intake-rules/${id}` : "/nutrient-intake-rules", {
        method: id ? "PUT" : "POST",
        body: JSON.stringify(payload)
      });
      setEditing(null);
      await load();
      setSelectedNutrient(nutrientKey);
    } catch (err) {
      setError(err instanceof Error ? err.message : "영양섭취기준 저장에 실패했습니다.");
    } finally {
      setSaving(false);
    }
  }

  return (
    <section className={embedded ? "nutrient-rules-page is-embedded" : "page nutrient-rules-page"}>
      {embedded ? null : (
        <PageHeader
          title="영양소 섭취기준"
          subtitle="나이, 성별, 임신/수유, 활동수준에 따르는 에네르기와 영양소 기준을 한눈에 봅니다."
          action={(
            <div className="nutrient-actions">
              <button className="primary" type="button" onClick={() => setEditing(blankRule(selectedNutrient, summaries))}><Plus size={16} /> 새 기준</button>
              <button type="button" onClick={load} aria-label="새로고침"><RefreshCw size={16} /></button>
            </div>
          )}
        />
      )}

      {embedded ? (
        <div className="nutrient-embedded-head">
          <div>
            <p>WST578 기준표</p>
            <h2>나이와 성별에 따르는 영양소 섭취기준</h2>
          </div>
          <div className="nutrient-actions">
            <button className="primary" type="button" onClick={() => setEditing(blankRule(selectedNutrient, summaries))}><Plus size={16} /> 새 기준</button>
            <button type="button" onClick={load} aria-label="새로고침"><RefreshCw size={16} /></button>
          </div>
        </div>
      ) : null}

      <section className="nutrient-summary-strip">
        <div><ClipboardCheck size={18} /><strong>{items.length}</strong><span>전체 기준</span></div>
        <div><Target size={18} /><strong>{summaries.length}</strong><span>영양소</span></div>
        <div><BadgeCheck size={18} /><strong>{verifiedCount}</strong><span>검증됨</span></div>
        <div><Users size={18} /><strong>{selectedSummary?.count ?? 0}</strong><span>선택 영양소</span></div>
      </section>

      {error ? <p className="error">{error}</p> : null}

      <section className="nutrient-layout">
        <aside className="nutrient-sidebar-panel">
          <div className="nutrient-sidebar-title">
            <div>
              <p>영양소 목록</p>
              <strong>{nutrientOptions.length}개</strong>
            </div>
            <span>{loading ? "..." : `${summaries.length}`}</span>
          </div>
          <label className="search-field">
            <Search size={16} />
            <input value={nutrientQuery} onChange={(event) => setNutrientQuery(event.target.value)} placeholder="영양소 검색" />
          </label>
          <div className="nutrient-list">
            {nutrientOptions.map((item) => (
              <button
                key={item.key}
                type="button"
                className={item.key === selectedNutrient ? "active" : ""}
                onClick={() => {
                  setSelectedNutrient(item.key);
                  setReferenceType("");
                  setPopulationGroup("");
                  setGender("");
                  setLifeStage("");
                }}
              >
                <div className="nutrient-list-main">
                  <span>{nutrientDisplayName(item)}</span>
                  <b>{item.count}</b>
                </div>
                <small>{item.label}</small>
                <div className="nutrient-ref-chips">
                  {item.referenceTypes.map((type) => <i key={`${item.key}-${type}`}>{type}</i>)}
                </div>
              </button>
            ))}
            {!nutrientOptions.length ? <p className="nutrient-list-empty">검색된 영양소가 없습니다.</p> : null}
          </div>
        </aside>

        <section className="nutrient-main-panel">
          <div className="nutrient-selected-head">
            <div>
              <p>{selectedSummary?.key ?? "nutrient"}</p>
              <h2>{nutrientDisplayName(selectedSummary)}</h2>
            </div>
            <span>{loading ? "불러오는 중..." : `${selectedItems.length}개 기준 표시`}</span>
          </div>

          <div className="nutrient-filter-row">
            <select value={referenceType} onChange={(event) => setReferenceType(event.target.value)} aria-label="기준종류">
              <option value="">모든 기준종류</option>
              {referenceOptions.map((value) => <option key={value} value={value}>{referenceLabels[value] ?? value}</option>)}
            </select>
            <select value={populationGroup} onChange={(event) => setPopulationGroup(event.target.value)} aria-label="대상">
              <option value="">모든 대상</option>
              {populationOptions.map((value) => <option key={value} value={value}>{populationLabels[value] ?? value}</option>)}
            </select>
            <select value={gender} onChange={(event) => setGender(event.target.value)} aria-label="성별">
              <option value="">모든 성별</option>
              {genderOptions.map((value) => <option key={value} value={value}>{genderLabels[value] ?? value}</option>)}
            </select>
            <select value={lifeStage} onChange={(event) => setLifeStage(event.target.value)} aria-label="생리단계">
              <option value="">모든 단계</option>
              {lifeStageOptions.map((value) => <option key={value} value={value}>{lifeStageLabels[value] ?? value}</option>)}
            </select>
          </div>

          <div className="nutrient-reference-stack">
            {grouped.map(([type, rules]) => (
              <article className={`nutrient-reference-card is-${referenceTone[type] ?? "base"}`} key={type}>
                <header>
                  <div>
                    <p>{type}</p>
                    <h3>{referenceLabels[type] ?? type}</h3>
                  </div>
                  <span>{rules.length}개</span>
                </header>
                <div className="nutrient-rule-table-wrap">
                  <table className="nutrient-rule-table">
                    <thead>
                      <tr>
                        <th>나이</th>
                        <th>대상</th>
                        <th>성별</th>
                        <th>단계/활동</th>
                        <th>기준값</th>
                        <th>상태</th>
                        <th>편집</th>
                      </tr>
                    </thead>
                    <tbody>
                      {rules.map((rule) => (
                        <tr key={rule._id ?? rule.ruleKey}>
                          <td>{ageText(rule)}</td>
                          <td>{populationLabels[rule.populationGroup ?? ""] ?? rule.populationGroup ?? "일반"}</td>
                          <td>{genderLabels[rule.gender ?? "all"] ?? rule.gender}</td>
                          <td>{lifeStageLabels[rule.lifeStage ?? "general"] ?? rule.lifeStage}{rule.physicalActivityLevel ? ` · ${rule.physicalActivityLevel}` : ""}</td>
                          <td><strong>{valueText(rule)}</strong></td>
                          <td>{rule.doctor_verified ? <span className="badge ok"><BadgeCheck size={14} /> 검증</span> : <span className="badge"><ShieldAlert size={14} /> 확인필요</span>}</td>
                          <td><button type="button" className="nutrient-edit-button" onClick={() => setEditing(rule)} aria-label="섭취기준 수정"><Pencil size={14} /> 수정</button></td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
              </article>
            ))}

            {!loading && !grouped.length ? <p className="empty-row">조건에 맞는 영양섭취기준이 없습니다.</p> : null}
          </div>
        </section>
      </section>

      {editing ? (
        <Modal onClose={() => setEditing(null)}>
          <form className="editor nutrient-editor" onSubmit={submit}>
            <div className="modal-head">
              <h2>{editing._id ? "섭취기준 수정" : "새 섭취기준 추가"}</h2>
              <button type="button" onClick={() => setEditing(null)} aria-label="닫기"><X size={16} /></button>
            </div>
            <div className="editor-grid">
              <label>규칙 key<input name="ruleKey" defaultValue={editing.ruleKey ?? ""} placeholder="비우면 자동 생성" /></label>
              <label>표준코드<input name="standardCode" defaultValue={editing.standardCode ?? "WST578"} required /></label>
              <label>영양소 key<input name="nutrientKey" defaultValue={editing.nutrientKey ?? ""} required /></label>
              <label>영양소 이름<input name="nutrientLabel" defaultValue={editing.nutrientLabel ?? ""} required /></label>
              <label>기준종류<input name="referenceType" defaultValue={editing.referenceType ?? "RNI"} required /></label>
              <label>단위<input name="unit" defaultValue={editing.unit ?? ""} required /></label>
              <label>원자료값<input name="rawValue" defaultValue={editing.rawValue ?? ""} required /></label>
              <label>비교식
                <select name="comparator" defaultValue={editing.comparator ?? "eq"}>
                  <option value="eq">같음</option><option value="lt">미만</option><option value="lte">이하</option>
                  <option value="gt">초과</option><option value="gte">이상</option><option value="range">범위</option>
                </select>
              </label>
              <label>값<input name="value" type="number" step="any" defaultValue={editing.value ?? ""} /></label>
              <label>최소값<input name="valueMin" type="number" step="any" defaultValue={editing.valueMin ?? ""} /></label>
              <label>최대값<input name="valueMax" type="number" step="any" defaultValue={editing.valueMax ?? ""} /></label>
              <label>나이구간<input name="ageGroup" defaultValue={editing.ageGroup ?? ""} required /></label>
              <label>나이 최소<input name="ageMin" type="number" step="any" defaultValue={editing.ageMin ?? ""} /></label>
              <label>나이 최대<input name="ageMax" type="number" step="any" defaultValue={editing.ageMax ?? ""} /></label>
              <label>성별
                <select name="gender" defaultValue={editing.gender ?? "all"}>
                  <option value="all">전체</option><option value="male">남성</option><option value="female">녀성</option>
                </select>
              </label>
              <label>생리단계<input name="lifeStage" defaultValue={editing.lifeStage ?? "general"} /></label>
              <label>대상<input name="populationGroup" defaultValue={editing.populationGroup ?? "general"} /></label>
              <label>활동수준<input name="physicalActivityLevel" defaultValue={editing.physicalActivityLevel ?? ""} /></label>
              <label>의사 검증
                <select name="doctor_verified" defaultValue={String(Boolean(editing.doctor_verified))}>
                  <option value="false">미검증</option><option value="true">검증</option>
                </select>
              </label>
              <label className="wide-field">자료 출처<input name="dataSource" defaultValue={editing.dataSource ?? defaultSource} required /></label>
              <label className="wide-field">출처 설명<textarea name="sourceNote" defaultValue={editing.sourceNote ?? ""} /></label>
              <label className="wide-field">출처 파일<input name="sourceRefs" defaultValue={(editing.sourceRefs ?? []).join(", ")} /></label>
              <label className="wide-field">태그<input name="tags" defaultValue={(editing.tags ?? []).join(", ")} /></label>
            </div>
            <div className="editor-actions">
              <button type="button" onClick={() => setEditing(null)}>취소</button>
              <button className="primary" type="submit" disabled={saving}>{saving ? "저장 중..." : "저장"}</button>
            </div>
          </form>
        </Modal>
      ) : null}
    </section>
  );
}
