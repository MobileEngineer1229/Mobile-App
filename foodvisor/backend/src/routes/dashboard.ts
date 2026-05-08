import { Router } from "express";
import { Activity } from "../models/activity.js";
import { Food } from "../models/food.js";
import { MealLog } from "../models/meal-log.js";
import { Recipe } from "../models/recipe.js";
import { User } from "../models/user.js";
import { WeightEntry } from "../models/weight-entry.js";

export const dashboardRouter = Router();

dashboardRouter.get("/", async (_req, res, next) => {
  try {
    const [foods, recipes, activities, users, mealLogs, weightEntries, calories] = await Promise.all([
      Food.countDocuments(),
      Recipe.countDocuments(),
      Activity.countDocuments(),
      User.countDocuments(),
      MealLog.countDocuments(),
      WeightEntry.countDocuments(),
      MealLog.aggregate([{ $group: { _id: null, total: { $sum: "$calories" } } }])
    ]);

    res.json({
      foods,
      recipes,
      activities,
      users,
      mealLogs,
      weightEntries,
      caloriesLogged: calories[0]?.total ?? 0
    });
  } catch (error) {
    next(error);
  }
});
