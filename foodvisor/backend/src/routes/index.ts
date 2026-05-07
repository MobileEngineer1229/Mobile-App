import { Router } from "express";
import { Activity, DailyValueProfile, Food, MealLog, Program, Recipe, RecipeIngredient, User, WeightEntry } from "../models/content.js";
import { createCrudRouter } from "./crud.js";
import { dashboardRouter } from "./dashboard.js";

export const apiRouter = Router();

apiRouter.get("/health", (_req, res) => {
  res.json({ status: "ok", service: "foodvisor-backend" });
});

apiRouter.use("/dashboard", dashboardRouter);
apiRouter.use("/foods", createCrudRouter(Food, ["koreanName", "chineseName", "brand", "category", "foodGroup", "foodSubgroup", "tags"]));
apiRouter.use("/daily-value-profiles", createCrudRouter(DailyValueProfile, ["profileKey", "label", "purpose", "notes"]));
apiRouter.use("/recipes", createCrudRouter(Recipe, ["title", "description", "mealType", "tags"]));
apiRouter.use("/recipe-ingredients", createCrudRouter(RecipeIngredient, ["name", "koreanName", "foodName", "category", "notes"]));
apiRouter.use("/activities", createCrudRouter(Activity, ["name", "category", "description"]));
apiRouter.use("/users", createCrudRouter(User, ["name", "email", "goal"]));
apiRouter.use("/meal-logs", createCrudRouter(MealLog, ["userName", "foodName", "mealType", "source"]));
apiRouter.use("/weight-entries", createCrudRouter(WeightEntry, ["userName", "note"]));
apiRouter.use("/programs", createCrudRouter(Program, ["title", "stage", "focus", "description"]));
