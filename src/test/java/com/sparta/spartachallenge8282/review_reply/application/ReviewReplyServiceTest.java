package com.sparta.spartachallenge8282.review_reply.application;

import com.sparta.spartachallenge8282.global.exception.CustomException;
import com.sparta.spartachallenge8282.review.domain.Review;
import com.sparta.spartachallenge8282.review.domain.ReviewRepository;
import com.sparta.spartachallenge8282.review.presentation.dto.request.ReviewCreateRequestDto;
import com.sparta.spartachallenge8282.review_reply.domain.ReviewReply;
import com.sparta.spartachallenge8282.review_reply.domain.ReviewReplyRepository;
import com.sparta.spartachallenge8282.review_reply.presentation.dto.ReviewReplyRequestDto;
import com.sparta.spartachallenge8282.review_reply.presentation.dto.ReviewReplyResponseDto;
import com.sparta.spartachallenge8282.store.domain.Store;
import com.sparta.spartachallenge8282.store.domain.StoreRepository;
import com.sparta.spartachallenge8282.user.domain.User;
import com.sparta.spartachallenge8282.user.domain.UserRole;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReviewReplyServiceTest {

    @Mock
    private ReviewReplyRepository reviewReplyRepository;
    @Mock
    private ReviewRepository reviewRepository;
    @Mock
    private StoreRepository storeRepository;

    @InjectMocks
    private ReviewReplyService reviewReplyService;

    private void printException(Throwable e) {
        CustomException ex = (CustomException) e;
        System.out.println("예외 발생: " + ex.getErrorCode().getCode() + " - " + ex.getErrorCode().getMessage());
    }

    private User createOwner(Long ownerId) {
        User owner = User.builder()
                .email("owner@test.com")
                .password("encoded-pw")
                .nickname("사장님")
                .address("서울시 종로구")
                .role(UserRole.OWNER)
                .build();
        ReflectionTestUtils.setField(owner, "id", ownerId);
        return owner;
    }

    private Store createStore(User owner) {
        Store store = Store.builder()
                .owner(owner)
                .build();
        ReflectionTestUtils.setField(store, "id", UUID.randomUUID());
        return store;
    }

    // ── createReply ──────────────────────────────────────────

    @Test
    @DisplayName("답글 생성 성공")
    void createReplyTest() {
        // given
        UUID reviewId = UUID.randomUUID();
        UUID storeId = UUID.randomUUID();
        Long ownerId = 1L;

        Review review = Review.builder()
                .requestDto(new ReviewCreateRequestDto(UUID.randomUUID(), 5, "맛있어요", null))
                .userId(999L)
                .storeId(storeId)
                .build();
        ReflectionTestUtils.setField(review, "id", reviewId);

        User owner = createOwner(ownerId);
        Store store = createStore(owner);
        ReflectionTestUtils.setField(store, "id", storeId);

        ReviewReplyRequestDto requestDto = new ReviewReplyRequestDto("감사합니다!");

        when(reviewRepository.findByIdAndDeletedAtIsNull(reviewId)).thenReturn(Optional.of(review));
        when(storeRepository.findById(storeId)).thenReturn(Optional.of(store));
        when(reviewReplyRepository.existsByReviewIdAndDeletedAtIsNull(reviewId)).thenReturn(false);

        ReviewReply savedReply = ReviewReply.builder()
                .reviewId(reviewId)
                .storeId(storeId)
                .content(requestDto.content())
                .build();
        when(reviewReplyRepository.save(org.mockito.ArgumentMatchers.any(ReviewReply.class))).thenReturn(savedReply);

        // when
        ReviewReplyResponseDto result = reviewReplyService.createReply(requestDto, reviewId, ownerId);
        System.out.println("결과: " + result);

        // then
        assertThat(result).isNotNull();
    }

    @Test
    @DisplayName("답글 생성 실패: 리뷰 없음")
    void createReplyTest_fail_review_not_found() {
        // given
        UUID reviewId = UUID.randomUUID();
        ReviewReplyRequestDto requestDto = new ReviewReplyRequestDto("감사합니다!");

        when(reviewRepository.findByIdAndDeletedAtIsNull(reviewId)).thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> reviewReplyService.createReply(requestDto, reviewId, 1L))
                .isInstanceOf(CustomException.class)
                .satisfies(this::printException);
    }

    @Test
    @DisplayName("답글 생성 실패: 가게 없음")
    void createReplyTest_fail_store_not_found() {
        // given
        UUID reviewId = UUID.randomUUID();
        UUID storeId = UUID.randomUUID();

        Review review = Review.builder()
                .requestDto(new ReviewCreateRequestDto(UUID.randomUUID(), 5, "맛있어요", null))
                .userId(999L)
                .storeId(storeId)
                .build();
        ReflectionTestUtils.setField(review, "id", reviewId);

        ReviewReplyRequestDto requestDto = new ReviewReplyRequestDto("감사합니다!");

        when(reviewRepository.findByIdAndDeletedAtIsNull(reviewId)).thenReturn(Optional.of(review));
        when(storeRepository.findById(storeId)).thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> reviewReplyService.createReply(requestDto, reviewId, 1L))
                .isInstanceOf(CustomException.class)
                .satisfies(this::printException);
    }

    @Test
    @DisplayName("답글 생성 실패: 가게 소유주가 아님")
    void createReplyTest_fail_not_owner() {
        // given
        UUID reviewId = UUID.randomUUID();
        UUID storeId = UUID.randomUUID();
        Long ownerId = 1L;
        Long otherUserId = 999L;

        Review review = Review.builder()
                .requestDto(new ReviewCreateRequestDto(UUID.randomUUID(), 5, "맛있어요", null))
                .userId(500L)
                .storeId(storeId)
                .build();
        ReflectionTestUtils.setField(review, "id", reviewId);

        User owner = createOwner(ownerId);
        Store store = createStore(owner);

        ReviewReplyRequestDto requestDto = new ReviewReplyRequestDto("감사합니다!");

        when(reviewRepository.findByIdAndDeletedAtIsNull(reviewId)).thenReturn(Optional.of(review));
        when(storeRepository.findById(storeId)).thenReturn(Optional.of(store));

        // when & then - otherUserId(가게주인 아님)가 답글 작성 시도
        assertThatThrownBy(() -> reviewReplyService.createReply(requestDto, reviewId, otherUserId))
                .isInstanceOf(CustomException.class)
                .satisfies(this::printException);
    }

    @Test
    @DisplayName("답글 생성 실패: 이미 답글 존재")
    void createReplyTest_fail_already_exists() {
        // given
        UUID reviewId = UUID.randomUUID();
        UUID storeId = UUID.randomUUID();
        Long ownerId = 1L;

        Review review = Review.builder()
                .requestDto(new ReviewCreateRequestDto(UUID.randomUUID(), 5, "맛있어요", null))
                .userId(999L)
                .storeId(storeId)
                .build();
        ReflectionTestUtils.setField(review, "id", reviewId);

        User owner = createOwner(ownerId);
        Store store = createStore(owner);

        ReviewReplyRequestDto requestDto = new ReviewReplyRequestDto("감사합니다!");

        when(reviewRepository.findByIdAndDeletedAtIsNull(reviewId)).thenReturn(Optional.of(review));
        when(storeRepository.findById(storeId)).thenReturn(Optional.of(store));
        when(reviewReplyRepository.existsByReviewIdAndDeletedAtIsNull(reviewId)).thenReturn(true);

        // when & then
        assertThatThrownBy(() -> reviewReplyService.createReply(requestDto, reviewId, ownerId))
                .isInstanceOf(CustomException.class)
                .satisfies(this::printException);
    }

    // ── updateReply ──────────────────────────────────────────

    @Test
    @DisplayName("답글 수정 성공")
    void updateReplyTest() {
        // given
        UUID reviewId = UUID.randomUUID();
        UUID storeId = UUID.randomUUID();
        Long ownerId = 1L;

        ReviewReply reviewReply = ReviewReply.builder()
                .reviewId(reviewId)
                .storeId(storeId)
                .content("기존 답글")
                .build();

        User owner = createOwner(ownerId);
        Store store = createStore(owner);

        ReviewReplyRequestDto requestDto = new ReviewReplyRequestDto("수정된 답글입니다");

        when(reviewReplyRepository.findByReviewIdAndDeletedAtIsNull(reviewId)).thenReturn(Optional.of(reviewReply));
        when(storeRepository.findById(storeId)).thenReturn(Optional.of(store));

        // when
        ReviewReplyResponseDto result = reviewReplyService.updateReply(requestDto, reviewId, ownerId);
        System.out.println("결과: " + result);

        // then
        assertThat(result).isNotNull();
    }

    @Test
    @DisplayName("답글 수정 실패: 답글 없음")
    void updateReplyTest_fail_not_found() {
        // given
        UUID reviewId = UUID.randomUUID();
        ReviewReplyRequestDto requestDto = new ReviewReplyRequestDto("수정된 답글입니다");

        when(reviewReplyRepository.findByReviewIdAndDeletedAtIsNull(reviewId)).thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> reviewReplyService.updateReply(requestDto, reviewId, 1L))
                .isInstanceOf(CustomException.class)
                .satisfies(this::printException);
    }

    @Test
    @DisplayName("답글 수정 실패: 가게 없음")
    void updateReplyTest_fail_store_not_found() {
        // given
        UUID reviewId = UUID.randomUUID();
        UUID storeId = UUID.randomUUID();

        ReviewReply reviewReply = ReviewReply.builder()
                .reviewId(reviewId)
                .storeId(storeId)
                .content("기존 답글")
                .build();

        ReviewReplyRequestDto requestDto = new ReviewReplyRequestDto("수정된 답글입니다");

        when(reviewReplyRepository.findByReviewIdAndDeletedAtIsNull(reviewId)).thenReturn(Optional.of(reviewReply));
        when(storeRepository.findById(storeId)).thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> reviewReplyService.updateReply(requestDto, reviewId, 1L))
                .isInstanceOf(CustomException.class)
                .satisfies(this::printException);
    }

    @Test
    @DisplayName("답글 수정 실패: 가게 소유주가 아님")
    void updateReplyTest_fail_not_owner() {
        // given
        UUID reviewId = UUID.randomUUID();
        UUID storeId = UUID.randomUUID();
        Long ownerId = 1L;
        Long otherUserId = 999L;

        ReviewReply reviewReply = ReviewReply.builder()
                .reviewId(reviewId)
                .storeId(storeId)
                .content("기존 답글")
                .build();

        User owner = createOwner(ownerId);
        Store store = createStore(owner);

        ReviewReplyRequestDto requestDto = new ReviewReplyRequestDto("수정된 답글입니다");

        when(reviewReplyRepository.findByReviewIdAndDeletedAtIsNull(reviewId)).thenReturn(Optional.of(reviewReply));
        when(storeRepository.findById(storeId)).thenReturn(Optional.of(store));

        // when & then
        assertThatThrownBy(() -> reviewReplyService.updateReply(requestDto, reviewId, otherUserId))
                .isInstanceOf(CustomException.class)
                .satisfies(this::printException);
    }

    // ── deleteReply ──────────────────────────────────────────

    @Test
    @DisplayName("답글 삭제 성공: 본인(가게주인)")
    void deleteReplyTest_owner() {
        // given
        UUID reviewId = UUID.randomUUID();
        UUID storeId = UUID.randomUUID();
        Long ownerId = 1L;

        ReviewReply reviewReply = ReviewReply.builder()
                .reviewId(reviewId)
                .storeId(storeId)
                .content("삭제될 답글")
                .build();

        User owner = createOwner(ownerId);
        Store store = createStore(owner);

        when(reviewReplyRepository.findByReviewIdAndDeletedAtIsNull(reviewId)).thenReturn(Optional.of(reviewReply));
        when(storeRepository.findById(storeId)).thenReturn(Optional.of(store));

        // when & then
        reviewReplyService.deleteReply(reviewId, ownerId, "OWNER");
        System.out.println("삭제 완료: reviewId=" + reviewId + ", isDeleted=" + reviewReply.isDeleted());
    }

    @Test
    @DisplayName("답글 삭제 성공: MANAGER 권한")
    void deleteReplyTest_manager() {
        // given
        UUID reviewId = UUID.randomUUID();
        UUID storeId = UUID.randomUUID();
        Long ownerId = 1L;
        Long managerId = 999L;

        ReviewReply reviewReply = ReviewReply.builder()
                .reviewId(reviewId)
                .storeId(storeId)
                .content("삭제될 답글")
                .build();

        User owner = createOwner(ownerId);
        Store store = createStore(owner);

        when(reviewReplyRepository.findByReviewIdAndDeletedAtIsNull(reviewId)).thenReturn(Optional.of(reviewReply));
        when(storeRepository.findById(storeId)).thenReturn(Optional.of(store));

        // when & then
        reviewReplyService.deleteReply(reviewId, managerId, "MANAGER");
        System.out.println("삭제 완료: reviewId=" + reviewId + ", isDeleted=" + reviewReply.isDeleted());
    }

    @Test
    @DisplayName("답글 삭제 실패: 답글 없음")
    void deleteReplyTest_fail_not_found() {
        // given
        UUID reviewId = UUID.randomUUID();

        when(reviewReplyRepository.findByReviewIdAndDeletedAtIsNull(reviewId)).thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> reviewReplyService.deleteReply(reviewId, 1L, "CUSTOMER"))
                .isInstanceOf(CustomException.class)
                .satisfies(this::printException);
    }

    @Test
    @DisplayName("답글 삭제 실패: 가게 없음")
    void deleteReplyTest_fail_store_not_found() {
        // given
        UUID reviewId = UUID.randomUUID();
        UUID storeId = UUID.randomUUID();

        ReviewReply reviewReply = ReviewReply.builder()
                .reviewId(reviewId)
                .storeId(storeId)
                .content("삭제될 답글")
                .build();

        when(reviewReplyRepository.findByReviewIdAndDeletedAtIsNull(reviewId)).thenReturn(Optional.of(reviewReply));
        when(storeRepository.findById(storeId)).thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> reviewReplyService.deleteReply(reviewId, 1L, "CUSTOMER"))
                .isInstanceOf(CustomException.class)
                .satisfies(this::printException);
    }

    @Test
    @DisplayName("답글 삭제 실패: 본인도 아니고 권한도 없음")
    void deleteReplyTest_fail_no_permission() {
        // given
        UUID reviewId = UUID.randomUUID();
        UUID storeId = UUID.randomUUID();
        Long ownerId = 1L;
        Long otherUserId = 999L;

        ReviewReply reviewReply = ReviewReply.builder()
                .reviewId(reviewId)
                .storeId(storeId)
                .content("삭제될 답글")
                .build();

        User owner = createOwner(ownerId);
        Store store = createStore(owner);

        when(reviewReplyRepository.findByReviewIdAndDeletedAtIsNull(reviewId)).thenReturn(Optional.of(reviewReply));
        when(storeRepository.findById(storeId)).thenReturn(Optional.of(store));

        // when & then
        assertThatThrownBy(() -> reviewReplyService.deleteReply(reviewId, otherUserId, "CUSTOMER"))
                .isInstanceOf(CustomException.class)
                .satisfies(this::printException);
    }
}