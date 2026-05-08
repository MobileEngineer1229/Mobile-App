import { ResourceManager } from "@/components/ResourceManager";
import { resourceConfigs } from "@/lib/resourceConfigs";

const cfg = resourceConfigs["risk-assessment-rules"];

export default function RiskAssessmentRulesPage() {
  return (
    <ResourceManager
      title="Risk Assessment Rules"
      description="BMI, child growth, waist, obesity screening, and pregnancy weight-gain reference rules."
      endpoint="/risk-assessment-rules"
      searchPlaceholder="Search standards, metrics, populations"
      filters={cfg.filters}
      bulkActions={cfg.bulkActions}
      sortable={cfg.sortable}
      columns={[
        { key: "standardCode", label: "Standard" },
        { key: "metricLabel", label: "Metric" },
        { key: "populationGroup", label: "Population" },
        { key: "ageMin", label: "Age Min" },
        { key: "ageMax", label: "Age Max" },
        { key: "gender", label: "Gender" },
        { key: "interpretationKo", label: "Interpretation" },
        { key: "doctor_verified", label: "Doctor Verified", kind: "boolean" }
      ]}
      fields={[
        { name: "ruleKey", label: "Rule Key", type: "text", required: true },
        { name: "standardCode", label: "Standard Code", type: "text", required: true },
        { name: "metricKey", label: "Metric Key", type: "text", required: true },
        { name: "metricLabel", label: "Metric Label", type: "text", required: true },
        { name: "populationGroup", label: "Population Group", type: "text", required: true },
        { name: "ageMin", label: "Age Min", type: "number" },
        { name: "ageMax", label: "Age Max", type: "number" },
        { name: "gender", label: "Gender", type: "select", options: ["all", "male", "female"] },
        { name: "thresholds", label: "Thresholds JSON", type: "textarea" },
        { name: "interpretationKo", label: "Interpretation KO", type: "textarea", required: true },
        { name: "dataSource", label: "Data Source", type: "text", required: true },
        { name: "sourceNote", label: "Source Note", type: "textarea" },
        { name: "sourceRefs", label: "Source Refs", type: "tags" },
        { name: "tags", label: "Tags", type: "tags" },
        { name: "doctor_verified", label: "Doctor Verified", type: "boolean" }
      ]}
    />
  );
}
