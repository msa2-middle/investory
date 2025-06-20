package com.project.stock.investory.post.exception;

// 인증 필요 예외
public class AuthenticationRequiredException extends RuntimeException {
    public AuthenticationRequiredException() {
        super("로그인이 필요합니다.");
    }
}