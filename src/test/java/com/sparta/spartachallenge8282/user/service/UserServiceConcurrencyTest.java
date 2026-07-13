package com.sparta.spartachallenge8282.user.service;

import com.sparta.spartachallenge8282.global.exception.CustomException;
import com.sparta.spartachallenge8282.global.exception.ErrorCode;
import com.sparta.spartachallenge8282.global.security.JwtProvider;
import com.sparta.spartachallenge8282.user.entity.User;
import com.sparta.spartachallenge8282.user.entity.UserRole;
import com.sparta.spartachallenge8282.user.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest //비관적 락 테스트시 필요
class UserServiceConcurrencyTest {

    @Autowired
    private UserService userService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtProvider jwtProvider;

    private User savedUser;
    private String validRefreshToken;
    private String bearerToken;

    @AfterEach
    void tearDown() {
        if (savedUser != null) {
            userRepository.delete(savedUser);
        }
    }

    @Test
    @DisplayName("토큰 재발급(reissue) 동시성 테스트 - 비관적 락을 통해 1건만 성공하고 나머지는 INVALID_REFRESH_TOKEN 발생")
    void reissue_concurrency() throws InterruptedException {
        // given
        savedUser = userRepository.save(User.builder()
                .email("concurrency@sparta.com")
                .password("encodedPassword")
                .nickname("동시성테스터")
                .address("서울")
                .role(UserRole.CUSTOMER)
                .build());

        // Refresh 토큰 생성
        validRefreshToken = jwtProvider.createRefreshToken(savedUser.getEmail());
        //DB에 저장
        savedUser.updateRefreshToken(validRefreshToken);
        //즉시 DB 반영
        userRepository.saveAndFlush(savedUser);

        bearerToken = "Bearer " + validRefreshToken;

        int threadCount = 5;
        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch endLatch = new CountDownLatch(threadCount);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);

        // when
        for (int i = 0; i < threadCount; i++) {
            executorService.submit(() -> { //5명 준비 -> 땅!
                try {
                    startLatch.await();
                    userService.reissue(bearerToken);
                    successCount.incrementAndGet();
                } catch (CustomException e) {
                    if (e.getErrorCode() == ErrorCode.INVALID_REFRESH_TOKEN) {
                        failCount.incrementAndGet();
                    } else {
                        System.err.println("Unexpected CustomException: " + e.getErrorCode());
                    }
                } catch (Exception e) {
                    System.err.println("Unexpected Exception: " + e.getMessage());
                    e.printStackTrace();
                } finally {
                    endLatch.countDown();
                }
            });
        }

        startLatch.countDown(); // 대기 중인 모든 스레드를 동시에 실행
        endLatch.await();
        executorService.shutdown();

        System.out.println("Success count: " + successCount.get());
        System.out.println("Fail count: " + failCount.get());
        // then
        assertThat(successCount.get()).isEqualTo(1); // 오직 1건만 성공해야 한다
        assertThat(failCount.get()).isEqualTo(threadCount - 1); // 나머지는 예외가 발생해야 한다
    }
}
