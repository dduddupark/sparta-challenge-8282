-- =====================================================================
-- V2 — p_payment 유니크 인덱스 확정 (이슈 2026-07-10 문제 2-2)
--
-- 배경: `ddl-auto: update` 는 이미 존재하는 컬럼에 유니크 제약을 신뢰성 있게 추가하지 못한다.
-- 그래서 운영/개발 DB 에 idempotency_key·order_id 유니크 인덱스가 실제로는 없을 수 있고,
-- 이 경우 insert-first 멱등 방어선(PaymentService)이 무력화되어 이중결제가 가능하다.
--
-- 이 마이그레이션은 baseline(V1) 위에서 실행되므로 기존 DB 에도 적용된다.
-- `IF NOT EXISTS` 로 멱등하게 동작한다(이미 유니크 인덱스/제약이 있으면 인덱스만 보강, 없으면 생성).
-- =====================================================================

create unique index if not exists uq_payment_order_id on p_payment (order_id);
create unique index if not exists uq_payment_idempotency_key on p_payment (idempotency_key);
create unique index if not exists uq_payment_transaction_id on p_payment (transaction_id);
