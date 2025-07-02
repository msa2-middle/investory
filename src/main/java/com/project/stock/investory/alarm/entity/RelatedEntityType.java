package com.project.stock.investory.alarm.entity;

public enum RelatedEntityType {
    POST("게시글"),
    COMMENT("댓글"),
    STOCK("주식"),
    USER("사용자");

    private final String description;

    RelatedEntityType(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}