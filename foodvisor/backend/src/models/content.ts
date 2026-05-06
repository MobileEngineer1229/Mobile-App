import { Schema, model } from "mongoose";

const macroSchema = new Schema(
  {
    protein: { type: Number, default: 0 },
    fat: { type: Number, default: 0 },
    carbs: { type: Number, default: 0 },
    fiber: { type: Number, default: 0 }
  },
  { _id: false }
);

const vitaminSchema = new Schema(
  {
    vitaminA: { type: Number, default: 0 },
    vitaminB1: { type: Number, default: 0 },
    vitaminB2: { type: Number, default: 0 },
    vitaminB3: { type: Number, default: 0 },
    vitaminB6: { type: Number, default: 0 },
    vitaminB12: { type: Number, default: 0 },
    vitaminC: { type: Number, default: 0 },
    vitaminD: { type: Number, default: 0 },
    vitaminE: { type: Number, default: 0 },
    vitaminK: { type: Number, default: 0 },
    folate: { type: Number, default: 0 }
  },
  { _id: false }
);

const mineralSchema = new Schema(
  {
    calcium: { type: Number, default: 0 },
    iron: { type: Number, default: 0 },
    magnesium: { type: Number, default: 0 },
    potassium: { type: Number, default: 0 },
    sodium: { type: Number, default: 0 },
    zinc: { type: Number, default: 0 }
  },
  { _id: false }
);

const dailyValuePercentSchema = new Schema(
  {
    calories: { type: Number, default: 0 },
    protein: { type: Number, default: 0 },
    carbs: { type: Number, default: 0 },
    fat: { type: Number, default: 0 },
    saturatedFat: { type: Number, default: 0 },
    fiber: { type: Number, default: 0 },
    sugar: { type: Number, default: 0 },
    sodium: { type: Number, default: 0 },
    cholesterol: { type: Number, default: 0 },
    calcium: { type: Number, default: 0 },
    iron: { type: Number, default: 0 },
    magnesium: { type: Number, default: 0 },
    potassium: { type: Number, default: 0 },
    zinc: { type: Number, default: 0 },
    vitaminA: { type: Number, default: 0 },
    vitaminB1: { type: Number, default: 0 },
    vitaminB2: { type: Number, default: 0 },
    vitaminB3: { type: Number, default: 0 },
    vitaminB6: { type: Number, default: 0 },
    vitaminB12: { type: Number, default: 0 },
    vitaminC: { type: Number, default: 0 },
    vitaminD: { type: Number, default: 0 },
    vitaminE: { type: Number, default: 0 },
    vitaminK: { type: Number, default: 0 },
    folate: { type: Number, default: 0 }
  },
  { _id: false }
);

const dailyValuePercentByProfileSchema = new Schema(
  {
    profileKey: { type: String, required: true, trim: true },
    label: { type: String, required: true, trim: true },
    purpose: { type: String, required: true, trim: true },
    values: { type: dailyValuePercentSchema, default: {} }
  },
  { _id: false }
);

const reviewFlag = {
  doctor_verified: { type: Boolean, default: false, index: true }
};

const foodSchema = new Schema(
  {
    name: { type: String, required: true, trim: true, index: true },
    koreanName: { type: String, trim: true, index: true },
    chineseName: { type: String, trim: true, index: true },
    fdcId: { type: Number, unique: true, sparse: true, index: true },
    dataType: { type: String, trim: true, index: true },
    brand: { type: String, trim: true },
    category: { type: String, required: true, trim: true, index: true },
    foodGroup: { type: String, trim: true, index: true },
    foodSubgroup: { type: String, trim: true, index: true },
    servingSize: { type: String, required: true },
    calories: { type: Number, required: true, min: 0 },
    macros: { type: macroSchema, default: {} },
    saturatedFat: { type: Number, default: 0 },
    transFat: { type: Number, default: 0 },
    unsaturatedFat: { type: Number, default: 0 },
    omega3: { type: Number, default: 0 },
    omega6: { type: Number, default: 0 },
    cholesterolMg: { type: Number, default: 0 },
    sugar: { type: Number, default: 0 },
    glycemicIndex: { type: Number, default: 0 },
    oiliness: { type: String, trim: true },
    oilinessScore: { type: Number, min: 0, max: 5, default: 0 },
    vitamins: { type: vitaminSchema, default: {} },
    minerals: { type: mineralSchema, default: {} },
    bestTimeToEat: [{ type: String, trim: true }],
    goodPairings: [{ type: String, trim: true }],
    avoidPairings: [{ type: String, trim: true }],
    cautionGroups: [{ type: String, trim: true }],
    cautions: { type: String, trim: true },
    koreanCautions: { type: String, trim: true },
    benefits: { type: String, trim: true },
    allergens: [{ type: String, trim: true }],
    ingredients: [{ type: String, trim: true }],
    additives: [{ type: String, trim: true }],
    dailyValuePercent: { type: dailyValuePercentSchema, default: {} },
    dailyValuePercentByProfile: { type: [dailyValuePercentByProfileSchema], default: [] },
    dietUseCases: [{ type: String, trim: true }],
    dietUseNote: { type: String, trim: true },
    dataSource: { type: String, trim: true },
    sourceNote: { type: String, trim: true },
    tags: [{ type: String, trim: true }],
    barcode: { type: String, trim: true },
    imageUrl: { type: String, trim: true },
    imageSource: { type: String, trim: true },
    imageSourceUrl: { type: String, trim: true },
    imagePageUrl: { type: String, trim: true },
    imageLicense: { type: String, trim: true },
    imageStatus: { type: String, trim: true, index: true },
    icon: { type: String, trim: true },
    ...reviewFlag
  },
  { timestamps: true }
);

const dailyValueProfileSchema = new Schema(
  {
    profileKey: { type: String, required: true, unique: true, trim: true, index: true },
    label: { type: String, required: true, trim: true },
    ageMin: { type: Number, required: true, min: 0 },
    ageMax: { type: Number, required: true, min: 0 },
    gender: { type: String, enum: ["all", "male", "female"], default: "all" },
    purpose: { type: String, required: true, trim: true, index: true },
    notes: { type: String, trim: true },
    values: { type: dailyValuePercentSchema, default: {} },
    ...reviewFlag
  },
  { timestamps: true }
);

const recipeIngredientSchema = new Schema(
  {
    name: { type: String, required: true, trim: true, index: true },
    koreanName: { type: String, trim: true, index: true },
    foodName: { type: String, trim: true, index: true },
    fdcId: { type: Number, index: true },
    category: { type: String, trim: true, index: true },
    amount: { type: Number, default: 0 },
    unit: { type: String, trim: true, default: "g" },
    preparation: { type: String, trim: true },
    optional: { type: Boolean, default: false },
    substitutes: [{ type: String, trim: true }],
    notes: { type: String, trim: true },
    ...reviewFlag
  },
  { timestamps: true }
);

const cookingStepSchema = new Schema(
  {
    order: { type: Number, required: true, min: 1 },
    title: { type: String, trim: true },
    instruction: { type: String, required: true, trim: true },
    durationMinutes: { type: Number, default: 0 },
    temperatureC: { type: Number, default: 0 },
    technique: { type: String, trim: true },
    equipment: [{ type: String, trim: true }],
    tips: { type: String, trim: true }
  },
  { _id: false }
);

const recipeNutritionSchema = new Schema(
  {
    servings: { type: Number, default: 1 },
    servingSize: { type: String, trim: true },
    caloriesPerServing: { type: Number, default: 0 },
    macrosPerServing: { type: macroSchema, default: {} },
    sourceNote: { type: String, trim: true }
  },
  { _id: false }
);

const recipeSchema = new Schema(
  {
    title: { type: String, required: true, trim: true, index: true },
    description: { type: String, required: true },
    mealType: { type: String, required: true, enum: ["breakfast", "lunch", "dinner", "snack"] },
    calories: { type: Number, required: true, min: 0 },
    prepTimeMinutes: { type: Number, default: 10 },
    cookTimeMinutes: { type: Number, default: 0 },
    totalTimeMinutes: { type: Number, default: 0 },
    difficulty: { type: String, enum: ["easy", "medium", "hard"], default: "easy" },
    cuisine: { type: String, trim: true },
    cookingMethods: [{ type: String, trim: true }],
    equipment: [{ type: String, trim: true }],
    ingredients: [{ type: String, trim: true }],
    ingredientRefs: [{ type: Schema.Types.ObjectId, ref: "RecipeIngredient" }],
    steps: [{ type: String, trim: true }],
    cookingSteps: { type: [cookingStepSchema], default: [] },
    nutrition: { type: recipeNutritionSchema, default: {} },
    macros: { type: macroSchema, default: {} },
    allergens: [{ type: String, trim: true }],
    dietUseCases: [{ type: String, trim: true }],
    cautionGroups: [{ type: String, trim: true }],
    tags: [{ type: String, trim: true }],
    imageUrl: { type: String, trim: true },
    ...reviewFlag
  },
  { timestamps: true }
);

const activitySchema = new Schema(
  {
    name: { type: String, required: true, trim: true, index: true },
    category: { type: String, required: true, trim: true, index: true },
    caloriesPerHour: { type: Number, required: true, min: 0 },
    metValue: { type: Number, required: true, min: 0 },
    icon: { type: String, trim: true },
    description: { type: String, trim: true },
    ...reviewFlag
  },
  { timestamps: true }
);

const userSchema = new Schema(
  {
    name: { type: String, required: true, trim: true },
    email: { type: String, required: true, unique: true, lowercase: true, trim: true },
    goal: { type: String, required: true },
    gender: { type: String, enum: ["male", "female", "non_binary"], required: true },
    age: { type: Number, min: 13, max: 120 },
    heightCm: { type: Number, min: 50 },
    currentWeightKg: { type: Number, min: 20 },
    targetWeightKg: { type: Number, min: 20 },
    calorieGoal: { type: Number, min: 0 },
    dietaryRestrictions: [{ type: String, trim: true }],
    medicalConditions: [{ type: String, trim: true }],
    programStage: { type: String, default: "onboarding" },
    ...reviewFlag
  },
  { timestamps: true }
);

const mealLogSchema = new Schema(
  {
    userName: { type: String, required: true, trim: true },
    date: { type: Date, required: true, index: true },
    mealType: { type: String, required: true, enum: ["breakfast", "lunch", "dinner", "snack"] },
    foodName: { type: String, required: true, trim: true },
    calories: { type: Number, required: true, min: 0 },
    macros: { type: macroSchema, default: {} },
    source: { type: String, enum: ["photo", "barcode", "search", "voice", "favorite"], default: "search" },
    photoUrl: { type: String, trim: true },
    ...reviewFlag
  },
  { timestamps: true }
);

const weightEntrySchema = new Schema(
  {
    userName: { type: String, required: true, trim: true },
    date: { type: Date, required: true, index: true },
    weightKg: { type: Number, required: true, min: 20 },
    note: { type: String, trim: true },
    ...reviewFlag
  },
  { timestamps: true }
);

const programSchema = new Schema(
  {
    title: { type: String, required: true, trim: true },
    stage: { type: String, required: true, trim: true },
    focus: { type: String, required: true, trim: true },
    description: { type: String, required: true },
    cta: { type: String, default: "Next" },
    ...reviewFlag
  },
  { timestamps: true }
);

export const Food = model("Food", foodSchema);
export const DailyValueProfile = model("DailyValueProfile", dailyValueProfileSchema);
export const RecipeIngredient = model("RecipeIngredient", recipeIngredientSchema);
export const Recipe = model("Recipe", recipeSchema);
export const Activity = model("Activity", activitySchema);
export const User = model("User", userSchema);
export const MealLog = model("MealLog", mealLogSchema);
export const WeightEntry = model("WeightEntry", weightEntrySchema);
export const Program = model("Program", programSchema);
