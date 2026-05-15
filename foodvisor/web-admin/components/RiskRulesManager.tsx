"use client";

import { Activity, BadgeCheck, Calculator, RefreshCw, Ruler, Search, Users } from "lucide-react";
import { useEffect, useMemo, useState } from "react";
import PageHeader from "@/components/PageHeader";
import { apiFetch } from "@/lib/api";

type RiskRule = {
  _id: string;
  ruleKey: string;
  standardCode: string;
  metricKey: string;
  metricLabel: string;
  populationGroup: string;
  ageMin?: number;
  ageMax?: number;
  gender?: "all" | "male" | "female";
  thresholds?: unknown;
  interpretationKo: string;
  dataSource: string;
  sourceRefs?: string[];
  tags?: string[];
  doctor_verified?: boolean;
};

type ApiList = {
  items: RiskRule[];
  total: number;
  page: number;
  limit: number;
};

type ThresholdItem = {
  label?: string;
  comparator?: string;
  valueMin?: number;
  valueMax?: number;
  bmiMin?: number;
  bmiMax?: number;
  age?: number;
  maleOverweight?: number;
  maleObesity?: number;
  femaleOverweight?: number;
  femaleObesity?: number;
  maleP75?: number;
  maleP90?: number;
  femaleP75?: number;
  femaleP90?: number;
  bmiCategory?: string;
  totalGainMinKg?: number;
  totalGainMaxKg?: number;
  weeklyGainMinKg?: number;
  weeklyGainMaxKg?: number;
};

type WeightOrHeightThresholds = {
  male?: number[][];
  female?: number[][];
  unit?: string;
  bands?: string[];
  note?: string;
};

type HeightWeightThresholds = {
  formula?: string;
  chineseWst428?: ThresholdItem[];
  healthyWeightKgFormula?: { min?: string; max?: string };
};

const populationLabels: Record<string, string> = {
  adult: "성인",
  child_adolescent: "어린이/청소년",
  pregnant: "임신부"
};

const metricLabels: Record<string, string> = {
  bmi: "성인 BMI",
  height_weight_bmi: "키-몸무게 BMI",
  child_bmi: "어린이 BMI",
  weight_kg: "나이별 몸무게",
  waist_cm: "허리둘레",
  height_cm: "나이별 키",
  pregnancy_weight_gain_kg: "임신 체중 증가"
};

const metricOptions = [
  { value: "", label: "모든 지표" },
  { value: "bmi", label: "성인 BMI" },
  { value: "height_weight_bmi", label: "키-몸무게 BMI" },
  { value: "child_bmi", label: "어린이 BMI" },
  { value: "weight_kg", label: "나이별 몸무게" },
  { value: "height_cm", label: "나이별 키" },
  { value: "waist_cm", label: "허리둘레" },
  { value: "pregnancy_weight_gain_kg", label: "임신 체중 증가" }
];

function ageLabel(rule: RiskRule) {
  if (rule.ageMin != null && rule.ageMax != null) return `${rule.ageMin}-${rule.ageMax}세`;
  if (rule.ageMin != null) return `${rule.ageMin}세 이상`;
  if (rule.ageMax != null) return `${rule.ageMax}세 이하`;
  return "전체";
}

function genderLabel(gender?: string) {
  if (gender === "male") return "남성";
  if (gender === "female") return "여성";
  return "전체";
}

function comparatorLabel(item: ThresholdItem) {
  const min = item.valueMin ?? item.bmiMin;
  const max = item.valueMax ?? item.bmiMax;
  if (item.comparator === "lt") return `< ${max}`;
  if (item.comparator === "gte") return `>= ${min}`;
  if (min != null && max != null) return `${min} - ${max}`;
  if (min != null) return `>= ${min}`;
  if (max != null) return `< ${max}`;
  return "-";
}

function isObject(value: unknown): value is Record<string, unknown> {
  return Boolean(value && typeof value === "object" && !Array.isArray(value));
}

function asThresholdItems(value: unknown): ThresholdItem[] {
  return Array.isArray(value) ? value.filter(isObject) as ThresholdItem[] : [];
}

function renderSimpleBands(items: ThresholdItem[], unit = "") {
  return (
    <div className="risk-band-list">
      {items.map((item, index) => (
        <div className="risk-band" key={`${item.label ?? item.bmiCategory ?? index}-${index}`}>
          <span>{item.label ?? item.bmiCategory ?? `기준 ${index + 1}`}</span>
          <strong>{comparatorLabel(item)}{unit ? ` ${unit}` : ""}</strong>
        </div>
      ))}
    </div>
  );
}

function renderRowsTable(rows: number[][], unit: string, heading: string) {
  return (
    <div className="risk-mini-table-block">
      <h3>{heading}</h3>
      <table className="risk-mini-table">
        <thead>
          <tr>
            <th>나이</th>
            <th>-2 표준편차</th>
            <th>-1 표준편차</th>
            <th>가운데값</th>
            <th>+1 표준편차</th>
            <th>+2 표준편차</th>
          </tr>
        </thead>
        <tbody>
          {rows.map((row) => (
            <tr key={`${heading}-${row[0]}`}>
              <td>{row[0]}세</td>
              <td>{row[1]}{unit}</td>
              <td>{row[2]}{unit}</td>
              <td>{row[3]}{unit}</td>
              <td>{row[4]}{unit}</td>
              <td>{row[5]}{unit}</td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}

function renderGenderedGrowthTable(thresholds: WeightOrHeightThresholds, fallbackUnit: string) {
  const unit = thresholds.unit ?? fallbackUnit;
  return (
    <div className="risk-table-pair">
      {thresholds.male?.length ? renderRowsTable(thresholds.male, unit, "남아") : null}
      {thresholds.female?.length ? renderRowsTable(thresholds.female, unit, "여아") : null}
      {thresholds.note ? <p className="risk-note">{thresholds.note}</p> : null}
    </div>
  );
}

function renderChildBmiTable(items: ThresholdItem[]) {
  return (
    <table className="risk-mini-table">
      <thead>
        <tr>
          <th>나이</th>
          <th>남아 과체중</th>
          <th>남아 비만</th>
          <th>여아 과체중</th>
          <th>여아 비만</th>
        </tr>
      </thead>
      <tbody>
        {items.map((item) => (
          <tr key={`bmi-${item.age}`}>
            <td>{item.age}세</td>
            <td>{item.maleOverweight}</td>
            <td>{item.maleObesity}</td>
            <td>{item.femaleOverweight}</td>
            <td>{item.femaleObesity}</td>
          </tr>
        ))}
      </tbody>
    </table>
  );
}

function renderWaistTable(items: ThresholdItem[]) {
  return (
    <table className="risk-mini-table">
      <thead>
        <tr>
          <th>나이</th>
          <th>남아 P75</th>
          <th>남아 P90</th>
          <th>여아 P75</th>
          <th>여아 P90</th>
        </tr>
      </thead>
      <tbody>
        {items.map((item) => (
          <tr key={`waist-${item.age}`}>
            <td>{item.age}세</td>
            <td>{item.maleP75}cm</td>
            <td>{item.maleP90}cm</td>
            <td>{item.femaleP75}cm</td>
            <td>{item.femaleP90}cm</td>
          </tr>
        ))}
      </tbody>
    </table>
  );
}

function renderPregnancy(items: ThresholdItem[]) {
  return (
    <table className="risk-mini-table">
      <thead>
        <tr>
          <th>BMI 분류</th>
          <th>총 증가</th>
          <th>주당 증가</th>
        </tr>
      </thead>
      <tbody>
        {items.map((item) => (
          <tr key={item.bmiCategory}>
            <td>{item.bmiCategory}</td>
            <td>{item.totalGainMinKg}-{item.totalGainMaxKg}kg</td>
            <td>{item.weeklyGainMinKg}-{item.weeklyGainMaxKg}kg</td>
          </tr>
        ))}
      </tbody>
    </table>
  );
}

function renderThresholds(rule: RiskRule) {
  if (rule.metricKey === "weight_kg" && isObject(rule.thresholds)) {
    return renderGenderedGrowthTable(rule.thresholds as WeightOrHeightThresholds, "kg");
  }

  if (rule.metricKey === "height_cm" && isObject(rule.thresholds)) {
    return renderGenderedGrowthTable(rule.thresholds as WeightOrHeightThresholds, "cm");
  }

  if (rule.metricKey === "height_weight_bmi" && isObject(rule.thresholds)) {
    const thresholds = rule.thresholds as HeightWeightThresholds;
    return (
      <div className="risk-threshold-stack">
        <p className="risk-formula">{thresholds.formula}</p>
        {thresholds.chineseWst428 ? renderSimpleBands(thresholds.chineseWst428, "BMI") : null}
        {thresholds.healthyWeightKgFormula ? (
          <p className="risk-note">적정 체중 계산: {thresholds.healthyWeightKgFormula.min} - {thresholds.healthyWeightKgFormula.max}</p>
        ) : null}
      </div>
    );
  }

  const items = asThresholdItems(rule.thresholds);
  if (!items.length) return <p className="risk-note">기준값이 아직 정리되지 않았습니다.</p>;

  if (rule.metricKey === "child_bmi") return renderChildBmiTable(items);
  if (rule.metricKey === "waist_cm") return renderWaistTable(items);
  if (rule.metricKey === "pregnancy_weight_gain_kg") return renderPregnancy(items);
  return renderSimpleBands(items, rule.metricKey.includes("bmi") ? "BMI" : "");
}

function ruleIcon(metricKey: string) {
  if (metricKey.includes("height") || metricKey.includes("weight")) return Ruler;
  if (metricKey.includes("bmi")) return Calculator;
  if (metricKey.includes("waist")) return Activity;
  return Users;
}

export default function RiskRulesManager() {
  const [items, setItems] = useState<RiskRule[]>([]);
  const [query, setQuery] = useState("");
  const [population, setPopulation] = useState("");
  const [metric, setMetric] = useState("");
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

  async function load() {
    setLoading(true);
    setError("");
    try {
      const params = new URLSearchParams({ page: "1", limit: "100", sort: "metricKey" });
      if (query.trim()) params.set("q", query.trim());
      if (population) params.set("populationGroup", population);
      if (metric) params.set("metricKey", metric);
      const data = await apiFetch<ApiList>(`/risk-assessment-rules?${params.toString()}`);
      setItems(data.items);
    } catch (err) {
      setError(err instanceof Error ? err.message : "Risk Rules를 불러오지 못했습니다.");
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => {
    load();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [population, metric]);

  const stats = useMemo(() => {
    const child = items.filter((item) => item.populationGroup === "child_adolescent").length;
    const adult = items.filter((item) => item.populationGroup === "adult").length;
    const verified = items.filter((item) => item.doctor_verified).length;
    return { child, adult, verified };
  }, [items]);

  return (
    <section className="page risk-rules-page">
      <PageHeader
        title="Risk Rules"
        subtitle="키, 몸무게, BMI, 허리둘레, 임신 체중 증가 기준을 관리자용으로 읽기 쉽게 정리합니다."
        action={<button type="button" onClick={load}><RefreshCw size={16} /> 새로고침</button>}
      />

      <section className="risk-summary-strip">
        <div><strong>{items.length}</strong><span>표시 중</span></div>
        <div><strong>{stats.child}</strong><span>어린이/청소년</span></div>
        <div><strong>{stats.adult}</strong><span>성인</span></div>
        <div><strong>{stats.verified}</strong><span>검증됨</span></div>
      </section>

      <section className="panel">
        <div className="toolbar risk-toolbar">
          <label className="search-field">
            <Search size={16} />
            <input
              value={query}
              onChange={(event) => setQuery(event.target.value)}
              onKeyDown={(event) => event.key === "Enter" && load()}
              placeholder="기준명, 지표, 대상 검색"
            />
          </label>
          <button onClick={load} type="button">검색</button>
          <select className="sort-select" value={population} onChange={(event) => setPopulation(event.target.value)}>
            <option value="">모든 대상</option>
            <option value="adult">성인</option>
            <option value="child_adolescent">어린이/청소년</option>
            <option value="pregnant">임신부</option>
          </select>
          <select className="sort-select" value={metric} onChange={(event) => setMetric(event.target.value)}>
            {metricOptions.map((option) => <option key={option.value || "all"} value={option.value}>{option.label}</option>)}
          </select>
          <span>{loading ? "불러오는 중..." : `${items.length}개 기준`}</span>
        </div>

        {error ? <p className="error">{error}</p> : null}

        <div className="risk-rule-grid">
          {items.map((rule) => {
            const Icon = ruleIcon(rule.metricKey);
            return (
              <article className="risk-rule-card" key={rule._id}>
                <header className="risk-rule-card-head">
                  <div className="risk-rule-icon"><Icon size={18} /></div>
                  <div>
                    <p>{rule.standardCode}</p>
                    <h2>{metricLabels[rule.metricKey] ?? rule.metricLabel}</h2>
                  </div>
                  {rule.doctor_verified ? <span className="badge ok"><BadgeCheck size={14} /> 검증</span> : <span className="badge">미검증</span>}
                </header>

                <div className="risk-meta-grid">
                  <span><b>대상</b>{populationLabels[rule.populationGroup] ?? rule.populationGroup}</span>
                  <span><b>연령</b>{ageLabel(rule)}</span>
                  <span><b>성별</b>{genderLabel(rule.gender)}</span>
                  <span><b>자료</b>{rule.dataSource}</span>
                </div>

                <p className="risk-interpretation">{rule.interpretationKo}</p>

                <div className="risk-threshold-wrap">
                  {renderThresholds(rule)}
                </div>

                {rule.tags?.length ? (
                  <div className="risk-tag-row">
                    {rule.tags.map((tag, index) => <span key={`${rule._id}-${tag}-${index}`}>{tag}</span>)}
                  </div>
                ) : null}
              </article>
            );
          })}

          {!loading && !items.length ? <p className="empty-row">조건에 맞는 Risk Rule이 없습니다.</p> : null}
        </div>
      </section>
    </section>
  );
}
