"use client";

import { Activity, BadgeCheck, Calculator, ClipboardCheck, Ruler, Search, Target, Utensils } from "lucide-react";
import { useEffect, useMemo, useState } from "react";
import PageHeader from "@/components/PageHeader";
import NutrientIntakeRulesManager from "@/components/NutrientIntakeRulesManager";
import { apiFetch } from "@/lib/api";

type DailyProfile = {
  _id: string;
  profileKey: string;
  label: string;
  ageMin: number;
  ageMax: number;
  gender?: string;
  purpose: string;
  notes?: string;
  values?: Record<string, number>;
  doctor_verified?: boolean;
};

type NutritionConstraint = {
  _id: string;
  profileKey: string;
  nutrientKey: string;
  nutrientLabel: string;
  unit?: string;
  lowerBound?: number;
  upperBound?: number;
  isPercentOfCalories?: boolean;
  caloriesPerGram?: number;
  doctor_verified?: boolean;
};

type NutrientTarget = {
  nutrientKey: string;
  nutrientLabel: string;
  unit: string;
  goalType?: "RNI" | "AI" | "EER" | "EAR";
  EAR?: number;
  RNI?: number;
  AI?: number;
  UL?: number;
  EER?: number;
  AMDR?: { value?: number; min?: number; max?: number; unit: string };
  units?: Partial<Record<"EAR" | "RNI" | "AI" | "UL" | "EER" | "AMDR", string>>;
  sourceRuleKeys?: string[];
};

type ApiList<T> = {
  items: T[];
  total: number;
  page: number;
  limit: number;
};

type TargetResponse = {
  requestedProfile?: DailyTargetProfile;
  profile?: DailyTargetProfile;
  warnings?: DailyTargetWarning[];
  count: number;
  targets: NutrientTarget[];
  goals: Record<string, number | null>;
};

type DailyTab = "needed" | "rules" | "constraints";

type DailyTargetProfile = {
  age: number;
  gender: string;
  lifeStage?: string;
  populationGroup?: string;
  physicalActivityLevel?: string;
};

type DailyTargetWarning = {
  code: string;
  message: string;
};

const nutrientLabels: Record<string, string> = {
  calories: "에네르기",
  energyKcal: "에네르기",
  protein: "단백질",
  carbs: "탄수화물",
  carbohydrateG: "탄수화물",
  fat: "지방",
  saturatedFat: "포화지방",
  fiber: "식이섬유",
  dietaryFiberG: "식이섬유",
  sugar: "당",
  sodium: "나트리움",
  calcium: "칼시움",
  iron: "철분",
  potassium: "칼리움",
  magnesium: "마그네시움",
  zinc: "아연",
  folate: "엽산",
  vitaminC: "비타민 C",
  vitaminD: "비타민 D"
};

const highlightKeys = ["calories", "protein", "carbs", "fat", "fiber", "sodium", "calcium", "iron"];

function formatValue(value?: number | null, unit = "") {
  if (value == null || Number.isNaN(value)) return "-";
  return `${Number.isInteger(value) ? value : Number(value.toFixed(2))}${unit ? ` ${unit}` : ""}`;
}

function targetGoal(target: NutrientTarget) {
  return target.RNI ?? target.AI ?? target.EER ?? target.EAR;
}

function targetUnit(unit: string) {
  return unit.replace("?g", "ug");
}

function refUnit(target: NutrientTarget, type: keyof NonNullable<NutrientTarget["units"]>) {
  return targetUnit(target.units?.[type] ?? target.unit);
}

function goalLabel(type?: string) {
  if (type === "RNI") return "권장섭취량";
  if (type === "AI") return "충분섭취량";
  if (type === "EER") return "에네르기 필요량";
  if (type === "EAR") return "평균필요량";
  return "기준값";
}

function amdrText(target: NutrientTarget) {
  if (!target.AMDR) return "";
  if (target.AMDR.value != null) return formatValue(target.AMDR.value, targetUnit(target.AMDR.unit));
  const min = target.AMDR.min != null ? target.AMDR.min : "";
  const max = target.AMDR.max != null ? target.AMDR.max : "";
  return `${min}-${max} ${targetUnit(target.AMDR.unit)}`;
}

function populationFromAge(age: number) {
  if (age < 1) return "infant";
  if (age < 11) return "child";
  if (age < 18) return "adolescent";
  if (age >= 65) return "senior";
  return "adult";
}

function isPregnancyOrLactation(stage: string) {
  return stage.startsWith("pregnancy") || stage === "lactation" || stage === "pregnant" || stage === "lactating";
}

function populationForStage(stage: string) {
  if (stage.startsWith("pregnancy") || stage === "pregnant") return "pregnant";
  if (stage === "lactation" || stage === "lactating") return "lactating";
  return "";
}

function ageLabel(profile: DailyProfile) {
  return `${profile.ageMin}-${profile.ageMax}세`;
}

export default function DailyNutritionManager() {
  const [tab, setTab] = useState<DailyTab>("needed");
  const [profiles, setProfiles] = useState<DailyProfile[]>([]);
  const [constraints, setConstraints] = useState<NutritionConstraint[]>([]);
  const [targets, setTargets] = useState<NutrientTarget[]>([]);
  const [profileQuery, setProfileQuery] = useState("");
  const [constraintQuery, setConstraintQuery] = useState("");
  const [age, setAge] = useState(30);
  const [gender, setGender] = useState("all");
  const [populationGroup, setPopulationGroup] = useState("adult");
  const [lifeStage, setLifeStage] = useState("general");
  const [pal, setPal] = useState("moderate");
  const [loading, setLoading] = useState(true);
  const [targetLoading, setTargetLoading] = useState(false);
  const [targetWarnings, setTargetWarnings] = useState<DailyTargetWarning[]>([]);
  const [error, setError] = useState("");

  async function loadBasics() {
    setLoading(true);
    setError("");
    try {
      const [profileData, constraintData] = await Promise.all([
        apiFetch<ApiList<DailyProfile>>("/daily-value-profiles?page=1&limit=100&sort=profileKey"),
        apiFetch<ApiList<NutritionConstraint>>("/nutrition-constraints?page=1&limit=100&sort=profileKey")
      ]);
      setProfiles(profileData.items);
      setConstraints(constraintData.items);
    } catch (err) {
      setError(err instanceof Error ? err.message : "하루 영양자료를 불러오지 못했습니다.");
    } finally {
      setLoading(false);
    }
  }

  async function loadTargets() {
    setTargetLoading(true);
    setError("");
    try {
      const data = await apiFetch<TargetResponse>("/daily-targets/resolve", {
        method: "POST",
        body: JSON.stringify({ age, gender, populationGroup, lifeStage, physicalActivityLevel: pal })
      });
      setTargets(data.targets);
      setTargetWarnings(data.warnings ?? []);
      if (data.profile) {
        setAge(data.profile.age);
        setGender(data.profile.gender);
        setPopulationGroup(data.profile.populationGroup ?? populationFromAge(data.profile.age));
        setLifeStage(data.profile.lifeStage ?? "general");
        setPal(data.profile.physicalActivityLevel ?? "moderate");
      }
    } catch (err) {
      setError(err instanceof Error ? err.message : "하루 필요량 계산에 실패했습니다.");
    } finally {
      setTargetLoading(false);
    }
  }

  useEffect(() => {
    loadBasics();
    loadTargets();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  const filteredProfiles = useMemo(() => {
    const q = profileQuery.trim().toLowerCase();
    return profiles.filter((profile) => !q || profile.label.toLowerCase().includes(q) || profile.purpose.toLowerCase().includes(q) || profile.profileKey.toLowerCase().includes(q));
  }, [profiles, profileQuery]);

  const filteredConstraints = useMemo(() => {
    const q = constraintQuery.trim().toLowerCase();
    return constraints.filter((item) => !q || item.profileKey.toLowerCase().includes(q) || item.nutrientKey.toLowerCase().includes(q) || item.nutrientLabel.toLowerCase().includes(q));
  }, [constraints, constraintQuery]);

  const topTargets = useMemo(() => {
    const priority = ["energyKcal", "protein", "carbs", "fat", "calcium", "iron", "sodium", "folate", "vitaminC", "vitaminD"];
    const rank = (key: string) => {
      const index = priority.indexOf(key);
      return index === -1 ? 999 : index;
    };
    return [...targets].sort((a, b) => rank(a.nutrientKey) - rank(b.nutrientKey) || a.nutrientKey.localeCompare(b.nutrientKey)).slice(0, 12);
  }, [targets]);

  const verifiedProfiles = profiles.filter((profile) => profile.doctor_verified).length;
  const verifiedConstraints = constraints.filter((item) => item.doctor_verified).length;
  const expectedPopulation = populationFromAge(age);
  const stagePopulation = populationForStage(lifeStage);
  const profileWarnings = [
    populationGroup !== expectedPopulation ? `나이 ${age}세에는 보통 '${expectedPopulation}' 대상이 맞습니다.` : "",
    gender === "male" && (lifeStage === "pregnant" || lifeStage === "lactating") ? "남성에는 임신/수유 단계를 적용할 수 없습니다." : "",
    (lifeStage === "pregnant" || lifeStage === "lactating") && populationGroup !== lifeStage ? "임신/수유 단계는 대상도 같은 값으로 맞추는 것이 좋습니다." : ""
  ].filter(Boolean);
  const canonicalProfileWarnings = [
    ...profileWarnings,
    gender === "male" && isPregnancyOrLactation(lifeStage) ? "Pregnancy/lactation targets require female gender." : "",
    stagePopulation && populationGroup !== stagePopulation ? `Population group should be ${stagePopulation} for the selected life stage.` : ""
  ].filter((message, index, all) => Boolean(message) && all.indexOf(message) === index);

  function normalizeProfileInputs() {
    const nextPopulation = stagePopulation || expectedPopulation;
    setPopulationGroup(nextPopulation);
    if (isPregnancyOrLactation(lifeStage)) setGender("female");
  }

  return (
    <section className="page daily-nutrition-page">
      <PageHeader
        title="하루 필요 영양량"
        subtitle="Daily Values, Nutrient Intake Rules, Nutrition Constraints를 한곳에서 보고 실제 하루 목표량을 확인합니다."
        action={<button type="button" onClick={() => { loadBasics(); loadTargets(); }}>새로고침</button>}
      />

      <section className="daily-nutrition-summary">
        <div><Target size={18} /><strong>{profiles.length}</strong><span>Daily Value 프로필</span></div>
        <div><ClipboardCheck size={18} /><strong>{targets.length}</strong><span>계산된 필요량</span></div>
        <div><Ruler size={18} /><strong>{constraints.length}</strong><span>최적화 제한값</span></div>
        <div><BadgeCheck size={18} /><strong>{verifiedProfiles + verifiedConstraints}</strong><span>검증된 항목</span></div>
      </section>

      <section className="daily-tabbar" aria-label="하루 영양자료">
        <button type="button" className={tab === "needed" ? "active" : ""} onClick={() => setTab("needed")}><Calculator size={16} /> 하루 필요량</button>
        <button type="button" className={tab === "rules" ? "active" : ""} onClick={() => setTab("rules")}><ClipboardCheck size={16} /> 섭취기준표</button>
        <button type="button" className={tab === "constraints" ? "active" : ""} onClick={() => setTab("constraints")}><Ruler size={16} /> 식단 제한값</button>
      </section>

      {error ? <p className="error">{error}</p> : null}

      {tab === "needed" ? (
        <section className="daily-needed-layout">
          <div className="daily-calculator-panel">
            <header>
              <p>프로필 입력</p>
              <h2>사용자에게 필요한 하루 목표량</h2>
            </header>
            <div className="daily-input-grid">
              <label>나이<input type="number" min={0} max={120} value={age} onChange={(event) => setAge(Number(event.target.value))} /></label>
              <label>성별
                <select value={gender} onChange={(event) => setGender(event.target.value)}>
                  <option value="all">전체</option><option value="male">남성</option><option value="female">녀성</option>
                </select>
              </label>
              <label>대상
                <select value={populationGroup} onChange={(event) => setPopulationGroup(event.target.value)}>
                  <option value="general">일반</option><option value="infant">젖먹이</option><option value="child">어린이</option>
                  <option value="adolescent">청소년</option><option value="adult">성인</option><option value="senior">로인</option>
                  <option value="pregnant">임신부</option><option value="lactating">수유</option>
                </select>
              </label>
              <label>단계
                <select value={lifeStage} onChange={(event) => setLifeStage(event.target.value)}>
                  <option value="general">일반</option><option value="pregnancy">임신 전체</option><option value="pregnancy_early">임신 초기</option><option value="pregnancy_mid">임신 중기</option><option value="pregnancy_late">임신 말기</option><option value="lactation">수유</option>
                </select>
              </label>
              <label>활동수준
                <select value={pal} onChange={(event) => setPal(event.target.value)}>
                  <option value="rest">안정</option><option value="light">가벼움</option><option value="moderate">보통</option><option value="heavy">높음</option>
                </select>
              </label>
            </div>
            {canonicalProfileWarnings.length || targetWarnings.length ? (
              <div className="daily-profile-warnings">
                {canonicalProfileWarnings.map((message) => <p key={message}>{message}</p>)}
                {targetWarnings.map((warning) => <p key={warning.code}>{warning.message}</p>)}
                <button type="button" onClick={normalizeProfileInputs}>입력값 자동정리</button>
              </div>
            ) : null}
            <button className="primary" type="button" onClick={loadTargets} disabled={targetLoading}>{targetLoading ? "계산 중..." : "필요량 계산"}</button>
          </div>

          <div className="daily-target-panel">
            <header>
              <div>
                <p>계산 결과</p>
                <h2>오늘 기준 목표량</h2>
              </div>
              <span>{targetLoading ? "계산 중" : `${targets.length}개`}</span>
            </header>
            <div className="daily-target-grid">
              {topTargets.map((target) => (
                <article key={target.nutrientKey}>
                  <span>{nutrientLabels[target.nutrientKey] ?? target.nutrientLabel}</span>
                  <strong>{formatValue(targetGoal(target), targetUnit(target.unit))}</strong>
                  <small>{goalLabel(target.goalType)}{target.UL ? ` · 상한 ${formatValue(target.UL, refUnit(target, "UL"))}` : ""}</small>
                  <div className="daily-target-detail">
                    {target.EAR != null ? <em>EAR {formatValue(target.EAR, refUnit(target, "EAR"))}</em> : null}
                    {target.RNI != null ? <em>RNI {formatValue(target.RNI, refUnit(target, "RNI"))}</em> : null}
                    {target.AI != null ? <em>AI {formatValue(target.AI, refUnit(target, "AI"))}</em> : null}
                    {target.EER != null ? <em>EER {formatValue(target.EER, refUnit(target, "EER"))}</em> : null}
                    {target.AMDR ? <em>AMDR {amdrText(target)}</em> : null}
                  </div>
                  <small>산출: RNI → AI → EER → EAR</small>
                </article>
              ))}
            </div>
          </div>

          <div className="daily-profile-panel">
            <header>
              <div>
                <p>Daily Values</p>
                <h2>앱에서 쓰는 하루 기준 프로필</h2>
              </div>
              <label className="search-field">
                <Search size={16} />
                <input value={profileQuery} onChange={(event) => setProfileQuery(event.target.value)} placeholder="프로필 검색" />
              </label>
            </header>
            <div className="daily-profile-grid">
              {filteredProfiles.map((profile) => (
                <article key={profile._id}>
                  <header>
                    <div>
                      <p>{profile.profileKey}</p>
                      <h3>{profile.label}</h3>
                    </div>
                    <span>{ageLabel(profile)}</span>
                  </header>
                  <div className="daily-profile-values">
                    {highlightKeys.map((key) => (
                      <div key={`${profile._id}-${key}`}>
                        <span>{nutrientLabels[key] ?? key}</span>
                        <strong>{formatValue(profile.values?.[key])}</strong>
                      </div>
                    ))}
                  </div>
                  {profile.notes ? <p className="daily-profile-note">{profile.notes}</p> : null}
                </article>
              ))}
              {!loading && !filteredProfiles.length ? <p className="empty-row">프로필이 없습니다.</p> : null}
            </div>
          </div>
        </section>
      ) : null}

      {tab === "rules" ? <NutrientIntakeRulesManager embedded /> : null}

      {tab === "constraints" ? (
        <section className="daily-constraints-panel">
          <header>
            <div>
              <p>Nutrition Constraints</p>
              <h2>식단 최적화에 쓰는 하한/상한값</h2>
            </div>
            <label className="search-field">
              <Search size={16} />
              <input value={constraintQuery} onChange={(event) => setConstraintQuery(event.target.value)} placeholder="프로필, 영양소 검색" />
            </label>
          </header>
          <div className="daily-constraint-list">
            {filteredConstraints.map((item) => (
              <article key={item._id}>
                <div className="daily-constraint-name">
                  <Utensils size={16} />
                  <div>
                    <h3>{nutrientLabels[item.nutrientKey] ?? item.nutrientLabel}</h3>
                    <p>{item.profileKey}</p>
                  </div>
                </div>
                <div className="daily-bound-row">
                  <span>하한 <b>{formatValue(item.lowerBound, item.unit)}</b></span>
                  <span>상한 <b>{item.upperBound ? formatValue(item.upperBound, item.unit) : "없음"}</b></span>
                  {item.isPercentOfCalories ? <span><Activity size={14} /> 열량비률</span> : null}
                </div>
              </article>
            ))}
            {!loading && !filteredConstraints.length ? <p className="empty-row">조건에 맞는 제한값이 없습니다.</p> : null}
          </div>
        </section>
      ) : null}
    </section>
  );
}
