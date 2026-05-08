import { Activity, Apple, BookOpen, ClipboardList, FileQuestion, LayoutDashboard, Ruler, Salad, Target, Users } from "lucide-react";

export const navItems = [
  { group: null, href: "/", label: "Dashboard", icon: LayoutDashboard },
  { group: "Nutrition", href: "/foods", label: "Foods", icon: Apple },
  { group: "Nutrition", href: "/daily-value-profiles", label: "Daily Values", icon: Target },
  { group: "Nutrition", href: "/nutrition-constraints", label: "Nutrition Constraints", icon: Ruler },
  { group: "Nutrition", href: "/recipes", label: "Recipes", icon: Salad },
  { group: "Nutrition", href: "/recipe-ingredients", label: "Recipe Ingredients", icon: Apple },
  { group: "Activity", href: "/activities", label: "Activities", icon: Activity },
  { group: "People", href: "/users", label: "Users", icon: Users },
  { group: "People", href: "/human-type-qa", label: "Human Type QA", icon: FileQuestion },
  { group: "People", href: "/human-type-surveys", label: "Human Type Surveys", icon: ClipboardList },
  { group: "People", href: "/meal-logs", label: "Meal Logs", icon: BookOpen }
] as const;
