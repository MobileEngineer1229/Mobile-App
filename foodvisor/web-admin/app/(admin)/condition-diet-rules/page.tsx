import { ResourceManager } from "@/components/ResourceManager";
import { resourceConfigs } from "@/lib/resourceConfigs";

const cfg = resourceConfigs["condition-diet-rules"];

export default function ConditionDietRulesPage() {
  return (
    <ResourceManager
      title="Condition Diet Rules"
      description="Structured food and nutrient guidance for hypertension, diabetes, obesity, hyperlipidemia, and gout."
      endpoint="/condition-diet-rules"
      searchPlaceholder="Search condition, nutrient, recommendation"
      filters={cfg.filters}
      bulkActions={cfg.bulkActions}
      sortable={cfg.sortable}
      columns={[
        { key: "conditionLabel", label: "Condition" },
        { key: "ruleType", label: "Rule" },
        { key: "nutrientKey", label: "Nutrient" },
        { key: "comparator", label: "Comparator" },
        { key: "targetValue", label: "Target" },
        { key: "unit", label: "Unit" },
        { key: "recommendationKo", label: "Recommendation" },
        { key: "doctor_verified", label: "Doctor Verified", kind: "boolean" }
      ]}
      fields={[
        { name: "ruleKey", label: "Rule Key", type: "text", required: true },
        { name: "conditionKey", label: "Condition Key", type: "text", required: true },
        { name: "conditionLabel", label: "Condition Label", type: "text", required: true },
        { name: "ruleType", label: "Rule Type", type: "text", required: true },
        { name: "priority", label: "Priority", type: "number" },
        { name: "nutrientKey", label: "Nutrient Key", type: "text" },
        { name: "comparator", label: "Comparator", type: "select", options: ["eq", "lt", "lte", "gt", "gte", "range", "avoid", "prefer"] },
        { name: "targetValue", label: "Target Value", type: "number" },
        { name: "targetMin", label: "Target Min", type: "number" },
        { name: "targetMax", label: "Target Max", type: "number" },
        { name: "unit", label: "Unit", type: "text" },
        { name: "recommendationKo", label: "Recommendation KO", type: "textarea", required: true },
        { name: "recommendationZh", label: "Recommendation ZH", type: "textarea" },
        { name: "foodTagsPrefer", label: "Prefer Food Tags", type: "tags" },
        { name: "foodTagsAvoid", label: "Avoid Food Tags", type: "tags" },
        { name: "cautionTags", label: "Caution Tags", type: "tags" },
        { name: "dataSource", label: "Data Source", type: "text", required: true },
        { name: "sourceNote", label: "Source Note", type: "textarea" },
        { name: "sourceRefs", label: "Source Refs", type: "tags" },
        { name: "doctor_verified", label: "Doctor Verified", type: "boolean" }
      ]}
    />
  );
}
