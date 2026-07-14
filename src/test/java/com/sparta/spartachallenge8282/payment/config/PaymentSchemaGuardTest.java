package com.sparta.spartachallenge8282.payment.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link PaymentSchemaGuard} 가 기동 시 p_payment 유니크 인덱스를 실제로 생성하는지 검증한다.
 * (insert-first 멱등 방어선이 의존하는 인덱스 보장 — 이슈 2-2)
 */
@SpringBootTest
class PaymentSchemaGuardTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    @DisplayName("기동 후 p_payment 의 유니크 인덱스 3종이 존재한다")
    void uniqueIndexesExist() {
        Integer count = jdbcTemplate.queryForObject(
                "select count(*) from pg_indexes where tablename = 'p_payment' " +
                        "and indexname in ('uq_payment_order_id','uq_payment_idempotency_key','uq_payment_transaction_id')",
                Integer.class);

        assertThat(count).isEqualTo(3);
    }
}
