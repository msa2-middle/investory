package com.project.stock.investory.exception;

import org.springframework.http.HttpStatus;

public abstract class BusinessException extends RuntimeException {
    private final HttpStatus status;

    public BusinessException(String message, HttpStatus status) {
        super(message);
        this.status = status;
    }

    public HttpStatus getStatus() {
        return status;
    }
}
