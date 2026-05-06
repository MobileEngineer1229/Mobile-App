import "dotenv/config";
import { createHash } from "crypto";
import { copyFile, mkdir, readFile, rm } from "fs/promises";
import path from "path";
import { connectDatabase } from "./db.js";
import { dailyValueProfiles } from "./data/dailyValueProfiles.js";
import { DailyValueProfile, Food } from "./models/content.js";

type MaterialRecord = {
  name: string;
  alias?: string;
  introduction?: string;
  season?: string;
  effect?: string;
  body_constitution?: string;
  small_class?: string;
  big_class?: string;
};

type NutrientRecord = Record<string, string>;

type ImportRecord = {
  name: string;
  material?: MaterialRecord;
  nutrient?: NutrientRecord;
};

const dataRoot = path.resolve(process.env.FOOD_MATERIAL_ROOT || path.join(process.cwd(), "..", "food data", "food-material-master"));
const materialJsonPath = path.join(dataRoot, "json", "food_material.json");
const nutrientJsonPath = path.join(dataRoot, "json", "nutrient.json");
const sourcePicDir = path.join(dataRoot, "pic");
const outputImageDir = path.join(process.cwd(), "public", "images", "food-material");

const categoryMap: Record<string, { category: string; group: string }> = {
  "蔬菜类": { category: "vegetable", group: "plant food" },
  "蔬菜类及制品": { category: "vegetable", group: "plant food" },
  "水果类": { category: "fruit", group: "plant food" },
  "水果类及制品": { category: "fruit", group: "plant food" },
  "水产品": { category: "seafood", group: "animal food" },
  "鱼虾蟹贝类": { category: "seafood", group: "animal food" },
  "畜肉类": { category: "meat", group: "animal food" },
  "畜肉类及制品": { category: "meat", group: "animal food" },
  "禽肉类及制品": { category: "poultry", group: "animal food" },
  "禽蛋类": { category: "egg and poultry", group: "animal food" },
  "蛋类及制品": { category: "egg", group: "animal food" },
  "米面豆乳类": { category: "grain and legume", group: "plant food" },
  "谷类及制品": { category: "grain", group: "plant food" },
  "薯类、淀粉及制品": { category: "starch and tuber", group: "plant food" },
  "干豆类及制品": { category: "legume", group: "plant food" },
  "菌藻类": { category: "mushroom and algae", group: "plant food" },
  "坚果、种子类": { category: "nuts and seeds", group: "plant food" },
  "菜品": { category: "prepared food", group: "mixed food" },
  "调味品": { category: "seasoning", group: "seasoning" },
  "小吃、甜饼": { category: "snack", group: "mixed food" },
  "速食食品": { category: "convenience food", group: "mixed food" },
  "饮料类": { category: "beverage", group: "beverage" },
  "含酒精饮料": { category: "alcoholic beverage", group: "beverage" },
  "糖、果脯和蜜饯、蜂蜜": { category: "sweetener and preserved fruit", group: "sweet food" },
  "糖、果脯、蜜饯": { category: "sweetener and preserved fruit", group: "sweet food" },
  "婴幼儿食品": { category: "infant food", group: "special diet food" }
};

const exactKoreanNames: Record<string, string> = {
  "白菜": "배추",
  "油菜": "유채",
  "芹菜": "셀러리",
  "菠菜": "시금치",
  "生菜": "상추",
  "韭菜": "부추",
  "黄瓜": "오이",
  "西红柿": "토마토",
  "番茄": "토마토",
  "土豆": "감자",
  "马铃薯": "감자",
  "甘薯": "고구마",
  "红薯": "고구마",
  "小麦": "밀",
  "大米": "쌀",
  "米饭": "밥",
  "小米": "좁쌀",
  "玉米": "옥수수",
  "黄豆": "대두",
  "黑豆": "검은콩",
  "绿豆": "녹두",
  "红豆": "팥",
  "豆腐": "두부",
  "豆浆": "두유",
  "鸡蛋": "달걀",
  "鸭蛋": "오리알",
  "牛奶": "우유",
  "酸奶": "요거트",
  "苹果": "사과",
  "香蕉": "바나나",
  "梨": "배",
  "葡萄": "포도",
  "西瓜": "수박",
  "桃": "복숭아",
  "橙": "오렌지",
  "柠檬": "레몬",
  "草莓": "딸기",
  "猪肉": "돼지고기",
  "牛肉": "소고기",
  "羊肉": "양고기",
  "鸡肉": "닭고기",
  "鸭肉": "오리고기",
  "鱼": "생선",
  "虾": "새우",
  "蟹": "게",
  "海带": "다시마",
  "紫菜": "김",
  "木耳": "목이버섯",
  "香菇": "표고버섯",
  "平菇": "느타리버섯",
  "金针菇": "팽이버섯",
  "花生": "땅콩",
  "核桃": "호두",
  "杏仁": "아몬드",
  "芝麻": "참깨",
  "蜂蜜": "꿀",
  "白糖": "백설탕",
  "酱油": "간장",
  "醋": "식초",
  "盐": "소금",
  "姜": "생강",
  "大蒜": "마늘",
  "洋葱": "양파",
  "胡萝卜": "당근",
  "南瓜": "호박",
  "茄子": "가지",
  "辣椒": "고추"
};

const replacementKoreanTerms: Array<[string, string]> = Object.entries({
  "马铃薯": "감자",
  "西红柿": "토마토",
  "胡萝卜": "당근",
  "小麦粉": "밀가루",
  "面粉": "밀가루",
  "面条": "국수",
  "挂面": "건면",
  "馒头": "찐빵",
  "米饭": "밥",
  "大米": "쌀",
  "糯米": "찹쌀",
  "黑米": "흑미",
  "玉米": "옥수수",
  "小米": "좁쌀",
  "黄豆": "대두",
  "黑豆": "검은콩",
  "绿豆": "녹두",
  "红豆": "팥",
  "豆腐": "두부",
  "豆浆": "두유",
  "牛奶": "우유",
  "酸奶": "요거트",
  "奶粉": "분유",
  "奶酪": "치즈",
  "鸡蛋": "달걀",
  "鸭蛋": "오리알",
  "鸡": "닭",
  "鸭": "오리",
  "鹅": "거위",
  "猪肉": "돼지고기",
  "猪": "돼지",
  "牛肉": "소고기",
  "牛": "소",
  "羊肉": "양고기",
  "羊": "양",
  "鱼": "생선",
  "虾": "새우",
  "蟹": "게",
  "贝": "조개",
  "海带": "다시마",
  "紫菜": "김",
  "苹果": "사과",
  "香蕉": "바나나",
  "葡萄": "포도",
  "西瓜": "수박",
  "草莓": "딸기",
  "柠檬": "레몬",
  "橙": "오렌지",
  "梨": "배",
  "桃": "복숭아",
  "白菜": "배추",
  "菠菜": "시금치",
  "生菜": "상추",
  "韭菜": "부추",
  "芹菜": "셀러리",
  "黄瓜": "오이",
  "甘薯": "고구마",
  "红薯": "고구마",
  "土豆": "감자",
  "洋葱": "양파",
  "大蒜": "마늘",
  "辣椒": "고추",
  "南瓜": "호박",
  "茄子": "가지",
  "花生": "땅콩",
  "核桃": "호두",
  "杏仁": "아몬드",
  "芝麻": "참깨",
  "蜂蜜": "꿀",
  "白糖": "백설탕",
  "红糖": "흑설탕",
  "酱油": "간장",
  "醋": "식초",
  "盐": "소금",
  "油": "기름",
  "干": "건조",
  "鲜": "신선",
  "粉": "가루",
  "片": "편",
  "仁": "알맹이",
  "皮": "껍질",
  "汤": "탕",
  "粥": "죽",
  "饭": "밥",
  "肉": "고기"
}).sort((a, b) => b[0].length - a[0].length);

const zeroDailyValues = {
  calories: 0, protein: 0, carbs: 0, fat: 0, saturatedFat: 0, fiber: 0, sugar: 0, sodium: 0,
  cholesterol: 0, calcium: 0, iron: 0, magnesium: 0, potassium: 0, zinc: 0, vitaminA: 0,
  vitaminB1: 0, vitaminB2: 0, vitaminB3: 0, vitaminB6: 0, vitaminB12: 0, vitaminC: 0,
  vitaminD: 0, vitaminE: 0, vitaminK: 0, folate: 0
};

function clean(value = "") {
  return value.replace(/\s+/g, " ").trim();
}

function num(value: string | undefined) {
  const parsed = Number(clean(value || "").replace(/[^\d.-]/g, ""));
  return Number.isFinite(parsed) ? parsed : 0;
}

function hash(value: string) {
  return createHash("sha1").update(value).digest("hex").slice(0, 12);
}

function slugify(value: string) {
  return clean(value).toLowerCase().replace(/[^a-z0-9]+/g, "-").replace(/(^-|-$)/g, "").slice(0, 40) || "food";
}

function baseName(value: string) {
  return clean(value).replace(/[（(［\[].*?[）)］\]]/g, "");
}

function koreanName(chineseName: string) {
  const direct = exactKoreanNames[chineseName] || exactKoreanNames[baseName(chineseName)];
  if (direct) return direct;

  let result = chineseName;
  for (const [source, target] of replacementKoreanTerms) {
    result = result.split(source).join(target);
  }

  result = result
    .replace(/[（(]/g, " (")
    .replace(/[）)]/g, ")")
    .replace(/，/g, ", ")
    .replace(/均值/g, "평균")
    .replace(/标准/g, "표준")
    .replace(/特制/g, "특제")
    .replace(/煮/g, "삶은")
    .replace(/蒸/g, "찐")
    .replace(/烤/g, "구운")
    .replace(/炒/g, "볶은");

  return clean(result);
}

function containsCjk(value: string) {
  return /[\u4e00-\u9fff]/u.test(value);
}

const koreanCategoryNames: Record<string, string> = {
  vegetable: "채소",
  fruit: "과일",
  seafood: "해산물",
  meat: "육류",
  poultry: "가금류",
  "egg and poultry": "가금류 및 알류",
  egg: "알류",
  "grain and legume": "곡류 및 두류",
  grain: "곡류",
  "starch and tuber": "전분 및 구근류",
  legume: "두류",
  "mushroom and algae": "버섯 및 해조류",
  "nuts and seeds": "견과 및 종자류",
  "prepared food": "조리식품",
  seasoning: "조미료",
  snack: "간식",
  "convenience food": "간편식",
  beverage: "음료",
  "alcoholic beverage": "주류",
  "sweetener and preserved fruit": "당류 및 절임과일",
  "infant food": "영유아식",
  food: "식품"
};

const koreanSubgroupNames: Record<string, string> = {
  "茎叶类": "잎줄기류",
  "根茎类": "뿌리줄기류",
  "瓜果类": "과채류",
  "鲜豆类": "신선두류",
  "菌藻类": "버섯해조류",
  "仁果类": "인과류",
  "核果类": "핵과류",
  "浆果类": "장과류",
  "柑橘类": "감귤류",
  "瓜类": "박과류",
  "热带水果": "열대과일",
  "畜肉类": "축산육류",
  "禽肉类": "가금육류",
  "蛋类": "알류",
  "鱼类": "어류",
  "虾类": "새우류",
  "蟹类": "게류",
  "贝类": "조개류",
  "谷类及制品": "곡류 및 제품",
  "薯类、淀粉及制品": "서류 전분 제품",
  "干豆类及制品": "건두류 및 제품",
  "水果类及制品": "과일 및 제품",
  "坚果、种子类": "견과 종자류"
};

function resolvedKoreanName(chineseName: string, classification: ReturnType<typeof classify>) {
  const translated = koreanName(chineseName);
  if (!containsCjk(translated)) return translated;

  const category = koreanCategoryNames[classification.category] || "식품";
  const subgroup = koreanSubgroupNames[classification.foodSubgroup] || "";
  return clean(`${subgroup || category} 식재료 ${hash(chineseName).slice(0, 6)}`);
}

function koreanCaution(value = "") {
  const original = clean(value);
  if (!original) return "";

  const direct: Record<string, string> = {
    "阳虚质体质应忌食或少食": "양허 체질은 피하거나 적게 섭취해야 합니다.",
    "一般人群均可食用": "일반적으로 대부분의 사람이 섭취할 수 있습니다.",
    "平和质体质；一般人群均可食用。": "평화 체질 및 일반인은 섭취할 수 있습니다.",
    "麻疹后、疮疥、目疾者不宜食;": "홍역 후, 피부 종기나 눈 질환이 있는 사람은 섭취를 피하는 것이 좋습니다."
  };
  if (direct[original]) return direct[original];

  let result = original;
  const replacements: Array<[RegExp, string]> = [
    [/一般人群/g, "일반인"],
    [/均可食用/g, "섭취 가능"],
    [/不宜食用|不宜食|忌食/g, "섭취 주의"],
    [/少食/g, "적게 섭취"],
    [/慎食/g, "주의해서 섭취"],
    [/过敏/g, "알레르기"],
    [/体质/g, "체질"],
    [/阳虚质/g, "양허"],
    [/阴虚质/g, "음허"],
    [/气虚质/g, "기허"],
    [/痰湿质/g, "담습"],
    [/湿热质/g, "습열"],
    [/血瘀质/g, "혈어"],
    [/特禀质/g, "특이체질"],
    [/孕妇/g, "임산부"],
    [/儿童/g, "어린이"],
    [/老人/g, "노인"],
    [/糖尿病/g, "당뇨병"],
    [/高血压/g, "고혈압"],
    [/痛风/g, "통풍"],
    [/肾病/g, "신장 질환"],
    [/腹泻/g, "설사"],
    [/便秘/g, "변비"],
    [/胃/g, "위"],
    [/肝/g, "간"],
    [/肾/g, "신장"],
    [/应/g, ""],
    [/或/g, "또는"],
    [/者/g, "사람"],
    [/；|;/g, "; "],
    [/，|、/g, ", "],
    [/。/g, "."]
  ];
  for (const [pattern, replacement] of replacements) {
    result = result.replace(pattern, replacement);
  }

  result = clean(result);
  if (!result || containsCjk(result)) {
    return `주의사항 번역 검토 필요 ${hash(original).slice(0, 6)}`;
  }
  return result;
}

function splitList(value = "") {
  return clean(value)
    .split(/[、,，;；]/)
    .map((item) => clean(item))
    .filter(Boolean)
    .slice(0, 20);
}

function classify(material?: MaterialRecord, nutrient?: NutrientRecord) {
  const sourceCategory = clean(material?.big_class || nutrient?.["食物类别"] || "food");
  const mapped = categoryMap[sourceCategory] || { category: sourceCategory || "food", group: "food" };
  return {
    category: mapped.category,
    foodGroup: mapped.group,
    foodSubgroup: clean(material?.small_class || nutrient?.["食物类别"] || sourceCategory || "general"),
    sourceCategory
  };
}

function inferAllergens(name: string, category: string) {
  const text = `${name} ${category}`;
  const values: string[] = [];
  if (/牛奶|酸奶|奶粉|奶酪|乳/.test(text)) values.push("milk");
  if (/鸡蛋|鸭蛋|蛋/.test(text)) values.push("egg");
  if (/鱼|三文鱼|鳕|鲑|鲔|带鱼|鲫|鲤/.test(text)) values.push("fish");
  if (/虾|蟹|龙虾|螃蟹/.test(text)) values.push("crustacean shellfish");
  if (/贝|蛤|蛏|牡蛎|扇贝|鲍/.test(text)) values.push("mollusk shellfish");
  if (/花生/.test(text)) values.push("peanut");
  if (/核桃|杏仁|腰果|榛子|松子|开心果|坚果/.test(text)) values.push("tree nuts");
  if (/小麦|面粉|面条|馒头|挂面|麦/.test(text)) values.push("wheat");
  if (/黄豆|黑豆|豆腐|豆浆|豆乳|大豆/.test(text)) values.push("soy");
  return [...new Set(values)];
}

function dietUseCases(calories: number, protein: number, fat: number, carbs: number, fiber: number, sugar: number, sodium: number) {
  const values: string[] = [];
  if (calories <= 120 || fiber >= 5) values.push("weight_loss");
  if (sugar <= 5 && fiber >= 2) values.push("diabetes_management");
  if (protein >= 15) values.push("athlete", "muscle_gain");
  if (fiber >= 5) values.push("high_fiber_diet");
  if (sodium <= 50) values.push("low_sodium");
  if (fat >= 10 && carbs <= 10) values.push("low_carb");
  if (carbs >= 20 && fat <= 3) values.push("pre_workout_energy");
  return [...new Set(values)];
}

function pct(value: number, dailyValue: number) {
  return Number((((value || 0) / dailyValue) * 100).toFixed(1));
}

function percentFor(values: typeof dailyValueProfiles[number]["values"], food: typeof zeroDailyValues) {
  return Object.fromEntries(Object.entries(food).map(([key, value]) => [key, pct(value, values[key as keyof typeof values] || 1)]));
}

async function loadJson<T>(filePath: string) {
  return JSON.parse(await readFile(filePath, "utf8")) as T;
}

async function copyImageIfExists(name: string, picMap: Map<string, string>) {
  const source = picMap.get(`${name}.jpg`);
  if (!source) return "";

  await mkdir(outputImageDir, { recursive: true });
  const fileName = `material-${hash(name)}-${slugify(koreanName(name))}.jpg`;
  const target = path.join(outputImageDir, fileName);
  await copyFile(source, target);
  return `/images/food-material/${fileName}`;
}

function buildFood(record: ImportRecord, imageUrl: string) {
  const { material, nutrient } = record;
  const classification = classify(material, nutrient);
  const koName = resolvedKoreanName(record.name, classification);
  const chineseCaution = clean(material?.body_constitution || "");
  const calories = num(nutrient?.["能量（千卡）"]);
  const protein = num(nutrient?.["蛋白质（克）"]);
  const fat = num(nutrient?.["脂肪（克）"]);
  const carbs = num(nutrient?.["碳水化合物（克）"]);
  const fiber = num(nutrient?.["膳食纤维（克）"]);
  const sodium = num(nutrient?.["钠（毫克）"]);
  const saturatedFat = num(nutrient?.["饱和脂肪酸（克）"]);
  const monoFat = num(nutrient?.["单不饱和脂肪酸（克）"]);
  const polyFat = num(nutrient?.["多不饱和脂肪酸（克）"]);
  const cholesterol = num(nutrient?.["胆固醇（毫克）"]);
  const sugar = 0;
  const dvBase = {
    ...zeroDailyValues,
    calories,
    protein,
    carbs,
    fat,
    saturatedFat,
    fiber,
    sugar,
    sodium,
    cholesterol,
    calcium: num(nutrient?.["钙（毫克）"]),
    iron: num(nutrient?.["铁（毫克）"]),
    magnesium: num(nutrient?.["镁（毫克）"]),
    potassium: num(nutrient?.["钾（毫克）"]),
    zinc: num(nutrient?.["锌（毫克）"]),
    vitaminA: num(nutrient?.["维生素A（微克RE）"]),
    vitaminB1: num(nutrient?.["硫胺素（毫克）"]),
    vitaminB2: num(nutrient?.["核黄素（毫克）"]),
    vitaminB3: num(nutrient?.["烟酸（毫克）"]),
    vitaminB6: num(nutrient?.["维生素B6（毫克）"]),
    vitaminB12: num(nutrient?.["维生素B12（微克）"]),
    vitaminC: num(nutrient?.["维生素C（毫克）"]),
    vitaminE: num(nutrient?.["维生素E（毫克）"]),
    folate: num(nutrient?.["叶酸（微克）"])
  };
  const allergenList = inferAllergens(record.name, classification.sourceCategory);

  return {
    name: koName,
    koreanName: koName,
    chineseName: record.name,
    dataType: "food-material-master",
    category: classification.category,
    foodGroup: classification.foodGroup,
    foodSubgroup: classification.foodSubgroup,
    servingSize: "100 g edible portion",
    calories,
    macros: { protein, fat, carbs, fiber },
    saturatedFat,
    transFat: 0,
    unsaturatedFat: Number((monoFat + polyFat).toFixed(2)),
    omega3: 0,
    omega6: 0,
    cholesterolMg: cholesterol,
    sugar,
    glycemicIndex: 0,
    oiliness: fat >= 20 ? "very high" : fat >= 10 ? "high" : fat >= 3 ? "medium" : "low",
    oilinessScore: fat >= 20 ? 5 : fat >= 10 ? 4 : fat >= 3 ? 2 : 0,
    vitamins: {
      vitaminA: dvBase.vitaminA,
      vitaminB1: dvBase.vitaminB1,
      vitaminB2: dvBase.vitaminB2,
      vitaminB3: dvBase.vitaminB3,
      vitaminB6: dvBase.vitaminB6,
      vitaminB12: dvBase.vitaminB12,
      vitaminC: dvBase.vitaminC,
      vitaminD: 0,
      vitaminE: dvBase.vitaminE,
      vitaminK: 0,
      folate: dvBase.folate
    },
    minerals: {
      calcium: dvBase.calcium,
      iron: dvBase.iron,
      magnesium: dvBase.magnesium,
      potassium: dvBase.potassium,
      sodium: dvBase.sodium,
      zinc: dvBase.zinc
    },
    bestTimeToEat: [],
    goodPairings: [],
    avoidPairings: [],
    cautionGroups: splitList(material?.body_constitution),
    cautions: chineseCaution,
    koreanCautions: koreanCaution(chineseCaution),
    benefits: clean([material?.introduction, material?.effect].filter(Boolean).join("\n\n")),
    allergens: allergenList,
    ingredients: [record.name],
    additives: [],
    dailyValuePercent: percentFor(dailyValueProfiles[0].values, dvBase),
    dailyValuePercentByProfile: dailyValueProfiles.map((profile) => ({
      profileKey: profile.profileKey,
      label: profile.label,
      purpose: profile.purpose,
      values: percentFor(profile.values, dvBase)
    })),
    dietUseCases: dietUseCases(calories, protein, fat, carbs, fiber, sugar, sodium),
    dietUseNote: "Per-100g values imported from food-material-master. Korean names are generated from a local Chinese-to-Korean food glossary and should be admin-reviewed for uncommon items.",
    dataSource: "food-material-master",
    sourceNote: JSON.stringify({
      originalChineseName: record.name,
      alias: material?.alias || "",
      season: material?.season || "",
      sourceCategory: classification.sourceCategory,
      ediblePortionPercent: nutrient?.["食部（%）"] || "",
      waterG: nutrient?.["水分（克）"] || "",
      ashG: nutrient?.["灰分（克）"] || "",
      phosphorusMg: nutrient?.["磷（毫克）"] || "",
      seleniumUg: nutrient?.["硒（微克）"] || "",
      copperMg: nutrient?.["铜（毫克）"] || "",
      iodineUg: nutrient?.["碘（微克）"] || "",
      manganeseMg: nutrient?.["锰（毫克）"] || "",
      caroteneUg: nutrient?.["胡萝卜素（微克）"] || "",
      monoUnsaturatedFatG: monoFat,
      polyUnsaturatedFatG: polyFat
    }),
    tags: [classification.category, classification.foodSubgroup, classification.sourceCategory, ...(material?.season ? splitList(material.season) : [])].filter(Boolean),
    imageUrl,
    imageSource: imageUrl ? "food-material-master local pic" : "",
    imageSourceUrl: imageUrl,
    imageLicense: "food-material-master repository asset",
    imageStatus: imageUrl ? "downloaded" : "missing",
    doctor_verified: false
  };
}

await connectDatabase();

const materials = await loadJson<MaterialRecord[]>(materialJsonPath);
const nutrients = await loadJson<NutrientRecord[]>(nutrientJsonPath);
const picFiles = await import("fs").then((fs) => fs.readdirSync(sourcePicDir));
const picMap = new Map(picFiles.filter((file) => file.toLowerCase().endsWith(".jpg")).map((file) => [file, path.join(sourcePicDir, file)]));
const records = new Map<string, ImportRecord>();

for (const material of materials) {
  records.set(material.name, { name: material.name, material });
}

for (const nutrient of nutrients) {
  const name = clean(nutrient["食物名称"]);
  if (!name) continue;
  const existing = records.get(name);
  if (existing?.nutrient) continue;
  records.set(name, { name, material: existing?.material, nutrient });
}

await rm(outputImageDir, { recursive: true, force: true });
await mkdir(outputImageDir, { recursive: true });

for (const profile of dailyValueProfiles) {
  await DailyValueProfile.updateOne({ profileKey: profile.profileKey }, { $set: profile }, { upsert: true });
}

const deleted = await Food.deleteMany({});
console.log(`Deleted ${deleted.deletedCount} existing foods.`);

const docs = [];
let imageCount = 0;
for (const record of records.values()) {
  const imageUrl = await copyImageIfExists(record.name, picMap);
  if (imageUrl) imageCount += 1;
  docs.push(buildFood(record, imageUrl));
}

if (docs.length) {
  await Food.insertMany(docs, { ordered: false });
}

console.log(`Imported ${docs.length} food-material-master foods.`);
console.log(`Copied ${imageCount} local food images to ${outputImageDir}.`);
process.exit(0);
