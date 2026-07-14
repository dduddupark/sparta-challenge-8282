package com.sparta.spartachallenge8282.region.application;

import com.sparta.spartachallenge8282.global.exception.CustomException;
import com.sparta.spartachallenge8282.global.exception.ErrorCode;
import com.sparta.spartachallenge8282.region.domain.Region;
import com.sparta.spartachallenge8282.region.domain.RegionRepository;
import com.sparta.spartachallenge8282.region.presentation.dto.request.RegionCreateRequest;
import com.sparta.spartachallenge8282.region.presentation.dto.request.RegionUpdateRequest;
import com.sparta.spartachallenge8282.region.presentation.dto.response.RegionCreateResponse;
import com.sparta.spartachallenge8282.region.presentation.dto.response.RegionDeleteResponse;
import com.sparta.spartachallenge8282.region.presentation.dto.response.RegionResponse;
import com.sparta.spartachallenge8282.store.domain.StoreRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class RegionServiceTest {

    @Mock
    private RegionRepository regionRepository;

    @Mock
    private StoreRepository storeRepository;

    @InjectMocks
    private RegionService regionService;

    @Test
    void 지역생성_성공하면_생성된_id를_반환한다() {
        // given
        RegionCreateRequest request = new RegionCreateRequest("광화문", 1, true, false);
        given(regionRepository.existsByNameAndDeletedAtIsNull("광화문")).willReturn(false);

        UUID generatedId = UUID.randomUUID();
        Region saved = Region.builder()
                .name("광화문").sortOrder(1).isActive(true).isServiceAvailable(false)
                .build();
        ReflectionTestUtils.setField(saved, "id", generatedId);
        given(regionRepository.save(any(Region.class))).willReturn(saved);

        // when
        RegionCreateResponse result = regionService.createRegion(request);

        // then
        assertThat(result.regionId()).isEqualTo(generatedId);
    }

    @Test
    void 지역생성_이름중복이면_DUPLICATE_REGION_NAME() {
        // given
        RegionCreateRequest request = new RegionCreateRequest("광화문", 1, true, false);
        given(regionRepository.existsByNameAndDeletedAtIsNull("광화문")).willReturn(true);

        // when
        CustomException exception = assertThrows(CustomException.class,
                () -> regionService.createRegion(request));

        // then
        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.DUPLICATE_REGION_NAME);

        verify(regionRepository, never()).save(any());   // save가 호출되지 않았는지 확인
    }

    @Test
    void 단건조회_성공하면_RegionResponse를_반환한다() {
        // given
        UUID id = UUID.randomUUID();
        Region region = Region.builder()
                .name("광화문").sortOrder(1).isActive(true).isServiceAvailable(false)
                .build();
        ReflectionTestUtils.setField(region, "id", id);   // 빌더로는 id를 못 넣어서 주입
        given(regionRepository.findByIdAndDeletedAtIsNullAndIsActiveTrue(id)).willReturn(Optional.of(region));

        // when
        RegionResponse result = regionService.getRegion(id);

        // then
        assertThat(result.regionId()).isEqualTo(id);
        assertThat(result.name()).isEqualTo("광화문");
        assertThat(result.sortOrder()).isEqualTo(1);
        assertThat(result.isActive()).isTrue();
        assertThat(result.isServiceAvailable()).isFalse();
    }

    @Test
    void 단건조회_없는id는_REGION_NOT_FOUND를_던진다() {
        // given
        UUID id = UUID.randomUUID();
        given(regionRepository.findByIdAndDeletedAtIsNullAndIsActiveTrue(id))
                .willReturn(Optional.empty());

        // when
        CustomException exception = assertThrows(CustomException.class,
                () -> regionService.getRegion(id));

        // then
        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.REGION_NOT_FOUND);
    }

    @Test
    void 목록조회_성공하면_페이징된_RegionResponse를_반환한다() {
        // given
        Pageable pageable = PageRequest.of(0, 10);
        UUID id = UUID.randomUUID();
        Region region = Region.builder()
                .name("광화문").sortOrder(1).isActive(true).isServiceAvailable(false)
                .build();
        ReflectionTestUtils.setField(region, "id", id);
        Page<Region> page = new PageImpl<>(List.of(region), pageable, 1);   // 가짜 페이지 결과 반환

        given(regionRepository.searchRegions("광화문", true, pageable)).willReturn(page);

        // when
        Page<RegionResponse> result = regionService.getRegionList("광화문", pageable);

        // then
        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).regionId()).isEqualTo(region.getId());
        assertThat(result.getContent().get(0).name()).isEqualTo("광화문");
    }

    @Test
    void 목록조회_keyword가_null이면_빈문자열로_검색한다() {
        // given
        Pageable pageable = PageRequest.of(0, 3);
        Pageable normalized = PageRequest.of(0, 10);
        Page<Region> page = new PageImpl<>(List.of(), normalized, 0);

        given(regionRepository.searchRegions("", true, normalized)).willReturn(page);

        // when
        regionService.getRegionList(null, pageable);

        // then
        verify(regionRepository).searchRegions("", true, normalized);
    }

    @Test
    void 지역수정_성공하면_수정된_RegionResponse를_반환한다() {
        // given
        UUID id = UUID.randomUUID();
        Region region = Region.builder()
                .name("광화문").sortOrder(1).isActive(true).isServiceAvailable(false)
                .build();
        ReflectionTestUtils.setField(region, "id", id);
        RegionUpdateRequest request = new RegionUpdateRequest("종로", 2, false, true);

        given(regionRepository.findByIdAndDeletedAtIsNull(id)).willReturn(Optional.of(region));
        given(regionRepository.existsByNameAndDeletedAtIsNull("종로")).willReturn(false);

        // when
        RegionResponse result = regionService.updateRegion(id, request);

        // then
        assertThat(result.regionId()).isEqualTo(id);
        assertThat(result.name()).isEqualTo("종로");
        assertThat(result.sortOrder()).isEqualTo(2);
        assertThat(result.isActive()).isFalse();
        assertThat(result.isServiceAvailable()).isTrue();
    }

    @Test
    void 지역수정_없는id는_REGION_NOT_FOUND를_던진다() {
        // given
        UUID id = UUID.randomUUID();
        RegionUpdateRequest request = new RegionUpdateRequest("종로", 2, false, true);

        given(regionRepository.findByIdAndDeletedAtIsNull(id)).willReturn(Optional.empty());

        // when
        CustomException exception = assertThrows(CustomException.class,
                () -> regionService.updateRegion(id, request));

        // then
        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.REGION_NOT_FOUND);

        verify(regionRepository, never()).existsByNameAndDeletedAtIsNull(any());
    }

    @Test
    void 지역수정_이름중복이면_DUPLICATE_REGION_NAME을_던진다() {
        // given
        UUID id = UUID.randomUUID();
        Region region = Region.builder()
                .name("광화문").sortOrder(1).isActive(true).isServiceAvailable(false)
                .build();
        ReflectionTestUtils.setField(region, "id", id);
        RegionUpdateRequest request = new RegionUpdateRequest("종로", null, null, null);

        given(regionRepository.findByIdAndDeletedAtIsNull(id)).willReturn(Optional.of(region));
        given(regionRepository.existsByNameAndDeletedAtIsNull("종로")).willReturn(true);

        // when
        CustomException exception = assertThrows(CustomException.class,
                () -> regionService.updateRegion(id, request));

        // then
        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.DUPLICATE_REGION_NAME);
    }

    @Test
    void 지역삭제_성공하면_deletedAt을_반환하고_엔티티가_소프트삭제된다() {
        // given
        UUID id = UUID.randomUUID();
        Long userId = 1L;
        Region region = Region.builder()
                .name("광화문").sortOrder(1).isActive(true).isServiceAvailable(false)
                .build();
        ReflectionTestUtils.setField(region, "id", id);

        given(regionRepository.findById(id)).willReturn(Optional.of(region));
        given(storeRepository.existsByRegion_IdAndDeletedAtIsNull(id)).willReturn(false);

        // when
        RegionDeleteResponse result = regionService.deleteRegion(id, userId);

        // then
        assertThat(result.deletedAt()).isNotNull();
        assertThat(region.isDeleted()).isTrue();
        assertThat(region.getDeletedAt()).isEqualTo(result.deletedAt());
        assertThat(result.regionId()).isEqualTo(id);
        assertThat(region.getDeletedBy()).isEqualTo(userId);
    }

    @Test
    void 지역삭제_사용중인_지역이면_REGION_IN_USE를_던진다() {
        // given
        UUID id = UUID.randomUUID();
        Region region = Region.builder()
                .name("광화문").sortOrder(1).isActive(true).isServiceAvailable(false)
                .build();
        ReflectionTestUtils.setField(region, "id", id);

        given(regionRepository.findById(id)).willReturn(Optional.of(region));
        given(storeRepository.existsByRegion_IdAndDeletedAtIsNull(id)).willReturn(true);

        // when
        CustomException exception = assertThrows(CustomException.class,
                () -> regionService.deleteRegion(id, 1L));

        // then
        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.REGION_IN_USE);
        assertThat(region.isDeleted()).isFalse();
    }

    @Test
    void 지역삭제_없는id는_REGION_NOT_FOUND를_던진다() {
        // given
        UUID id = UUID.randomUUID();
        given(regionRepository.findById(id)).willReturn(Optional.empty());

        // when
        CustomException exception = assertThrows(CustomException.class,
                () -> regionService.deleteRegion(id, 1L));

        // then
        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.REGION_NOT_FOUND);
    }

    @Test
    void 지역삭제_이미삭제된지역이면_ALREADY_DELETED_REGION을_던진다() {
        // given
        UUID id = UUID.randomUUID();
        Region region = Region.builder()
                .name("광화문").sortOrder(1).isActive(true).isServiceAvailable(false)
                .build();
        ReflectionTestUtils.setField(region, "id", id);
        ReflectionTestUtils.setField(region, "deletedAt", LocalDateTime.now());

        given(regionRepository.findById(id)).willReturn(Optional.of(region));

        // when
        CustomException exception = assertThrows(CustomException.class,
                () -> regionService.deleteRegion(id, 1L));

        // then
        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.ALREADY_DELETED_REGION);
    }

}
