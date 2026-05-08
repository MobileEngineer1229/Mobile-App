import { Router } from "express";
import { mkdir, writeFile } from "fs/promises";
import path from "path";
import crypto from "crypto";
import { Activity } from "../models/activity.js";
import { DailyValueProfile } from "../models/daily-value-profile.js";
import { Food } from "../models/food.js";
import { HumanTypeQA } from "../models/human-type-qa.js";
import { HumanTypeSurvey } from "../models/human-type-survey.js";
import { MealLog } from "../models/meal-log.js";
import { NutritionConstraint } from "../models/nutrition-constraint.js";
import { Program } from "../models/program.js";
import { RecipeIngredient } from "../models/recipe-ingredient.js";
import { Recipe } from "../models/recipe.js";
import { User } from "../models/user.js";
import { WeightEntry } from "../models/weight-entry.js";
import { createCrudRouter } from "./crud.js";
import { dashboardRouter } from "./dashboard.js";
import { dietOptimizerRouter } from "./diet-optimizer-routes.js";
import { healthMetricsRouter } from "./health-metrics-routes.js";
import { classifyHumanTypeAnswers } from "../services/humanTypeClassifier.js";

export const apiRouter = Router();

apiRouter.get("/health", (_req, res) => {
  res.json({ status: "ok", service: "foodvisor-backend" });
});

apiRouter.use("/dashboard", dashboardRouter);
apiRouter.use("/health-metrics", healthMetricsRouter);
apiRouter.use("/diet-optimizer", dietOptimizerRouter);

const foodsRouter = createCrudRouter(Food, ["koreanName", "chineseName", "brand", "category", "foodGroup", "foodSubgroup", "tags"]);

foodsRouter.post("/image-upload", async (req, res, next) => {
  try {
    const { fileName, mimeType, data } = req.body as { fileName?: string; mimeType?: string; data?: string };
    const extensions: Record<string, string> = {
      "image/jpeg": ".jpg",
      "image/png": ".png",
      "image/webp": ".webp",
      "image/gif": ".gif"
    };
    const ext = extensions[mimeType ?? ""];

    if (!ext || !data) {
      res.status(400).json({ message: "Only jpg, png, webp, and gif image uploads are supported." });
      return;
    }

    const buffer = Buffer.from(data, "base64");
    if (!buffer.length || buffer.byteLength > 8 * 1024 * 1024) {
      res.status(400).json({ message: "Image must be smaller than 8MB." });
      return;
    }

    const baseName = path.parse(fileName ?? "food-image").name
      .toLowerCase()
      .replace(/[^a-z0-9]+/g, "-")
      .replace(/(^-|-$)/g, "")
      .slice(0, 48) || "food-image";
    const storedName = `${baseName}-${crypto.randomUUID()}${ext}`;
    const uploadDir = path.join(process.cwd(), "public", "images", "uploads");

    await mkdir(uploadDir, { recursive: true });
    await writeFile(path.join(uploadDir, storedName), buffer);

    res.status(201).json({
      imageUrl: `/images/uploads/${storedName}`,
      imageStatus: "uploaded"
    });
  } catch (error) {
    next(error);
  }
});

apiRouter.use("/foods", foodsRouter);
apiRouter.use("/daily-value-profiles", createCrudRouter(DailyValueProfile, ["profileKey", "label", "purpose", "notes"]));
apiRouter.use("/nutrition-constraints", createCrudRouter(NutritionConstraint, ["profileKey", "nutrientKey", "nutrientLabel", "dataSource", "sourceNote"]));
apiRouter.use("/recipes", createCrudRouter(Recipe, ["title", "description", "mealType", "tags"]));
apiRouter.use("/recipe-ingredients", createCrudRouter(RecipeIngredient, ["name", "koreanName", "foodName", "category", "notes"]));
apiRouter.use("/activities", createCrudRouter(Activity, ["name", "category", "description"]));
apiRouter.use("/users", createCrudRouter(User, ["name", "email", "goal"]));
apiRouter.use("/human-type-qa", createCrudRouter(HumanTypeQA, ["questionId", "categoryKey", "categoryNameKo", "promptKo", "scoringNoteKo", "analysisNoteKo"]));

const humanTypeSurveyRouter = createCrudRouter(HumanTypeSurvey, ["userName", "status", "resultType", "resultLabelKo", "summaryKo", "notes"]);

humanTypeSurveyRouter.post("/classify", async (req, res, next) => {
  try {
    const { userId, userName, answers, notes } = req.body as {
      userId?: string;
      userName?: string;
      answers?: Array<{ questionId: string; value: unknown; labelKo?: string }>;
      notes?: string;
    };

    if (!Array.isArray(answers) || !answers.length) {
      res.status(400).json({ message: "answers must be a non-empty array." });
      return;
    }

    const result = await classifyHumanTypeAnswers(answers);
    const survey = await HumanTypeSurvey.create({
      userId: userId || undefined,
      userName,
      status: "completed",
      notes,
      doctor_verified: false,
      ...result
    });

    res.status(201).json(survey);
  } catch (error) {
    next(error);
  }
});

humanTypeSurveyRouter.post("/:id/reclassify", async (req, res, next) => {
  try {
    const existing = await HumanTypeSurvey.findById(req.params.id).lean() as {
      answers: Array<{ questionId: string; value: unknown; labelKo?: string }>;
      status?: string;
    } | null;
    if (!existing) {
      res.status(404).json({ message: "Resource not found" });
      return;
    }

    const result = await classifyHumanTypeAnswers(existing.answers);
    const updated = await HumanTypeSurvey.findByIdAndUpdate(
      req.params.id,
      {
        ...result,
        status: existing.status === "draft" ? "completed" : existing.status
      },
      { new: true, runValidators: true }
    );

    res.json(updated);
  } catch (error) {
    next(error);
  }
});

apiRouter.use("/human-type-surveys", humanTypeSurveyRouter);
apiRouter.use("/meal-logs", createCrudRouter(MealLog, ["userName", "foodName", "mealType", "source"]));
apiRouter.use("/weight-entries", createCrudRouter(WeightEntry, ["userName", "note"]));
apiRouter.use("/programs", createCrudRouter(Program, ["title", "stage", "focus", "description"]));
