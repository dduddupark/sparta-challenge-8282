-- Review / ReviewReply Soft Delete 조건부 유일성 인덱스 적용 SQL
--
-- 목적
--   1. deleted_at IS NULL인 데이터끼리는 order_id / review_id 중복을 허용하지 않는다.
--   2. 소프트 삭제된 리뷰·답글의 order_id / review_id는 재작성 시 다시 사용할 수 있다.
--
-- 배경
--   기존에는 p_review.order_id, p_review_reply.review_id에 일반 UNIQUE 제약이 걸려 있었다.
--   Soft Delete는 행을 물리적으로 지우지 않으므로, 삭제된 행이 여전히 UNIQUE 제약을
--   점유해 같은 주문/리뷰로 재작성 시 DataIntegrityViolationException이 발생했다.
--
-- 적용 시점
--   p_review, p_review_reply 테이블이 생성된 뒤 운영 DB에 최초 1회 적용한다.
--   일반적인 애플리케이션 재배포 때마다 반복해서 실행할 필요는 없다.
--
-- 작성자: 박영재
-- 작성일: 2026-07-14 (로컬/개발 DB 적용 완료, 배포 DB는 적용 안 함)


-- 1. 대상 테이블 존재 확인
-- 정상이라면 review_table, review_reply_table에 각각 테이블명이 표시된다.
SELECT
    to_regclass('public.p_review') AS review_table,
    to_regclass('public.p_review_reply') AS review_reply_table;


-- 2. 기존 일반 UNIQUE 제약 이름 확인
-- 환경마다 이름이 자동 생성되어 다를 수 있으므로, 3번을 실행하기 전에 반드시 먼저 확인한다.
SELECT
    conrelid::regclass AS table_name,
    conname AS constraint_name
FROM pg_constraint
WHERE conrelid IN ('p_review'::regclass, 'p_review_reply'::regclass)
  AND contype = 'u';


-- 3. 삭제되지 않은 데이터의 order_id / review_id 중복 확인
-- 결과가 0건이어야 한다.
-- 결과가 나오면 아래 인덱스를 생성하기 전에 중복 데이터를 먼저 정리한다.
SELECT
    'REVIEW' AS domain,
    order_id,
    COUNT(*) AS duplicate_count
FROM p_review
WHERE deleted_at IS NULL
GROUP BY order_id
HAVING COUNT(*) > 1

UNION ALL

SELECT
    'REVIEW_REPLY' AS domain,
    review_id,
    COUNT(*) AS duplicate_count
FROM p_review_reply
WHERE deleted_at IS NULL
GROUP BY review_id
HAVING COUNT(*) > 1;


-- 4. 기존 일반 UNIQUE 제약 삭제 및 Partial Unique Index 생성
-- 2번에서 확인한 실제 제약 이름으로 아래 DROP CONSTRAINT를 교체한 뒤 실행한다.
-- 두 인덱스 중 하나라도 생성에 실패하면 COMMIT되지 않도록 한 트랜잭션에서 실행한다.
-- Service가 아래 인덱스 이름으로 충돌을 판별하므로 이름을 변경하지 않는다.
BEGIN;

ALTER TABLE p_review DROP CONSTRAINT IF EXISTS uk2ivyaatle6o0u2qqjc4rgw8hc;
ALTER TABLE p_review_reply DROP CONSTRAINT IF EXISTS ukfer3cnugqpnwvtryg5pbexycb;

CREATE UNIQUE INDEX IF NOT EXISTS uq_review_order_id_active
    ON p_review (order_id)
    WHERE deleted_at IS NULL;

CREATE UNIQUE INDEX IF NOT EXISTS uq_review_reply_review_id_active
    ON p_review_reply (review_id)
    WHERE deleted_at IS NULL;

COMMIT;


-- 5. 적용 결과 확인
-- 정상이라면 uq_review_order_id_active, uq_review_reply_review_id_active 두 행이 조회되고,
-- indexdef에 "WHERE (deleted_at IS NULL)"이 포함되어 있어야 한다.
SELECT
    schemaname,
    tablename,
    indexname,
    indexdef
FROM pg_indexes
WHERE schemaname = 'public'
  AND indexname IN (
                    'uq_review_order_id_active',
                    'uq_review_reply_review_id_active'
    )
ORDER BY indexname;


-- ============================================================
-- 참고: 다른 도메인에도 Soft Delete + Unique 컬럼 조합이 있다면
-- 동일한 문제가 잠재할 수 있다 (Category, Region 도메인도 동일 패턴 적용됨).
-- ============================================================