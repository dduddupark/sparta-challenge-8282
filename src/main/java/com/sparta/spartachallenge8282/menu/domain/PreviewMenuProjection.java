package com.sparta.spartachallenge8282.menu.domain;

import java.util.UUID;

public interface PreviewMenuProjection {
    UUID getMenuId();

    UUID getStoreId();

    String getName();

    Integer getPrice();

    Integer getSortOrder();

}
