import { ResourceManager } from "@/components/ResourceManager";
import { resourceConfigs } from "@/lib/resourceConfigs";

const cfg = resourceConfigs["nutrient-intake-rules"];

export default function NutrientIntakeRulesPage() {
  return (
    <ResourceManager
      title="Nutrient Intake Rules"
      description="Age, gender, life-stage, and activity-level nutrient reference rules from WST578."
      endpoint="/nutrient-intake-rules"
      searchPlaceholder="Search nutrient, age group, standard, source"
      filters={cfg.filters}
      bulkActions={cfg.bulkActions}
      sortable={cfg.sortable}
      columns={[
        { key: "standardCode", label: "Standard" },
        { key: "nutrientLabel", label: "Nutrient" },
        { key: "referenceType", label: "Type" },
        { key: "ageGroup", label: "Age Group" },
        { key: "gender", label: "Gender" },
        { key: "lifeStage", label: "Life Stage" },
        { key: "physicalActivityLevel", label: "PAL" },
        { key: "rawValue", label: "Value" },
        { key: "unit", label: "Unit" },
        { key: "doctor_verified", label: "Doctor Verified", kind: "boolean" }
      ]}
      fields={[
        { name: "ruleKey", label: "Rule Key", type: "text", required: true },
        { name: "standardCode", label: "Standard Code", type: "text", required: true },
        { name: "nutrientKey", label: "Nutrient Key", type: "text", required: true },
        { name: "nutrientLabel", label: "Nutrient Label", type: "text", required: true },
        { name: "referenceType", label: "Reference Type", type: "text", required: true },
        { name: "unit", label: "Unit", type: "text", required: true },
        { name: "rawValue", label: "Raw Value", type: "text", required: true },
        { name: "comparator", label: "Comparator", type: "select", options: ["eq", "lt", "lte", "gt", "gte", "range"] },
        { name: "value", label: "Value", type: "number" },
        { name: "valueMin", label: "Value Min", type: "number" },
        { name: "valueMax", label: "Value Max", type: "number" },
        { name: "ageGroup", label: "Age Group", type: "text", required: true },
        { name: "ageMin", label: "Age Min", type: "number" },
        { name: "ageMax", label: "Age Max", type: "number" },
        { name: "gender", label: "Gender", type: "select", options: ["all", "male", "female"] },
        { name: "lifeStage", label: "Life Stage", type: "text" },
        { name: "populationGroup", label: "Population Group", type: "text" },
        { name: "physicalActivityLevel", label: "Physical Activity Level", type: "text" },
        { name: "dataSource", label: "Data Source", type: "text", required: true },
        { name: "sourceNote", label: "Source Note", type: "textarea" },
        { name: "sourceRefs", label: "Source Refs", type: "tags" },
        { name: "tags", label: "Tags", type: "tags" },
        { name: "doctor_verified", label: "Doctor Verified", type: "boolean" }
      ]}
    />
  );
}
