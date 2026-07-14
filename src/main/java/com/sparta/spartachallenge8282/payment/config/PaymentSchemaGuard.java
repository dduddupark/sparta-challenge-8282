package com.sparta.spartachallenge8282.payment.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * p_payment 유니크 인덱스 보장 (이슈 2026-07-10 문제 2-2).
 *
 * <p><b>배경</b>: {@code ddl-auto: update} 는 이미 존재하는 컬럼에 유니크 제약을 신뢰성 있게
 * 추가하지 못한다. 그래서 운영/개발 DB 에 {@code idempotency_key}·{@code order_id} 유니크 인덱스가
 * 실제로는 없을 수 있고, 이 경우 insert-first 멱등 방어선({@code PaymentService})이 무력화되어
 * 이중결제가 가능하다.
 *
 * <p><b>동작</b>: 애플리케이션 기동 완료 후({@link ApplicationRunner} — Hibernate 가 테이블을 만든 뒤)
 * {@code CREATE UNIQUE INDEX IF NOT EXISTS} 를 실행해 신규·기존 DB 모두에서 인덱스를 멱등하게 보장한다.
 * 엔티티의 {@code unique=true} 는 신규 DB 용, 이 가드는 기존 DB 의 누락분을 메우는 안전망이다.
 *
 * <p><b>주의</b>: 중복 데이터가 이미 있으면 유니크 인덱스 생성이 실패하며 기동이 중단된다.
 * 유일성을 보장할 수 없는 상태로 결제 API 를 여는 것보다 fail-fast 가 안전하다(중복 행을 먼저 정리할 것).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentSchemaGuard implements ApplicationRunner {

    private final JdbcTemplate jdbcTemplate;

    private static final String[] UNIQUE_INDEXES = {
            "create unique index if not exists uq_payment_order_id on p_payment (order_id)",
            "create unique index if not exists uq_payment_idempotency_key on p_payment (idempotency_key)",
            "create unique index if not exists uq_payment_transaction_id on p_payment (transaction_id)"
    };

    @Override
    public void run(ApplicationArguments args) {
        for (String ddl : UNIQUE_INDEXES) {
            jdbcTemplate.execute(ddl);
        }
        log.info("[PaymentSchemaGuard] p_payment 유니크 인덱스 보장 완료 (order_id, idempotency_key, transaction_id)");
    }
}
