package com.sparta.spartachallenge8282.region.application;

import com.sparta.spartachallenge8282.global.exception.CustomException;
import com.sparta.spartachallenge8282.global.exception.ErrorCode;
import com.sparta.spartachallenge8282.region.presentation.dto.request.RegionCreateRequest;
import com.sparta.spartachallenge8282.region.presentation.dto.response.RegionCreateResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 실제 PostgreSQL에서 지역 이름 정책을 검증한다.
 *
 * <ul>
 *   <li>같은 이름을 동시에 생성하면 한 건만 성공한다.</li>
 *   <li>소프트 삭제된 이름은 새로운 지역에서 다시 사용할 수 있다.</li>
 * </ul>
 *
 * <p>{@link BeforeEach}와 {@link AfterEach}가 각 테스트 메서드 전후에 자동 실행되므로
 * 특정 테스트를 먼저 실행할 필요가 없으며, 각 테스트는 독립적으로 반복 실행할 수 있다.
 */
@SpringBootTest
class RegionNamePolicyTest {

    private static final String INDEX_NAME = "uk_region_name_active";

    @Autowired
    private RegionService regionService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private String testName;
    private boolean indexCreatedByTest;

    /** 각 생성 요청이 성공했는지, DB 이름 중복으로 거절됐는지 표현한다. */
    private enum CreateResult {
        SUCCESS,
        DUPLICATE
    }

    /**
     * 각 테스트 실행 전에 partial unique index를 준비한다.
     * 이미 존재하는 인덱스는 그대로 사용하고, 없을 때만 테스트가 임시로 생성한다.
     */
    @BeforeEach
    void prepareUniqueIndex() {
        Boolean indexExists = jdbcTemplate.queryForObject(
                "SELECT EXISTS (SELECT 1 FROM pg_indexes WHERE schemaname = current_schema() AND indexname = ?)",
                Boolean.class,
                INDEX_NAME
        );

        if (!Boolean.TRUE.equals(indexExists)) {
            jdbcTemplate.execute("""
                    CREATE UNIQUE INDEX uk_region_name_active
                    ON p_region (name)
                    WHERE deleted_at IS NULL
                    """);
            indexCreatedByTest = true;
        }
    }

    /**
     * 각 테스트가 만든 데이터를 삭제한다.
     * 인덱스는 이 테스트가 임시 생성한 경우에만 제거하므로 기존 DB 인덱스는 보존된다.
     */
    @AfterEach
    void cleanupTestDataAndIndex() {
        if (testName != null) {
            jdbcTemplate.update("DELETE FROM p_region WHERE name = ?", testName);
        }
        if (indexCreatedByTest) {
            jdbcTemplate.execute("DROP INDEX IF EXISTS " + INDEX_NAME);
        }
    }

    @Test
    @DisplayName("같은 활성 지역을 동시에 생성하면 정확히 1건만 성공한다")
    void concurrentCreateOnlyOneSucceeds() throws Exception {
        // given
        testName = "동시성-지역-" + UUID.randomUUID();
        ExecutorService executor = Executors.newFixedThreadPool(2);

        // 두 요청이 모두 출발선에 도착할 때까지 기다린다.
        CountDownLatch readyLatch = new CountDownLatch(2);
        // 출발선에서 기다리는 두 요청을 한 번에 출발시킨다.
        CountDownLatch startLatch = new CountDownLatch(1);

        Callable<CreateResult> createRequest = () -> {
            readyLatch.countDown();
            startLatch.await();

            try {
                regionService.createRegion(new RegionCreateRequest(testName, 0, true, true));
                return CreateResult.SUCCESS;
            } catch (CustomException exception) {
                if (exception.getErrorCode() == ErrorCode.DUPLICATE_REGION_NAME) {
                    return CreateResult.DUPLICATE;
                }
                throw exception;
            }
        };

        try {
            // submit은 요청을 실행할 스레드에 맡기고, Future는 나중에 결과를 받을 수 있는 표다.
            Future<CreateResult> first = executor.submit(createRequest);
            Future<CreateResult> second = executor.submit(createRequest);

            assertThat(readyLatch.await(10, TimeUnit.SECONDS)).isTrue();
            startLatch.countDown();

            // get은 각 요청이 끝날 때까지 기다리므로 별도의 완료용 doneLatch가 필요 없다.
            CreateResult firstResult = first.get(30, TimeUnit.SECONDS);
            CreateResult secondResult = second.get(30, TimeUnit.SECONDS);

            assertThat(List.of(firstResult, secondResult))
                    .containsExactlyInAnyOrder(CreateResult.SUCCESS, CreateResult.DUPLICATE);
        } finally {
            // 준비 과정에서 실패했더라도 대기 중인 스레드가 멈춰 있지 않게 신호를 연다.
            startLatch.countDown();
            executor.shutdownNow();
        }

        // DB에도 활성 지역이 정확히 한 건만 남아야 한다.
        Integer activeCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM p_region WHERE name = ? AND deleted_at IS NULL",
                Integer.class,
                testName
        );
        assertThat(activeCount).isEqualTo(1);
    }

    @Test
    @DisplayName("소프트 삭제된 지역 이름은 새 활성 지역에서 재사용할 수 있다")
    void softDeletedNameCanBeReused() {
        // given
        testName = "재사용-지역-" + UUID.randomUUID();
        RegionCreateResponse deleted = regionService.createRegion(
                new RegionCreateRequest(testName, 0, true, true));
        regionService.deleteRegion(deleted.regionId(), 1L);

        // when
        RegionCreateResponse recreated = regionService.createRegion(
                new RegionCreateRequest(testName, 0, true, true));

        // then
        Integer totalCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM p_region WHERE name = ?",
                Integer.class,
                testName
        );
        Integer activeCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM p_region WHERE name = ? AND deleted_at IS NULL",
                Integer.class,
                testName
        );
        assertThat(recreated.regionId()).isNotEqualTo(deleted.regionId());
        assertThat(totalCount).isEqualTo(2);
        assertThat(activeCount).isEqualTo(1);
    }
}
