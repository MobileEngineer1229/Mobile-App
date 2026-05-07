import { readFileSync } from "fs";
import path from "path";

function clean(value = "") {
  return value.replace(/\s+/g, " ").trim();
}

function stripVariant(value: string) {
  return clean(value).replace(/[（(［[].*?[）)］\]]/g, "");
}

const exactNames: Record<string, string> = {
  白菜: "배추",
  油菜: "유채",
  芹菜: "셀러리",
  菠菜: "시금치",
  韭菜: "부추",
  生菜: "상추",
  黄瓜: "오이",
  西红柿: "도마도",
  番茄: "도마도",
  土豆: "감자",
  马铃薯: "감자",
  甘薯: "단감자",
  红薯: "단감자",
  大米: "쌀",
  米饭: "밥",
  小米: "조",
  玉米: "강냉이",
  小麦: "밀",
  黄豆: "대두",
  黑豆: "검은콩",
  绿豆: "녹두",
  红豆: "팥",
  豆腐: "두부",
  豆浆: "두유",
  鸡蛋: "닭알",
  鸭蛋: "오리알",
  牛奶: "소젖",
  酸奶: "요구르트",
  苹果: "사과",
  香蕉: "바나나",
  梨: "배",
  葡萄: "포도",
  西瓜: "수박",
  桃: "복숭아",
  橙: "오렌지",
  柠檬: "레몬",
  草莓: "딸기",
  猪肉: "돼지고기",
  牛肉: "소고기",
  羊肉: "양고기",
  鸡肉: "닭고기",
  鸭肉: "오리고기",
  鱼: "생선",
  虾: "새우",
  蟹: "게",
  海带: "다시마",
  紫菜: "김",
  木耳: "목이버섯",
  香菇: "표고버섯",
  平菇: "느타리버섯",
  金针菇: "팽이버섯",
  花生: "락화생",
  核桃: "호두",
  杏仁: "편도",
  芝麻: "참깨",
  蜂蜜: "꿀",
  白糖: "백설탕",
  红糖: "흑설탕",
  酱油: "간장",
  醋: "식초",
  盐: "소금",
  姜: "생강",
  大蒜: "마늘",
  洋葱: "양파",
  胡萝卜: "홍당무",
  南瓜: "호박",
  茄子: "가지",
  辣椒: "고추"
};

let referenceNames: Record<string, string> | null = null;

function loadReferenceNames() {
  if (referenceNames) return referenceNames;

  const referencePath = path.resolve(process.env.FOOD_TRANSLATION_REFERENCE || path.join(process.cwd(), "..", "reference.md"));
  const parsed: Record<string, string> = {};

  try {
    const content = readFileSync(referencePath, "utf8");

    // Parse JSON-style "Chinese": "Korean" entries
    const jsonPattern = /"([^"]+)"\s*:\s*"([^"]+)"/g;
    let match: RegExpExecArray | null;
    while ((match = jsonPattern.exec(content))) {
      parsed[clean(match[1])] = clean(match[2]);
    }

    // Parse arrow-style "Chinese → Korean" entries (comments after ( stripped)
    // Handles: 白菜 → 배추, 芹菜 → 셀러리 (comment...), 山楂脯 → 산사포
    const arrowPattern = /^([^\n→"]+?)\s*→\s*([^\n(（]+)/gm;
    while ((match = arrowPattern.exec(content))) {
      const chinese = clean(match[1]);
      const korean = clean(match[2].split(/[（(]/)[0]);
      if (chinese && korean && /[一-鿿]/.test(chinese)) {
        // Don't overwrite a higher-quality JSON entry
        if (!parsed[chinese]) parsed[chinese] = korean;
      }
    }
  } catch (error) {
    console.warn(`Could not load food translation reference at ${referencePath}: ${error instanceof Error ? error.message : String(error)}`);
  }

  referenceNames = parsed;
  return referenceNames;
}

const phraseMap: Array<[string, string]> = Object.entries({
  西红柿: "도마도",
  番茄: "도마도",
  马铃薯: "감자",
  胡萝卜: "홍당무",
  小麦粉: "밀가루",
  面粉: "밀가루",
  面条: "국수",
  挂面: "건면",
  馒头: "찐빵",
  包子: "만두",
  饺子: "교자",
  米饭: "밥",
  大米: "쌀",
  糯米: "찹쌀",
  黑米: "흑미",
  玉米: "강냉이",
  小米: "조",
  燕麦: "귀리",
  荞麦: "메밀",
  高粱: "수수",
  黄豆: "대두",
  黑豆: "검은콩",
  绿豆: "녹두",
  红豆: "팥",
  豆腐: "두부",
  豆浆: "두유",
  豆芽: "콩나물",
  牛奶: "소젖",
  酸奶: "요구르트",
  奶粉: "가루젖",
  奶酪: "젖가공품",
  鸡蛋: "닭알",
  鸭蛋: "오리알",
  鸡肉: "닭고기",
  鸭肉: "오리고기",
  鹅肉: "거위고기",
  猪肉: "돼지고기",
  牛肉: "소고기",
  羊肉: "양고기",
  鱼肉: "생선살",
  海带: "다시마",
  紫菜: "김",
  苹果: "사과",
  香蕉: "바나나",
  葡萄: "포도",
  西瓜: "수박",
  草莓: "딸기",
  柠檬: "레몬",
  橙子: "오렌지",
  橘子: "귤",
  梨: "배",
  桃: "복숭아",
  杏肉: "살구 과일살",
  杏干: "말린 살구",
  西梅: "서양자두",
  李子: "자두",
  枣: "대추",
  白菜: "배추",
  菠菜: "시금치",
  生菜: "상추",
  韭菜: "부추",
  芹菜: "셀러리",
  黄瓜: "오이",
  甘薯: "단감자",
  红薯: "단감자",
  土豆: "감자",
  洋葱: "양파",
  大蒜: "마늘",
  辣椒: "고추",
  南瓜: "호박",
  茄子: "가지",
  花生: "락화생",
  核桃: "호두",
  杏仁: "편도",
  芝麻: "참깨",
  蜂蜜: "꿀",
  白糖: "백설탕",
  红糖: "흑설탕",
  酱油: "간장",
  食醋: "식초",
  醋: "식초",
  食盐: "소금",
  九制: "구제",
  蜜饯: "꿀절임",
  果脯: "과일절임",
  果干: "건과일",
  脱水: "건조",
  速冻: "랭동",
  罐头: "통졸임",
  鲜: "신선",
  干: "건조",
  熟: "익힌",
  生: "생",
  煮: "삶은",
  蒸: "찐",
  烤: "구운",
  炒: "볶은",
  炸: "튀긴",
  卤: "조림",
  酱: "장",
  咸: "염장",
  甜: "단",
  酸: "신",
  辣: "매운",
  粉: "가루",
  片: "편",
  仁: "알맹이",
  皮: "껍질",
  汤: "탕",
  粥: "죽",
  饭: "밥",
  肉: "과육",
  油: "기름"
}).sort((a, b) => b[0].length - a[0].length);

const charMap: Record<string, string> = {
  白: "흰", 菜: "채소", 油: "기름", 芹: "셀러리", 菠: "시금치", 韭: "부추", 生: "생", 黄: "노란",
  瓜: "박", 西: "서양", 红: "붉은", 柿: "감", 番: "도마도", 茄: "가지", 土: "흙", 豆: "콩",
  甘: "단", 薯: "서류", 米: "쌀", 饭: "밥", 小: "작은", 玉: "강냉이", 麦: "밀", 黑: "검은",
  绿: "녹색", 腐: "두부", 浆: "즙", 鸡: "닭", 鸭: "오리", 蛋: "알", 牛: "소", 奶: "우유",
  酸: "신", 苹: "사과", 果: "과일", 香: "향", 蕉: "바나나", 梨: "배", 葡: "포도", 萄: "포도",
  桃: "복숭아", 橙: "오렌지", 柠: "레몬", 檬: "레몬", 草: "풀", 莓: "딸기", 猪: "돼지",
  羊: "양", 肉: "과육", 鱼: "생선", 虾: "새우", 蟹: "게", 海: "바다", 紫: "자색",
  木: "목이", 菇: "버섯", 平: "느타리", 金: "금", 针: "침", 花: "꽃", 核: "핵",
  杏: "살구", 芝: "깨", 麻: "참깨", 蜂: "벌", 蜜: "꿀", 糖: "설탕", 酱: "장", 醋: "식초",
  盐: "소금", 姜: "생강", 蒜: "마늘", 洋: "양", 葱: "파", 胡: "호", 萝: "무", 卜: "복",
  南: "남", 辣: "매운", 椒: "고추", 清: "청", 凉: "량", 九: "구", 制: "가공", 梅: "매실",
  李: "자두", 子: "자", 枣: "대추", 脯: "절임", 饯: "절임", 仁: "알맹이", 干: "건조",
  鲜: "신선", 熟: "익힌", 炒: "볶은", 烤: "구운", 蒸: "찐", 煮: "삶은", 炸: "튀긴",
  卤: "조림", 咸: "짠", 甜: "단", 粉: "가루", 片: "편", 皮: "껍질", 汤: "탕", 粥: "죽",
  藻: "해조", 贝: "조개", 蚌: "조개", 螺: "소라", 鳝: "장어", 鳗: "장어", 鳕: "대구",
  鲫: "붕어", 鲤: "잉어", 鳜: "쏘가리", 鲢: "연어", 鳙: "어류", 带: "갈치", 鳖: "자라",
  鹅: "거위", 鹌: "메추라기", 鹑: "메추라기", 乳: "유", 酪: "젖가공품", 燕: "귀리", 荞: "메밀",
  粱: "수수", 薏: "율무", 莲: "연", 藕: "연근", 竹: "죽순", 笋: "죽순", 蘑: "버섯",
  银: "은", 耳: "목이버섯", 葵: "해바라기", 瓣: "조각", 根: "뿌리", 茎: "줄기", 叶: "잎"
};

const categoryFallbacks: Record<string, string> = {
  vegetable: "남새",
  fruit: "과일",
  seafood: "바다산물",
  meat: "육류",
  poultry: "가금류",
  egg: "알류",
  "egg and poultry": "가금류 및 알류",
  grain: "곡류",
  "grain and legume": "곡류 및 두류",
  "starch and tuber": "서류 및 전분류",
  legume: "두류",
  "mushroom and algae": "버섯 및 해조류",
  "nuts and seeds": "견과 및 종자류",
  "prepared food": "료리식품",
  seasoning: "조미료",
  snack: "군음식",
  beverage: "음료",
  "alcoholic beverage": "술류",
  "sweetener and preserved fruit": "당류 및 절임과일",
  "infant food": "영유아식"
};

function hasCjk(value: string) {
  return /[㐀-鿿]/u.test(value);
}

function cleanup(value: string) {
  return clean(
    value
      .replace(/[，、]/g, " ")
      .replace(/[（）]/g, " ")
      .replace(/[()]/g, " ")
      .replace(/[;；:：]/g, " ")
      .replace(/\s+/g, " ")
  );
}

const dprkTermMap: Array<[string, string]> = Object.entries({
  요리: "료리",
  조리: "료리",
  식재료: "료리감",
  재료: "료리감",
  채소: "남새",
  야채: "남새",
  달걀: "닭알",
  계란: "닭알",
  양배추: "가두배추",
  콜리플라워: "꽃가두배추",
  브로콜리: "푸른꽃가두배추",
  토마토: "도마도",
  방울토마토: "방울도마도",
  체리토마토: "방울도마도",
  셀러리: "셀레리",
  파프리카: "단고추",
  피망: "단고추",
  고구마: "단감자",
  주키니호박: "서양호박",
  애호박: "어린호박",
  오렌지: "오렌지",
  요거트: "요구르트",
  요구르트: "요구르트",
  치즈: "젖가공품",
  버터: "빠다",
  마가린: "마가린",
  우유: "소젖",
  분유: "가루젖",
  쇠고기: "소고기",
  돼지고기: "돼지고기",
  닭고기: "닭고기",
  오리고기: "오리고기",
  해산물: "바다산물",
  해조류: "바다말류",
  다시마: "다시마",
  김: "김",
  통조림: "통졸임",
  냉동: "랭동",
  냉장: "랭장",
  국수: "국수",
  만두: "만두",
  간식: "군음식",
  주류: "술류",
  주스: "과일단물",
  쥬스: "과일단물",
  소스: "양념즙",
  드레싱: "양념즙",
  오일: "기름",
  샐러드: "랭채",
  아몬드: "편도",
  캐슈너트: "카슈견과",
  땅콩: "락화생",
  옥수수: "강냉이",
  당근: "홍당무",
  무청: "무잎",
  쪽파: "실파",
  대파: "파",
  참치: "다랑어",
  고등어: "고등어",
  연어: "련어",
  과육: "과일살",
  식품: "식료품"
}).sort((a, b) => b[0].length - a[0].length);

export function normalizeDprkKoreanName(value = "") {
  let result = clean(value);
  for (const [source, target] of dprkTermMap) {
    result = result.split(source).join(target);
  }
  return clean(result);
}

export function translateChineseFoodName(chineseName = "", category = "food") {
  const original = clean(chineseName);
  if (!original) return categoryFallbacks[category] || "식품";

  const base = stripVariant(original);
  const reference = loadReferenceNames();
  const direct = reference[original] || reference[base] || exactNames[original] || exactNames[base];
  if (direct) return normalizeDprkKoreanName(direct);

  let translated = original;
  const referencePhrases = Object.entries(reference).sort((a, b) => b[0].length - a[0].length);
  for (const [source, target] of referencePhrases) {
    translated = translated.split(source).join(` ${target} `);
  }
  for (const [source, target] of phraseMap) {
    translated = translated.split(source).join(` ${target} `);
  }

  if (hasCjk(translated)) {
    translated = Array.from(translated)
      .map((char) => {
        if (/[㐀-鿿]/u.test(char)) return charMap[char] ? ` ${charMap[char]} ` : " ";
        return char;
      })
      .join("");
  }

  const result = cleanup(translated);
  if (result) return normalizeDprkKoreanName(result);

  return normalizeDprkKoreanName(`${categoryFallbacks[category] || "식품"} 료리감`);
}

export function referenceTranslationCount() {
  return Object.keys(loadReferenceNames()).length;
}

export function hasReferenceTranslation(chineseName = "") {
  const original = clean(chineseName);
  const base = stripVariant(original);
  const reference = loadReferenceNames();
  return Boolean(reference[original] || reference[base]);
}
