package com.sparta.spartachallenge8282;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * (검증) fresh DB 경로 확인 — 빈 스키마에 Flyway V1(baseline)+V2 를 순서대로 적용한 뒤
 * Hibernate {@code ddl-auto: validate} 가 통과하는지 확인한다.
 *
 * <p>실 운영/개발과 격리하기 위해 별도 스키마 {@code fresh_verify} 를 사용하고, 컨텍스트가
 * 정상 로드되면(=Flyway 마이그레이션 + validate 성공) fresh 경로가 검증된 것이다.
 * 이 테스트는 스키마 형상 회귀 방지를 위해 상시 유지한다(엔티티/마이그레이션 drift 를 CI 에서 잡음).
 */
@SpringBootTest
class FreshSchemaMigrationVerifyTest {

    private static final String SCHEMA = "fresh_verify";

    @DynamicPropertySource
    static void freshSchemaProps(DynamicPropertyRegistry registry) {
        // 빈 스키마를 Flyway 가 생성(create-schemas)하고 그 안에서 V1→V2 적용, Hibernate 는 해당 스키마를 validate.
        registry.add("spring.datasource.url",
                () -> "jdbc:postgresql://localhost:5432/delivery?currentSchema=" + SCHEMA);
        registry.add("spring.flyway.schemas", () -> SCHEMA);
        registry.add("spring.flyway.default-schema", () -> SCHEMA);
        registry.add("spring.flyway.create-schemas", () -> "true");
        // 빈 스키마면 V1 부터 적용. 직전 실행이 실패로 스키마를 남겼어도 baseline 처리해 재실행이 깨지지 않게 함.
        registry.add("spring.flyway.baseline-on-migrate", () -> "true");
        registry.add("spring.jpa.properties.hibernate.default_schema", () -> SCHEMA);
    }

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void freshSchemaMigratesAndValidates() {
        // 컨텍스트가 여기까지 로드됐다는 것은 Flyway 적용 + Hibernate validate 성공을 의미한다.
        Integer paymentCols = jdbcTemplate.queryForObject(
                "select count(*) from information_schema.columns " +
                        "where table_schema = ? and table_name = 'p_payment'", Integer.class, SCHEMA);
        assertThat(paymentCols).isGreaterThan(0);

        // V2 가 만든 유니크 인덱스가 실제로 존재하는지 확인 (2-2 방어선)
        Integer uniqueIdx = jdbcTemplate.queryForObject(
                "select count(*) from pg_indexes " +
                        "where schemaname = ? and tablename = 'p_payment' " +
                        "and indexname in ('uq_payment_order_id','uq_payment_idempotency_key','uq_payment_transaction_id')",
                Integer.class, SCHEMA);
        assertThat(uniqueIdx).isEqualTo(3);

        // 정리: 검증용 스키마 제거
        jdbcTemplate.execute("drop schema if exists " + SCHEMA + " cascade");
    }
}
