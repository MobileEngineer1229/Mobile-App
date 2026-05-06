-- Migration 004: Meal Recommendation System
-- 아기 월령별 매끼 식사 추천

-- 1. 식사 추천 템플릿 (Meal Plan Templates)
CREATE TABLE IF NOT EXISTS meal_plan_templates (
    id SERIAL PRIMARY KEY,
    age_min_months  INTEGER NOT NULL,
    age_max_months  INTEGER NOT NULL,
    meal_type       VARCHAR(20) NOT NULL,   -- 'breakfast','lunch','dinner','snack','breastmilk','formula'
    title           VARCHAR(255) NOT NULL,  -- e.g. '아침 - 쌀미음 + 사과 퓨레'
    description     TEXT,                  -- 준비 팁, 조리 방법 메모
    total_kcal_approx INTEGER,             -- 해당 식사 예상 칼로리
    stage_label     VARCHAR(50),           -- '이유식 초기', '이유식 중기', '이유식 후기', '유아식'
    sort_order      INTEGER DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 2. 템플릿별 식품 목록 (권장 섭취량 포함)
CREATE TABLE IF NOT EXISTS meal_plan_foods (
    id SERIAL PRIMARY KEY,
    template_id     INTEGER NOT NULL REFERENCES meal_plan_templates(id) ON DELETE CASCADE,
    food_id         INTEGER NOT NULL REFERENCES foods(id),
    recommended_g   DECIMAL(6,1) NOT NULL,  -- 권장 섭취량 (g 또는 ml)
    unit            VARCHAR(10) DEFAULT 'g', -- 'g', 'ml'
    notes           TEXT                     -- '소금 넣지 말 것', '으깨서 제공' 등
);

-- Indexes
CREATE INDEX IF NOT EXISTS idx_meal_plan_templates_age ON meal_plan_templates(age_min_months, age_max_months);
CREATE INDEX IF NOT EXISTS idx_meal_plan_foods_template ON meal_plan_foods(template_id);

-- ═══════════════════════════════════════════════════════════════════════════
-- SEED: 월령별 식사 추천
-- Food IDs:
--  1=쌀미음, 2=쌀죽, 3=고구마퓨레, 4=당근퓨레, 5=브로콜리퓨레
--  6=사과퓨레, 7=바나나, 8=배퓨레, 9=닭고기퓨레, 10=쇠고기퓨레
-- 11=두부, 12=요거트, 13=모유, 14=분유, 15=흰밥
-- 16=달걀, 17=연어, 18=시금치, 19=아보카도, 20=우유
-- 21=오트밀, 22=렌틸콩, 23=단호박퓨레, 24=대구살, 25=체다치즈
-- ═══════════════════════════════════════════════════════════════════════════

-- ─── 0~5개월: 완전 모유/분유 수유 ───────────────────────────────────────────
INSERT INTO meal_plan_templates (age_min_months, age_max_months, meal_type, title, description, total_kcal_approx, stage_label, sort_order) VALUES
(0, 5, 'breastmilk', '아침 수유 — 모유 또는 분유', '2~3시간 간격으로 수유하세요. 이 시기에는 모유 또는 분유만으로 모든 영양이 충족됩니다.', 65, '완전 수유기', 1),
(0, 5, 'breastmilk', '점심 수유 — 모유 또는 분유', NULL, 65, '완전 수유기', 2),
(0, 5, 'breastmilk', '저녁 수유 — 모유 또는 분유', NULL, 65, '완전 수유기', 3),
(0, 5, 'breastmilk', '밤 수유 — 모유 또는 분유', '밤 수유는 아기가 원할 때 제공하세요.', 65, '완전 수유기', 4);

INSERT INTO meal_plan_foods (template_id, food_id, recommended_g, unit, notes) VALUES
((SELECT id FROM meal_plan_templates WHERE age_min_months=0 AND meal_type='breastmilk' AND sort_order=1), 13, 120, 'ml', '수요에 따라 조절'),
((SELECT id FROM meal_plan_templates WHERE age_min_months=0 AND meal_type='breastmilk' AND sort_order=2), 13, 120, 'ml', NULL),
((SELECT id FROM meal_plan_templates WHERE age_min_months=0 AND meal_type='breastmilk' AND sort_order=3), 13, 120, 'ml', NULL),
((SELECT id FROM meal_plan_templates WHERE age_min_months=0 AND meal_type='breastmilk' AND sort_order=4), 13, 100, 'ml', NULL);

-- ─── 6~7개월: 이유식 초기 ────────────────────────────────────────────────────
INSERT INTO meal_plan_templates (age_min_months, age_max_months, meal_type, title, description, total_kcal_approx, stage_label, sort_order) VALUES
(6, 7, 'breastmilk', '아침 — 모유/분유', '이유식 시작 전 수유로 하루를 시작하세요.', 68, '이유식 초기', 1),
(6, 7, 'lunch',      '점심 — 쌀미음 + 채소 퓨레', '10배죽(쌀미음)으로 시작하여 단일 재료 퓨레를 1가지씩 추가하세요. 새 식재료는 3일 간격으로 추가하여 알레르기를 확인하세요.', 55, '이유식 초기', 2),
(6, 7, 'breastmilk', '저녁 — 모유/분유', '이유식 후 부족한 영양은 수유로 보충하세요.', 68, '이유식 초기', 3),
(6, 7, 'snack',      '간식 — 과일 퓨레', '사과 또는 배 퓨레를 소량 제공하세요. 과일의 천연 단맛으로 이유식에 친숙해지게 합니다.', 26, '이유식 초기', 4);

INSERT INTO meal_plan_foods (template_id, food_id, recommended_g, unit, notes) VALUES
-- 아침 수유
((SELECT id FROM meal_plan_templates WHERE age_min_months=6 AND meal_type='breastmilk' AND sort_order=1), 14, 180, 'ml', '분유 또는 모유'),
-- 점심 이유식
((SELECT id FROM meal_plan_templates WHERE age_min_months=6 AND meal_type='lunch'), 1,  30, 'g', '쌀미음 (10배죽)'),
((SELECT id FROM meal_plan_templates WHERE age_min_months=6 AND meal_type='lunch'), 4,  20, 'g', '당근 퓨레 또는 고구마 퓨레'),
-- 저녁 수유
((SELECT id FROM meal_plan_templates WHERE age_min_months=6 AND meal_type='breastmilk' AND sort_order=3), 14, 180, 'ml', NULL),
-- 간식
((SELECT id FROM meal_plan_templates WHERE age_min_months=6 AND meal_type='snack'), 6, 30, 'g', '사과 퓨레 (당일 준비)');

-- ─── 8~9개월: 이유식 중기 ────────────────────────────────────────────────────
INSERT INTO meal_plan_templates (age_min_months, age_max_months, meal_type, title, description, total_kcal_approx, stage_label, sort_order) VALUES
(8, 9, 'breakfast', '아침 — 오트밀 + 과일 퓨레',      '오트밀은 철분과 섬유질이 풍부합니다. 바나나나 사과 퓨레로 단맛을 더해 주세요.', 80,  '이유식 중기', 1),
(8, 9, 'lunch',     '점심 — 쌀죽 + 단백질 + 채소',    '닭고기나 두부를 추가하여 단백질을 공급하세요. 채소는 2가지까지 혼합 가능합니다.', 115, '이유식 중기', 2),
(8, 9, 'dinner',    '저녁 — 쌀죽 + 채소 퓨레',        '저녁은 소화하기 쉬운 채소 위주로 구성하세요.', 80,  '이유식 중기', 3),
(8, 9, 'snack',     '간식 — 요거트 + 과일',            '플레인 요거트는 칼슘과 유산균이 풍부합니다. 알레르기 확인 후 제공하세요.', 70,  '이유식 중기', 4);

INSERT INTO meal_plan_foods (template_id, food_id, recommended_g, unit, notes) VALUES
-- 아침
((SELECT id FROM meal_plan_templates WHERE age_min_months=8 AND meal_type='breakfast'), 21, 40, 'g', '오트밀 (글루텐 알레르기 확인)'),
((SELECT id FROM meal_plan_templates WHERE age_min_months=8 AND meal_type='breakfast'), 7,  30, 'g', '바나나 으깬 것'),
-- 점심
((SELECT id FROM meal_plan_templates WHERE age_min_months=8 AND meal_type='lunch'), 2,  60, 'g', '쌀죽 (7배죽)'),
((SELECT id FROM meal_plan_templates WHERE age_min_months=8 AND meal_type='lunch'), 9,  30, 'g', '닭고기 퓨레 (껍질 제거)'),
((SELECT id FROM meal_plan_templates WHERE age_min_months=8 AND meal_type='lunch'), 5,  25, 'g', '브로콜리 퓨레'),
-- 저녁
((SELECT id FROM meal_plan_templates WHERE age_min_months=8 AND meal_type='dinner'), 2,  60, 'g', '쌀죽'),
((SELECT id FROM meal_plan_templates WHERE age_min_months=8 AND meal_type='dinner'), 3,  30, 'g', '고구마 퓨레'),
((SELECT id FROM meal_plan_templates WHERE age_min_months=8 AND meal_type='dinner'), 18, 20, 'g', '시금치 퓨레 (철분 공급)'),
-- 간식
((SELECT id FROM meal_plan_templates WHERE age_min_months=8 AND meal_type='snack'), 12, 50, 'g', '플레인 요거트 (무설탕)'),
((SELECT id FROM meal_plan_templates WHERE age_min_months=8 AND meal_type='snack'), 6,  30, 'g', '사과 퓨레');

-- ─── 10~12개월: 이유식 후기 ──────────────────────────────────────────────────
INSERT INTO meal_plan_templates (age_min_months, age_max_months, meal_type, title, description, total_kcal_approx, stage_label, sort_order) VALUES
(10, 12, 'breakfast', '아침 — 연두부 + 으깬 바나나 + 분유', '달걀은 노른자부터 시작하여 흰자 알레르기를 확인하세요.', 120, '이유식 후기', 1),
(10, 12, 'lunch',     '점심 — 진밥 + 쇠고기 + 채소 2종',   '진밥(5배죽)으로 씹는 연습을 시작합니다. 쇠고기는 철분 공급에 필수입니다.', 160, '이유식 후기', 2),
(10, 12, 'dinner',    '저녁 — 진밥 + 생선 + 채소',         '흰살 생선(대구)은 알레르기가 적어 이 시기에 적합합니다.', 130, '이유식 후기', 3),
(10, 12, 'snack',     '오전 간식 — 과일 + 치즈',            '체다치즈는 소량만 제공하세요 (나트륨 주의).', 80,  '이유식 후기', 4),
(10, 12, 'snack',     '오후 간식 — 요거트 + 아보카도',       '아보카도는 좋은 지방과 칼로리를 제공합니다.', 90,  '이유식 후기', 5);

INSERT INTO meal_plan_foods (template_id, food_id, recommended_g, unit, notes) VALUES
-- 아침
((SELECT id FROM meal_plan_templates WHERE age_min_months=10 AND meal_type='breakfast'), 11, 50, 'g', '연두부 (잘 으깨서)'),
((SELECT id FROM meal_plan_templates WHERE age_min_months=10 AND meal_type='breakfast'), 7,  40, 'g', '바나나 으깬 것'),
((SELECT id FROM meal_plan_templates WHERE age_min_months=10 AND meal_type='breakfast'), 14, 100,'ml','분유 (부족한 칼로리 보충)'),
-- 점심
((SELECT id FROM meal_plan_templates WHERE age_min_months=10 AND meal_type='lunch'), 2,  80, 'g', '진밥 (5배죽)'),
((SELECT id FROM meal_plan_templates WHERE age_min_months=10 AND meal_type='lunch'), 10, 40, 'g', '쇠고기 퓨레 (다진 것)'),
((SELECT id FROM meal_plan_templates WHERE age_min_months=10 AND meal_type='lunch'), 5,  30, 'g', '브로콜리 (작게 잘라서)'),
((SELECT id FROM meal_plan_templates WHERE age_min_months=10 AND meal_type='lunch'), 4,  25, 'g', '당근 (부드럽게 삶아서)'),
-- 저녁
((SELECT id FROM meal_plan_templates WHERE age_min_months=10 AND meal_type='dinner'), 2,  80, 'g', '진밥'),
((SELECT id FROM meal_plan_templates WHERE age_min_months=10 AND meal_type='dinner'), 24, 40, 'g', '대구살 (가시 완전 제거)'),
((SELECT id FROM meal_plan_templates WHERE age_min_months=10 AND meal_type='dinner'), 18, 25, 'g', '시금치'),
-- 오전 간식
((SELECT id FROM meal_plan_templates WHERE age_min_months=10 AND meal_type='snack' AND sort_order=4), 8,  50, 'g', '배 퓨레'),
((SELECT id FROM meal_plan_templates WHERE age_min_months=10 AND meal_type='snack' AND sort_order=4), 25, 10, 'g', '체다치즈 (소량)'),
-- 오후 간식
((SELECT id FROM meal_plan_templates WHERE age_min_months=10 AND meal_type='snack' AND sort_order=5), 12, 60, 'g', '플레인 요거트'),
((SELECT id FROM meal_plan_templates WHERE age_min_months=10 AND meal_type='snack' AND sort_order=5), 19, 30, 'g', '아보카도 (으깨서)');

-- ─── 13~24개월: 유아식 초기 ──────────────────────────────────────────────────
INSERT INTO meal_plan_templates (age_min_months, age_max_months, meal_type, title, description, total_kcal_approx, stage_label, sort_order) VALUES
(13, 24, 'breakfast', '아침 — 달걀 + 밥 + 우유',       '달걀은 완전식품입니다. 스크램블이나 반숙으로 제공하세요. 우유는 하루 400ml를 넘지 않게 합니다.', 200, '유아식 초기', 1),
(13, 24, 'lunch',     '점심 — 밥 + 쇠고기 + 채소 2종', '어른 식사와 비슷하게 구성하되 간(소금/설탕)은 최소화하세요.', 250, '유아식 초기', 2),
(13, 24, 'dinner',    '저녁 — 밥 + 생선 또는 닭고기 + 채소', '생선은 주 2~3회, 고기는 매일 제공을 목표로 합니다.', 220, '유아식 초기', 3),
(13, 24, 'snack',     '오전 간식 — 과일 + 요거트',      '신선한 과일로 비타민을 보충합니다.', 100, '유아식 초기', 4),
(13, 24, 'snack',     '오후 간식 — 우유 + 아보카도/치즈', '오후 간식으로 칼로리와 칼슘을 보충합니다.', 130, '유아식 초기', 5);

INSERT INTO meal_plan_foods (template_id, food_id, recommended_g, unit, notes) VALUES
-- 아침
((SELECT id FROM meal_plan_templates WHERE age_min_months=13 AND meal_type='breakfast'), 16, 50, 'g', '달걀 1개 (완숙 또는 스크램블)'),
((SELECT id FROM meal_plan_templates WHERE age_min_months=13 AND meal_type='breakfast'), 15, 80, 'g', '흰밥'),
((SELECT id FROM meal_plan_templates WHERE age_min_months=13 AND meal_type='breakfast'), 20,150,'ml', '우유 (하루 400ml 이내)'),
-- 점심
((SELECT id FROM meal_plan_templates WHERE age_min_months=13 AND meal_type='lunch'), 15, 100, 'g', '흰밥'),
((SELECT id FROM meal_plan_templates WHERE age_min_months=13 AND meal_type='lunch'), 10,  50, 'g', '쇠고기 (다진 것, 간 없이 조리)'),
((SELECT id FROM meal_plan_templates WHERE age_min_months=13 AND meal_type='lunch'), 5,   40, 'g', '브로콜리'),
((SELECT id FROM meal_plan_templates WHERE age_min_months=13 AND meal_type='lunch'), 18,  30, 'g', '시금치 나물'),
-- 저녁
((SELECT id FROM meal_plan_templates WHERE age_min_months=13 AND meal_type='dinner'), 15, 100, 'g', '흰밥'),
((SELECT id FROM meal_plan_templates WHERE age_min_months=13 AND meal_type='dinner'), 17,  50, 'g', '연어 (뼈 제거, 잘 익혀서)'),
((SELECT id FROM meal_plan_templates WHERE age_min_months=13 AND meal_type='dinner'), 3,   40, 'g', '고구마'),
((SELECT id FROM meal_plan_templates WHERE age_min_months=13 AND meal_type='dinner'), 11,  40, 'g', '두부 (부드럽게 조리)'),
-- 오전 간식
((SELECT id FROM meal_plan_templates WHERE age_min_months=13 AND meal_type='snack' AND sort_order=4), 7,  60, 'g', '바나나'),
((SELECT id FROM meal_plan_templates WHERE age_min_months=13 AND meal_type='snack' AND sort_order=4), 12, 80, 'g', '플레인 요거트'),
-- 오후 간식
((SELECT id FROM meal_plan_templates WHERE age_min_months=13 AND meal_type='snack' AND sort_order=5), 20,150,'ml','우유'),
((SELECT id FROM meal_plan_templates WHERE age_min_months=13 AND meal_type='snack' AND sort_order=5), 25, 15, 'g', '체다치즈 1장');

-- ─── 25~47개월: 유아식 중기/후기 ─────────────────────────────────────────────
INSERT INTO meal_plan_templates (age_min_months, age_max_months, meal_type, title, description, total_kcal_approx, stage_label, sort_order) VALUES
(25, 47, 'breakfast', '아침 — 달걀 + 밥 + 채소 + 우유',  '어른과 거의 같은 식사 구성입니다. 채소 반찬을 1가지 이상 포함하세요.', 280, '유아식', 1),
(25, 47, 'lunch',     '점심 — 밥 + 고기 반찬 + 채소 2종 + 국', '국은 싱겁게 끓여 제공하세요. 나트륨 하루 900mg 이내를 목표로 합니다.', 350, '유아식', 2),
(25, 47, 'dinner',    '저녁 — 밥 + 생선 + 채소 + 렌틸콩', '식물성 단백질(렌틸콩, 두부)도 균형 있게 포함하세요.', 300, '유아식', 3),
(25, 47, 'snack',     '오전 간식 — 과일 2종',             '신선한 제철 과일을 제공하세요.', 90,  '유아식', 4),
(25, 47, 'snack',     '오후 간식 — 우유 + 치즈 또는 요거트','칼슘 섭취를 위해 유제품을 간식으로 활용합니다.', 150, '유아식', 5);

INSERT INTO meal_plan_foods (template_id, food_id, recommended_g, unit, notes) VALUES
-- 아침
((SELECT id FROM meal_plan_templates WHERE age_min_months=25 AND meal_type='breakfast'), 16,  55, 'g', '달걀 1개'),
((SELECT id FROM meal_plan_templates WHERE age_min_months=25 AND meal_type='breakfast'), 15, 120, 'g', '흰밥'),
((SELECT id FROM meal_plan_templates WHERE age_min_months=25 AND meal_type='breakfast'), 18,  40, 'g', '시금치 나물'),
((SELECT id FROM meal_plan_templates WHERE age_min_months=25 AND meal_type='breakfast'), 20, 150,'ml','우유'),
-- 점심
((SELECT id FROM meal_plan_templates WHERE age_min_months=25 AND meal_type='lunch'), 15, 130, 'g', '흰밥'),
((SELECT id FROM meal_plan_templates WHERE age_min_months=25 AND meal_type='lunch'), 10,  60, 'g', '쇠고기 (살코기)'),
((SELECT id FROM meal_plan_templates WHERE age_min_months=25 AND meal_type='lunch'), 5,   50, 'g', '브로콜리'),
((SELECT id FROM meal_plan_templates WHERE age_min_months=25 AND meal_type='lunch'), 4,   40, 'g', '당근'),
-- 저녁
((SELECT id FROM meal_plan_templates WHERE age_min_months=25 AND meal_type='dinner'), 15, 130, 'g', '흰밥'),
((SELECT id FROM meal_plan_templates WHERE age_min_months=25 AND meal_type='dinner'), 17,  60, 'g', '연어'),
((SELECT id FROM meal_plan_templates WHERE age_min_months=25 AND meal_type='dinner'), 22,  50, 'g', '렌틸콩 (삶은 것)'),
((SELECT id FROM meal_plan_templates WHERE age_min_months=25 AND meal_type='dinner'), 3,   50, 'g', '고구마'),
-- 오전 간식
((SELECT id FROM meal_plan_templates WHERE age_min_months=25 AND meal_type='snack' AND sort_order=4), 6,  60, 'g', '사과'),
((SELECT id FROM meal_plan_templates WHERE age_min_months=25 AND meal_type='snack' AND sort_order=4), 7,  50, 'g', '바나나'),
-- 오후 간식
((SELECT id FROM meal_plan_templates WHERE age_min_months=25 AND meal_type='snack' AND sort_order=5), 20,150,'ml','우유'),
((SELECT id FROM meal_plan_templates WHERE age_min_months=25 AND meal_type='snack' AND sort_order=5), 25, 15, 'g', '치즈');
