package com.sparta.spartachallenge8282.store.domain;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.QueryFactory;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.sparta.spartachallenge8282.menu.domain.QMenu;
import com.sparta.spartachallenge8282.store.presentation.dto.request.StoresSearchCondition;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.List;

import static com.sparta.spartachallenge8282.store.domain.QStore.store;

@Repository
@RequiredArgsConstructor
public class StoreRepositoryCustomImpl implements StoreRepositoryCustom {


    private final JPAQueryFactory jpaQueryFactory;
    private final QMenu menu  = QMenu.menu;

    @Override
    public Page<Store> searchStores(StoresSearchCondition condition, Pageable pageable) {
       QStore store = QStore.store;

       BooleanBuilder whereCondition  = createWhereCondition(condition);

        List<Store> stores = jpaQueryFactory
                .selectFrom(store)
                .leftJoin(store.category).fetchJoin()
                .leftJoin(store.region).fetchJoin()
                .where(whereCondition)
                .orderBy(
                        resolveOrderSpecifiers(condition.sortType())
                )
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        Long totalStores = jpaQueryFactory
                .select(store.count())
                .from(store)
                .where(whereCondition)
                .fetchOne();

        return new PageImpl<>(
                stores,
                pageable,
                totalStores == null ? 0L : totalStores
        );
    }


    private BooleanBuilder createWhereCondition(StoresSearchCondition condition) {
        BooleanBuilder builder = new BooleanBuilder();

        builder.and(
                store.operationStatus.eq(StoreOperationStatus.ACTIVE)
        );

        builder.and(store.deletedAt.isNull());

        if(condition.categoryId() != null){
            builder.and(store.category.id.eq(condition.categoryId()));
        }
        if (condition.regionId() != null) {
            builder.and(
                    store.region.id.eq(condition.regionId())
            );
        }
        if(condition.isOpen() != null){
            builder.and(store.isOpen.eq(condition.isOpen()));
        }
        if(condition.keyword() != null && !condition.keyword().isEmpty()){
            String keyword = condition.keyword().trim();

            builder.and(
                    store.storeName.containsIgnoreCase(keyword)
                            .or(
                                    JPAExpressions
                                            .selectOne()
                                            .from(menu)
                                            .where(
                                                    menu.storeId.eq(store.id),
                                                    menu.deletedAt.isNull(),
                                                    menu.isHidden.isFalse(),
                                                    menu.name.containsIgnoreCase(keyword)
                                            )
                                            .exists()
                            )
            );
        }
        return builder;
    }

    private OrderSpecifier<?>[] resolveOrderSpecifiers(StoreSortType sortType) {
        if(sortType == null){
            return new OrderSpecifier<?>[]{
                    store.createdAt.desc(),
                    store.id.asc()
            };
        }
        return switch (sortType){
            case LATEST ->  new OrderSpecifier<?>[]{
                    store.createdAt.desc(),
                    store.id.asc()
            };
            case RATING_DESC ->   new OrderSpecifier<?>[]{
                    store.storeRating.desc(),
                    store.reviewCount.desc(),
                    store.id.asc()
            };
            case REVIEW_COUNT_DESC ->    new OrderSpecifier<?>[]{
                    store.reviewCount.desc(),
                    store.storeRating.desc(),
                    store.id.asc()
            };
            case DELIVERY_FEE_ASC ->     new OrderSpecifier<?>[]{
                    store.deliveryFee.asc(),
                    store.storeRating.desc(),
                    store.id.asc()
            };
        };
    }
}
