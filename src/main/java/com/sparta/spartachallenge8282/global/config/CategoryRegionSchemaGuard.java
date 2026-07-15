package com.sparta.spartachallenge8282.global.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * p_category · p_region 조건부(Soft Delete) 유니크 인덱스 보장.
 *
 * <p><b>배경</b>: 이름 유일성은 <b>삭제되지 않은 행끼리만</b> 지켜야 하고(소프트 삭제된 이름은 재사용 가능),
 * 이 규칙은 {@code WHERE deleted_at IS NULL} 조건이 붙은 <b>partial unique index</b> 로만 표현된다.
 * partial index 는 JPA/{@code @Column(unique = true)} 로 생성할 수 없어 별도 DDL 이 필요하다.
 * 이를 운영 DB 에 사람이 수동 적용하면(배포 담당자에게 부탁) 누락·순서 실수로 방어선이 조용히
 * 무력화되어 이름 중복이 뚫린다({@code CategoryService}/{@code RegionService} 의 사전 존재 검사만으로는
 * 동시 요청의 경합을 막지 못한다).
 *
 * <p><b>동작</b>: 애플리케이션 기동 완료 후({@link ApplicationRunner} — Hibernate 가 테이블을 만든 뒤)
 * {@code CREATE UNIQUE INDEX IF NOT EXISTS ... WHERE deleted_at IS NULL} 를 실행해
 * 신규·기존 DB(로컬·CI·운영) 모두에서 인덱스를 멱등하게 보장한다. {@code IF NOT EXISTS} 라서
 * 매 배포마다 실행돼도 무해하다. 이로써 "운영 DB 에 SQL 을 1회 수동 적용" 의존이 사라진다.
 *
 * <p><b>인덱스 이름 고정</b>: {@code CategoryService}/{@code RegionService} 가
 * {@code uk_category_name_active}/{@code uk_region_name_active} 이름으로 충돌을 판별하므로 변경하지 않는다.
 *
 * <p><b>주의</b>: 삭제되지 않은 행에 이미 이름 중복이 있으면 유니크 인덱스 생성이 실패하며 기동이
 * 중단된다. 유일성을 보장할 수 없는 상태로 API 를 여는 것보다 fail-fast 가 안전하다
 * (중복 행을 먼저 정리할 것).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CategoryRegionSchemaGuard implements ApplicationRunner {

    private final JdbcTemplate jdbcTemplate;

    private static final String[] PARTIAL_UNIQUE_INDEXES = {
            "create unique index if not exists uk_category_name_active on p_category (name) where deleted_at is null",
            "create unique index if not exists uk_region_name_active on p_region (name) where deleted_at is null"
    };

    @Override
    public void run(ApplicationArguments args) {
        for (String ddl : PARTIAL_UNIQUE_INDEXES) {
            jdbcTemplate.execute(ddl);
        }
        log.info("[CategoryRegionSchemaGuard] partial 유니크 인덱스 보장 완료 (uk_category_name_active, uk_region_name_active)");
    }
}
