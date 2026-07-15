-- Category / Region Soft Delete 조건부 유일성 인덱스 적용 SQL
--
-- 목적
--   1. deleted_at IS NULL인 데이터끼리는 이름 중복을 허용하지 않는다.
--   2. 소프트 삭제된 이름은 새로운 데이터에서 다시 사용할 수 있다.
--
-- 적용 시점
--   p_category, p_region 테이블이 생성된 뒤 운영 DB에 최초 1회 적용한다.
--   일반적인 애플리케이션 재배포 때마다 반복해서 실행할 필요는 없다.


-- 1. 대상 테이블 존재 확인
-- 정상이라면 category_table, region_table에 각각 테이블명이 표시된다.
SELECT
    to_regclass('public.p_category') AS category_table,
    to_regclass('public.p_region') AS region_table;


-- 2. 삭제되지 않은 데이터의 이름 중복 확인
-- 결과가 0건이어야 한다.
-- 결과가 나오면 아래 인덱스를 생성하기 전에 중복 데이터를 먼저 정리한다.
SELECT
    'CATEGORY' AS domain,
    name,
    COUNT(*) AS duplicate_count
FROM p_category
WHERE deleted_at IS NULL
GROUP BY name
HAVING COUNT(*) > 1

UNION ALL

SELECT
    'REGION' AS domain,
    name,
    COUNT(*) AS duplicate_count
FROM p_region
WHERE deleted_at IS NULL
GROUP BY name
HAVING COUNT(*) > 1;


-- 3. Partial Unique Index 생성
-- 두 인덱스 중 하나라도 생성에 실패하면 COMMIT되지 않도록 한 트랜잭션에서 실행한다.
-- Service가 아래 인덱스 이름으로 충돌을 판별하므로 이름을 변경하지 않는다.
BEGIN;

CREATE UNIQUE INDEX IF NOT EXISTS uk_category_name_active
ON p_category (name)
WHERE deleted_at IS NULL;

CREATE UNIQUE INDEX IF NOT EXISTS uk_region_name_active
ON p_region (name)
WHERE deleted_at IS NULL;

COMMIT;


-- 4. 적용 결과 확인
-- 정상이라면 uk_category_name_active, uk_region_name_active 두 행이 조회된다.
SELECT
    schemaname,
    tablename,
    indexname,
    indexdef
FROM pg_indexes
WHERE schemaname = 'public'
  AND indexname IN (
      'uk_category_name_active',
      'uk_region_name_active'
  )
ORDER BY indexname;

