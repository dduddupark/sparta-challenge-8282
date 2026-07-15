package com.sparta.spartachallenge8282.global.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link CategoryRegionSchemaGuard} 검증.
 *
 * <p>애플리케이션 컨텍스트가 뜨면 가드({@link org.springframework.boot.ApplicationRunner})가
 * 이미 실행된 상태이므로, {@code pg_indexes} 를 조회해 partial 유니크 인덱스 2종이
 * 실제로 존재하고(이름 일치), UNIQUE 이며, {@code WHERE deleted_at IS NULL} 부분 조건까지
 * 걸려 있는지 확인한다. (Service 가 이 인덱스 이름으로 충돌을 판별하므로 이름/조건이 핵심)
 *
 * <p>실제 PostgreSQL 에 대해 동작한다({@code pg_indexes} 는 Postgres 시스템 카탈로그).
 */
@SpringBootTest
class CategoryRegionSchemaGuardTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    /** 주어진 인덱스명의 정의(indexdef)를 조회한다. 없으면 빈 리스트. */
    private List<String> indexDefs(String indexName) {
        return jdbcTemplate.queryForList(
                "select indexdef from pg_indexes where schemaname = 'public' and indexname = ?",
                String.class, indexName);
    }

    @Test
    @DisplayName("uk_category_name_active - p_category(name) 부분 유니크 인덱스가 존재한다")
    void categoryPartialUniqueIndexExists() {
        List<String> defs = indexDefs("uk_category_name_active");

        assertThat(defs).hasSize(1);
        assertThat(defs.get(0))
                .contains("UNIQUE")
                .contains("p_category")
                .contains("(name)")
                .contains("deleted_at IS NULL"); // partial 조건
    }

    @Test
    @DisplayName("uk_region_name_active - p_region(name) 부분 유니크 인덱스가 존재한다")
    void regionPartialUniqueIndexExists() {
        List<String> defs = indexDefs("uk_region_name_active");

        assertThat(defs).hasSize(1);
        assertThat(defs.get(0))
                .contains("UNIQUE")
                .contains("p_region")
                .contains("(name)")
                .contains("deleted_at IS NULL"); // partial 조건
    }
}
