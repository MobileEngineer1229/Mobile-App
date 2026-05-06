import { Activity, Apple, BookOpen, Flame, Salad, Scale, Users } from "lucide-react";
import PageHeader from "@/components/PageHeader";
import { apiFetch } from "@/lib/api";

type Stats = {
  foods: number;
  recipes: number;
  activities: number;
  users: number;
  mealLogs: number;
  weightEntries: number;
  caloriesLogged: number;
};

const fallback: Stats = {
  foods: 0,
  recipes: 0,
  activities: 0,
  users: 0,
  mealLogs: 0,
  weightEntries: 0,
  caloriesLogged: 0
};

export default async function DashboardPage() {
  const stats = await apiFetch<Stats>("/dashboard").catch(() => fallback);
  const cards = [
    { label: "Foods", value: stats.foods, icon: Apple },
    { label: "Recipes", value: stats.recipes, icon: Salad },
    { label: "Activities", value: stats.activities, icon: Activity },
    { label: "Users", value: stats.users, icon: Users },
    { label: "Meal logs", value: stats.mealLogs, icon: BookOpen },
    { label: "Weight entries", value: stats.weightEntries, icon: Scale },
    { label: "Calories logged", value: stats.caloriesLogged, icon: Flame }
  ];

  return (
    <section>
      <PageHeader
        title="Dashboard"
        subtitle="Manage Foodvisor-style onboarding, nutrition, activity, diary, and profile content."
      />

      <div className="stats">
        {cards.map((card) => {
          const Icon = card.icon;
          return (
            <article className="stat" key={card.label}>
              <Icon size={22} />
              <strong>{card.value}</strong>
              <span>{card.label}</span>
            </article>
          );
        })}
      </div>

      <div className="insight-band">
        <h2>Reference Coverage</h2>
        <p>
          The admin is organized around the supplied mobile screenshots: profile intake, nutrition goals, food logging,
          recipes, activities, weight tracking, and meal diary records. Seeded records include `doctor_verified: false`.
        </p>
      </div>
    </section>
  );
}
