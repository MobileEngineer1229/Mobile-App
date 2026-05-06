-- Migration 003: Calorie Analysis System
-- 칼로리 분석 시스템

-- 1. 식품 데이터베이스 (Food Database)
CREATE TABLE IF NOT EXISTS foods (
    id SERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    name_ko VARCHAR(255),
    category VARCHAR(100) NOT NULL,
    -- '곡류', '채소류', '과일류', '육류', '어패류', '유제품', '두류', '유지류', '음료', '간식'
    calories_per_100g DECIMAL(8, 2) NOT NULL,   -- kcal
    protein_per_100g  DECIMAL(8, 2) DEFAULT 0,  -- g
    fat_per_100g      DECIMAL(8, 2) DEFAULT 0,  -- g
    carbs_per_100g    DECIMAL(8, 2) DEFAULT 0,  -- g
    fiber_per_100g    DECIMAL(8, 2) DEFAULT 0,  -- g
    sugar_per_100g    DECIMAL(8, 2) DEFAULT 0,  -- g
    sodium_per_100g   DECIMAL(8, 2) DEFAULT 0,  -- mg
    calcium_per_100g  DECIMAL(8, 2) DEFAULT 0,  -- mg
    iron_per_100g     DECIMAL(8, 2) DEFAULT 0,  -- mg
    is_baby_food      BOOLEAN DEFAULT false,     -- 이유식/아기 식품 여부
    min_age_months    INTEGER,                   -- 섭취 가능 최소 월령
    allergens         TEXT[] DEFAULT '{}',       -- 알레르기 유발 성분
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 2. 식사 기록 (Food Intake Log)
CREATE TABLE IF NOT EXISTS food_intake_logs (
    id SERIAL PRIMARY KEY,
    baby_id   INTEGER REFERENCES babies(id) ON DELETE CASCADE,
    user_id   INTEGER NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    food_id   INTEGER NOT NULL REFERENCES foods(id),
    log_date  DATE NOT NULL DEFAULT CURRENT_DATE,
    meal_type VARCHAR(20) NOT NULL DEFAULT 'snack',
    -- 'breakfast'(아침), 'lunch'(점심), 'dinner'(저녁), 'snack'(간식), 'breastmilk'(모유), 'formula'(분유)
    amount_g  DECIMAL(8, 2) NOT NULL,           -- 섭취량 (g 또는 ml)
    -- 계산 필드 (트리거 없이 직접 계산)
    calories  DECIMAL(8, 2) GENERATED ALWAYS AS (amount_g / 100.0 * 0) STORED, -- 아래 뷰에서 계산
    notes     TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
-- calories는 뷰로 계산 (GENERATED ALWAYS with subquery 불가)
ALTER TABLE food_intake_logs DROP COLUMN IF EXISTS calories;
ALTER TABLE food_intake_logs ADD COLUMN calories DECIMAL(8, 2);

-- 3. 나이별 권장 칼로리 (Age-based Calorie Recommendations, 한국 영양섭취기준)
CREATE TABLE IF NOT EXISTS calorie_recommendations (
    id SERIAL PRIMARY KEY,
    age_min_months  INTEGER NOT NULL,
    age_max_months  INTEGER NOT NULL,
    gender          VARCHAR(10) DEFAULT 'all',  -- 'all', 'male', 'female'
    kcal_per_day    INTEGER NOT NULL,
    protein_g       DECIMAL(5, 1),
    fat_g           DECIMAL(5, 1),
    carbs_g         DECIMAL(5, 1),
    calcium_mg      DECIMAL(7, 1),
    iron_mg         DECIMAL(5, 1),
    label           VARCHAR(100),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 4. 일별 칼로리 요약 (캐시용 — 선택사항, 집계 쿼리로 대체 가능)
CREATE TABLE IF NOT EXISTS daily_calorie_summary (
    id SERIAL PRIMARY KEY,
    baby_id       INTEGER REFERENCES babies(id) ON DELETE CASCADE,
    user_id       INTEGER NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    summary_date  DATE NOT NULL,
    total_calories DECIMAL(8, 2) DEFAULT 0,
    total_protein  DECIMAL(8, 2) DEFAULT 0,
    total_fat      DECIMAL(8, 2) DEFAULT 0,
    total_carbs    DECIMAL(8, 2) DEFAULT 0,
    meal_count     INTEGER DEFAULT 0,
    updated_at    TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(baby_id, summary_date),
    UNIQUE(user_id, summary_date, baby_id)
);

-- Indexes
CREATE INDEX IF NOT EXISTS idx_foods_category ON foods(category);
CREATE INDEX IF NOT EXISTS idx_foods_is_baby_food ON foods(is_baby_food);
CREATE INDEX IF NOT EXISTS idx_foods_name ON foods(name);
CREATE INDEX IF NOT EXISTS idx_food_intake_logs_baby_date ON food_intake_logs(baby_id, log_date);
CREATE INDEX IF NOT EXISTS idx_food_intake_logs_user_date ON food_intake_logs(user_id, log_date);
CREATE INDEX IF NOT EXISTS idx_calorie_rec_age ON calorie_recommendations(age_min_months, age_max_months);

-- ─── Seed: 나이별 권장 칼로리 (한국 영양섭취기준 2020) ───────────────────────────
INSERT INTO calorie_recommendations (age_min_months, age_max_months, gender, kcal_per_day, protein_g, fat_g, carbs_g, calcium_mg, iron_mg, label) VALUES
(0,  5,  'all',    550,  10.0, 31.0,  60.0, 200.0, 0.3,  '0~5개월'),
(6,  11, 'all',    700,  15.0, 25.0,  90.0, 300.0, 6.0,  '6~11개월'),
(12, 23, 'all',    900,  20.0, 25.0, 120.0, 500.0, 6.0,  '12~23개월'),
(24, 35, 'male',  1100,  25.0, 25.0, 150.0, 500.0, 6.0,  '2~3세 남'),
(24, 35, 'female',1000,  25.0, 25.0, 130.0, 500.0, 6.0,  '2~3세 여'),
(36, 47, 'male',  1300,  30.0, 25.0, 180.0, 600.0, 7.0,  '3~4세 남'),
(36, 47, 'female',1200,  30.0, 25.0, 160.0, 600.0, 7.0,  '3~4세 여'),
(48, 71, 'male',  1500,  35.0, 25.0, 210.0, 700.0, 8.0,  '4~6세 남'),
(48, 71, 'female',1400,  35.0, 25.0, 190.0, 700.0, 8.0,  '4~6세 여')
ON CONFLICT DO NOTHING;

-- ─── Seed: 식품 데이터베이스 (Korean Baby Foods + Common Foods) ──────────────
INSERT INTO foods (name, name_ko, category, calories_per_100g, protein_per_100g, fat_per_100g, carbs_per_100g, fiber_per_100g, sodium_per_100g, calcium_per_100g, iron_per_100g, is_baby_food, min_age_months, allergens) VALUES
-- 이유식 / 아기 식품
('Rice porridge (plain)',    '쌀미음',         '곡류',   55,  1.2, 0.2, 12.0, 0.1,   2.0,  2.0, 0.1, true,  4, '{}'),
('Rice porridge (thick)',    '쌀죽',           '곡류',   72,  1.5, 0.3, 15.8, 0.2,   3.0,  3.0, 0.2, true,  6, '{}'),
('Sweet potato puree',       '고구마 퓨레',    '채소류', 86,  1.6, 0.1, 20.1, 3.0,  55.0, 30.0, 0.6, true,  5, '{}'),
('Carrot puree',             '당근 퓨레',      '채소류', 41,  0.9, 0.2,  9.6, 2.8,  69.0, 33.0, 0.3, true,  5, '{}'),
('Broccoli puree',           '브로콜리 퓨레',  '채소류', 34,  2.8, 0.4,  6.6, 2.6,  33.0, 47.0, 0.7, true,  6, '{}'),
('Apple puree',              '사과 퓨레',      '과일류', 52,  0.3, 0.2, 13.8, 2.4,   1.0,  6.0, 0.1, true,  4, '{}'),
('Banana mash',              '바나나 으깬 것', '과일류', 89,  1.1, 0.3, 22.8, 2.6,   1.0,  5.0, 0.3, true,  5, '{}'),
('Pear puree',               '배 퓨레',        '과일류', 58,  0.4, 0.1, 15.5, 3.1,   1.0,  9.0, 0.2, true,  4, '{}'),
('Chicken puree',            '닭고기 퓨레',    '육류',  165, 20.0, 9.0,   0.0, 0.0,  65.0, 11.0, 1.0, true,  6, '{}'),
('Beef puree',               '쇠고기 퓨레',   '육류',  250, 17.0,20.0,   0.0, 0.0,  72.0,  6.0, 2.6, true,  6, '{}'),
('Tofu',                     '두부',           '두류',   76,  8.1, 4.2,  1.9, 0.3,   7.0, 130.0, 1.6, true,  6, '{}'),
('Plain yogurt',             '플레인 요거트',  '유제품', 61,  3.5, 3.3,  4.7, 0.0,  46.0, 121.0, 0.1, true,  6, '{유제품}'),
('Breast milk (100ml)',      '모유 (100ml)',   '음료',   65,  1.1, 3.5,  7.2, 0.0,  17.0, 32.0, 0.0, true,  0, '{}'),
('Formula milk (100ml)',     '분유 (100ml)',   '음료',   68,  1.5, 3.6,  7.5, 0.0,  28.0, 53.0, 1.0, true,  0, '{유제품}'),
-- 일반 식품
('White rice (cooked)',      '흰밥',           '곡류',  130,  2.5, 0.3, 28.7, 0.3,   1.0,  3.0, 0.2, false, 12, '{}'),
('Egg',                      '달걀',           '육류',  155, 13.0,11.0,  1.1, 0.0, 124.0, 56.0, 1.8, false,  9, '{달걀}'),
('Salmon',                   '연어',           '어패류',208, 20.0,13.4,  0.0, 0.0,  59.0, 12.0, 0.3, false,  8, '{어패류}'),
('Spinach',                  '시금치',         '채소류', 23,  2.9, 0.4,  3.6, 2.2,  79.0, 99.0, 2.7, false,  8, '{}'),
('Avocado',                  '아보카도',       '과일류',160,  2.0,14.7,  8.5, 6.7,   7.0, 12.0, 0.6, false,  6, '{}'),
('Whole milk',               '우유',           '유제품', 61,  3.2, 3.3,  4.8, 0.0,  43.0, 113.0, 0.0, false, 12, '{유제품}'),
('Oatmeal (cooked)',         '오트밀',         '곡류',   71,  2.5, 1.4, 12.0, 1.7,  49.0, 10.0, 0.7, false,  6, '{글루텐}'),
('Lentils (cooked)',         '렌틸콩',         '두류',  116,  9.0, 0.4, 20.0, 7.9,   2.0, 19.0, 3.3, false,  8, '{}'),
('Pumpkin puree',            '단호박 퓨레',    '채소류', 26,  1.0, 0.1,  6.5, 0.5,   1.0, 21.0, 0.8, true,   5, '{}'),
('Cod fish',                 '대구살',         '어패류', 82, 17.8, 0.7,  0.0, 0.0,  54.0, 16.0, 0.4, true,   7, '{어패류}'),
('Cheese (cheddar)',         '체다치즈',       '유제품',402, 25.0,33.1,  1.3, 0.0, 621.0,721.0, 0.7, false, 10, '{유제품}')
ON CONFLICT DO NOTHING;
