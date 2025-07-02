package com.project.stock.investory.alarm.entity;

public enum AlarmType {
    // 게시글 관련 알람
    POST_LIKE("게시글 좋아요"),
    POST_COMMENT("게시글 댓글"),

    // 댓글 관련 알람
    COMMENT_LIKE("댓글 좋아요"),

    // 주식 관련 알람
    STOCK_PRICE("주식 가격");

    private final String displayName;

    AlarmType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}