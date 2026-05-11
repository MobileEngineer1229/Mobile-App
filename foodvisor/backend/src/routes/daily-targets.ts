import { Router } from "express";
import { ValidationError } from "../utils/errors.js";
import { User } from "../models/user.js";
import { getDailyTargets, preferredGoal, type UserProfileForTargets } from "../services/dailyTargets.js";

export const dailyTargetsRouter = Router();

function parseProfileFromQuery(query: Record<string, unknown>): UserProfileForTargets {
  const age = Number(query.age);
  if (!Number.isFinite(age) || age < 0 || age > 120) {
    throw new ValidationError("age must be a finite number 0–120");
  }
  const genderRaw = String(query.gender ?? "all");
  const gender: UserProfileForTargets["gender"] =
    genderRaw === "male" || genderRaw === "female" ? genderRaw : "all";

  return {
    age,
    gender,
    lifeStage: query.lifeStage ? String(query.lifeStage) : undefined,
    populationGroup: query.populationGroup ? String(query.populationGroup) : undefined,
    physicalActivityLevel: query.pal ? String(query.pal) : undefined
  };
}

function projectGenderForUser(userGender: string | undefined): UserProfileForTargets["gender"] {
  if (userGender === "male" || userGender === "female") return userGender;
  return "all";
}

dailyTargetsRouter.get("/", async (req, res, next) => {
  try {
    const profile = parseProfileFromQuery(req.query as Record<string, unknown>);
    const targets = await getDailyTargets(profile);
    res.json({
      profile,
      count: targets.length,
      targets,
      goals: Object.fromEntries(targets.map((t) => [t.nutrientKey, preferredGoal(t) ?? null]))
    });
  } catch (error) {
    next(error);
  }
});

dailyTargetsRouter.get("/user/:userId", async (req, res, next) => {
  try {
    const user = await User.findById(req.params.userId).lean() as { age?: number; gender?: string; medicalConditions?: string[] } | null;
    if (!user) {
      throw new ValidationError("User not found");
    }
    if (typeof user.age !== "number") {
      throw new ValidationError("User has no age — daily targets need a numeric age");
    }
    const profile: UserProfileForTargets = {
      age: user.age,
      gender: projectGenderForUser(user.gender),
      lifeStage: "general",
      populationGroup: "general"
    };
    const targets = await getDailyTargets(profile);
    res.json({
      userId: req.params.userId,
      profile,
      count: targets.length,
      targets,
      goals: Object.fromEntries(targets.map((t) => [t.nutrientKey, preferredGoal(t) ?? null]))
    });
  } catch (error) {
    next(error);
  }
});
