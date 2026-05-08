export type HealthFormulaGender = "male" | "female" | "non_binary";
export type ActivityLevel = "sedentary" | "low_active" | "active" | "very_active";
export type WeightGoal = "lose_weight" | "maintain_weight" | "gain_weight";

export type MacroRatios = {
  carbs?: number;
  fat?: number;
  protein?: number;
};

export type ActivityBurnInput = {
  name?: string;
  metValue: number;
  durationMinutes: number;
};

export type HealthMetricsInput = {
  gender: HealthFormulaGender;
  age: number;
  heightCm: number;
  weightKg: number;
  activityLevel?: ActivityLevel;
  goal?: WeightGoal;
  dailyActivityCalories?: number;
  calorieAdjustment?: number;
  macroRatios?: MacroRatios;
  activities?: ActivityBurnInput[];
};

const defaultMacroRatios = {
  carbs: 0.6,
  fat: 0.25,
  protein: 0.15
};

const activityLevelToPal: Record<ActivityLevel, number> = {
  sedentary: 1.25,
  low_active: 1.5,
  active: 1.75,
  very_active: 2.2
};

const goalAdjustment: Record<WeightGoal, number> = {
  lose_weight: -500,
  maintain_weight: 0,
  gain_weight: 500
};

function round(value: number, decimals = 1) {
  return Number(value.toFixed(decimals));
}

function assertPositive(name: string, value: number) {
  if (!Number.isFinite(value) || value <= 0) {
    throw new Error(`${name} must be a positive number.`);
  }
}

function gendered(gender: HealthFormulaGender, male: number, female: number) {
  if (gender === "male") return male;
  if (gender === "female") return female;

  // 조선말: 성별공식을 하나로 정하기 어려운 경우에는 남/녀 공식의 평균값을 씁니다.
  // English: When a binary formula cannot be selected, use the average of male and female estimates.
  return (male + female) / 2;
}

export function calculateBmi(weightKg: number, heightCm: number) {
  assertPositive("weightKg", weightKg);
  assertPositive("heightCm", heightCm);

  // 조선말: BMI는 몸무게(kg)를 키(m)의 제곱으로 나눈 값입니다.
  // English: BMI is body weight in kilograms divided by height in meters squared.
  const bmi = weightKg / (heightCm / 100) ** 2;
  let category = "normal_weight";

  if (bmi < 18.5) category = "underweight";
  else if (bmi < 25) category = "normal_weight";
  else if (bmi < 30) category = "pre_obesity";
  else if (bmi < 35) category = "obesity_class_i";
  else if (bmi < 40) category = "obesity_class_ii";
  else category = "obesity_class_iii";

  return {
    value: round(bmi),
    category
  };
}

export function calculateBmr(gender: HealthFormulaGender, age: number, heightCm: number, weightKg: number) {
  assertPositive("age", age);
  assertPositive("heightCm", heightCm);
  assertPositive("weightKg", weightKg);

  // 조선말: BMR은 안정상태에서 하루에 필요한 기초열량 추정값입니다.
  // English: BMR estimates baseline daily calories needed at rest.
  const harrisBenedict1918 = gendered(
    gender,
    66.473 + 13.7516 * weightKg + 5.0033 * heightCm - 6.755 * age,
    655.0955 + 9.5634 * weightKg + 1.8496 * heightCm - 4.6756 * age
  );

  const revisedHarrisBenedict1984 = gendered(
    gender,
    88.362 + 13.397 * weightKg + 4.799 * heightCm - 5.677 * age,
    447.593 + 9.247 * weightKg + 3.098 * heightCm - 4.33 * age
  );

  const mifflinStJeor1990 = gendered(
    gender,
    10 * weightKg + 6.25 * heightCm - 5 * age + 5,
    10 * weightKg + 6.25 * heightCm - 5 * age - 161
  );

  const schofield1985Male = (() => {
    if (age < 3) return 59.512 * weightKg - 30.4;
    if (age < 10) return 22.706 * weightKg + 504.3;
    if (age < 18) return 17.686 * weightKg + 658.2;
    if (age < 30) return 15.057 * weightKg + 692.2;
    if (age < 60) return 11.472 * weightKg + 873.1;
    return 11.711 * weightKg + 587.7;
  })();

  const schofield1985Female = (() => {
    if (age < 3) return 58.317 * weightKg - 31.1;
    if (age < 10) return 20.315 * weightKg + 485.9;
    if (age < 18) return 13.384 * weightKg + 692.6;
    if (age < 30) return 14.818 * weightKg + 486.6;
    if (age < 60) return 8.126 * weightKg + 845.6;
    return 9.082 * weightKg + 658.5;
  })();

  const schofield1985 = gendered(gender, schofield1985Male, schofield1985Female);

  return {
    harrisBenedict1918: round(harrisBenedict1918),
    revisedHarrisBenedict1984: round(revisedHarrisBenedict1984),
    mifflinStJeor1990: round(mifflinStJeor1990),
    schofield1985: round(schofield1985),
    recommended: round(mifflinStJeor1990)
  };
}

export function getPalValue(activityLevel: ActivityLevel = "low_active") {
  return activityLevelToPal[activityLevel] ?? activityLevelToPal.low_active;
}

function getPaCoefficient(gender: HealthFormulaGender, palValue: number) {
  if (palValue < 1.4) return 1;
  if (palValue < 1.6) return gendered(gender, 1.12, 1.14);
  if (palValue < 1.9) return 1.27;
  return gendered(gender, 1.54, 1.45);
}

export function calculateTdee(input: Pick<HealthMetricsInput, "gender" | "age" | "heightCm" | "weightKg" | "activityLevel">) {
  const activityLevel = input.activityLevel ?? "low_active";
  const palValue = getPalValue(activityLevel);
  const bmr = calculateBmr(input.gender, input.age, input.heightCm, input.weightKg);
  const paCoefficient = getPaCoefficient(input.gender, palValue);

  // 조선말: TDEE는 하루 전체 에네르기소비량이며, 활동수준을 기초대사량에 반영합니다.
  // English: TDEE estimates total daily energy expenditure after applying activity level.
  const who2001 = bmr.schofield1985 * palValue;
  const iom2005 = gendered(
    input.gender,
    864 - 9.72 * input.age + paCoefficient * 14.2 * input.weightKg + 503 * (input.heightCm / 100),
    387 - 7.31 * input.age + paCoefficient * 10.9 * input.weightKg + 660.7 * (input.heightCm / 100)
  );

  return {
    activityLevel,
    palValue,
    paCoefficient: round(paCoefficient, 2),
    who2001: round(who2001),
    iom2005: round(iom2005),
    recommended: round(iom2005)
  };
}

export function calculateActivityBurn(weightKg: number, activities: ActivityBurnInput[] = []) {
  assertPositive("weightKg", weightKg);

  // 조선말: 운동소모열량은 MET * 몸무게(kg) * 시간(h) 공식으로 계산합니다.
  // English: Activity calories use MET * body weight in kg * duration in hours.
  const items = activities.map((activity) => {
    assertPositive("metValue", activity.metValue);
    assertPositive("durationMinutes", activity.durationMinutes);
    const calories = activity.metValue * weightKg * activity.durationMinutes / 60;
    return {
      name: activity.name,
      metValue: activity.metValue,
      durationMinutes: activity.durationMinutes,
      calories: round(calories)
    };
  });

  return {
    items,
    totalCalories: round(items.reduce((sum, item) => sum + item.calories, 0))
  };
}

export function calculateCalorieGoal(input: {
  tdeeCalories: number;
  goal?: WeightGoal;
  dailyActivityCalories?: number;
  calorieAdjustment?: number;
}) {
  assertPositive("tdeeCalories", input.tdeeCalories);
  const goal = input.goal ?? "maintain_weight";
  const adjustment = input.calorieAdjustment ?? goalAdjustment[goal] ?? 0;
  const dailyActivityCalories = input.dailyActivityCalories ?? 0;

  // 조선말: 하루 열량목표는 TDEE에 체중목표 보정값과 운동소모열량을 더하여 정합니다.
  // English: Daily calorie goal adds goal adjustment and activity calories to TDEE.
  return {
    goal,
    tdeeCalories: round(input.tdeeCalories),
    goalAdjustmentCalories: round(adjustment),
    dailyActivityCalories: round(dailyActivityCalories),
    totalCalories: Math.max(0, round(input.tdeeCalories + adjustment + dailyActivityCalories))
  };
}

export function calculateMacroGoal(totalCalories: number, ratios: MacroRatios = {}) {
  assertPositive("totalCalories", totalCalories);

  const carbsRatio = ratios.carbs ?? defaultMacroRatios.carbs;
  const fatRatio = ratios.fat ?? defaultMacroRatios.fat;
  const proteinRatio = ratios.protein ?? defaultMacroRatios.protein;
  const ratioTotal = carbsRatio + fatRatio + proteinRatio;

  if (!Number.isFinite(ratioTotal) || ratioTotal <= 0) {
    throw new Error("macroRatios must have a positive total.");
  }

  const normalized = {
    carbs: carbsRatio / ratioTotal,
    fat: fatRatio / ratioTotal,
    protein: proteinRatio / ratioTotal
  };

  // 조선말: 매크로 목표는 열량을 탄수화물/지방/단백질 비률로 나누고 g 단위로 바꿉니다.
  // English: Macro goals split calories by carb/fat/protein ratios and convert calories to grams.
  return {
    ratios: {
      carbs: round(normalized.carbs, 3),
      fat: round(normalized.fat, 3),
      protein: round(normalized.protein, 3)
    },
    grams: {
      carbs: round((totalCalories * normalized.carbs) / 4),
      fat: round((totalCalories * normalized.fat) / 9),
      protein: round((totalCalories * normalized.protein) / 4)
    },
    calories: {
      carbs: round(totalCalories * normalized.carbs),
      fat: round(totalCalories * normalized.fat),
      protein: round(totalCalories * normalized.protein)
    }
  };
}

export function calculateHealthMetrics(input: HealthMetricsInput) {
  const activityBurn = calculateActivityBurn(input.weightKg, input.activities ?? []);
  const tdee = calculateTdee(input);
  const calorieGoal = calculateCalorieGoal({
    tdeeCalories: tdee.recommended,
    goal: input.goal,
    dailyActivityCalories: (input.dailyActivityCalories ?? 0) + activityBurn.totalCalories,
    calorieAdjustment: input.calorieAdjustment
  });

  return {
    bmi: calculateBmi(input.weightKg, input.heightCm),
    bmr: calculateBmr(input.gender, input.age, input.heightCm, input.weightKg),
    tdee,
    calorieGoal,
    macroGoal: calculateMacroGoal(calorieGoal.totalCalories, input.macroRatios),
    activityBurn,
    notes: [
      "조선말: 이 결과는 건강관리용 추정값이며 의학적 진단이나 처방이 아닙니다.",
      "English: These values are wellness estimates, not medical diagnosis or treatment."
    ]
  };
}
