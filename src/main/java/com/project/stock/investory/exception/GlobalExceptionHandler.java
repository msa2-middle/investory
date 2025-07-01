package com.project.stock.investory.exception;

import jakarta.persistence.EntityNotFoundException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.servlet.NoHandlerFoundException;

import java.io.IOException;
import java.util.Map;

/**
 * 전역 예외를 JSON(일반 요청) 또는 204(no-content, SSE 요청)로 매핑한다.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    // ────────────────────────────── BIZ & VALIDATION ──────────────────────────────

    /** 비즈니스 로직 커스텀 예외 */
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<Map<String, String>> handleBusinessException(BusinessException e,
                                                                       NativeWebRequest req) {
        return wrap(e.getStatus(), e.getMessage(), req);
    }

    /** @Valid 검증 실패 */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> handleValidation(MethodArgumentNotValidException e,
                                                                NativeWebRequest req) {
        String msg = e.getBindingResult().getFieldError() == null ?
                "유효성 검사 실패" :
                e.getBindingResult().getFieldError().getDefaultMessage();
        return wrap(HttpStatus.BAD_REQUEST, msg, req);
    }

    /** 필수 파라미터 누락 */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<Map<String, String>> handleMissingParam(MissingServletRequestParameterException e,
                                                                  NativeWebRequest req) {
        return wrap(HttpStatus.BAD_REQUEST,
                "필수 요청 파라미터 누락: " + e.getParameterName(), req);
    }

    /** JSON 파싱 실패 */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, String>> handleInvalidJson(HttpMessageNotReadableException e,
                                                                 NativeWebRequest req) {
        return wrap(HttpStatus.BAD_REQUEST, "요청 본문의 형식이 올바르지 않습니다.", req);
    }

    /** JPA Entity 조회 실패 */
    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<Map<String, String>> handleEntityNotFound(EntityNotFoundException e,
                                                                    NativeWebRequest req) {
        return wrap(HttpStatus.NOT_FOUND, "해당 데이터를 찾을 수 없습니다.", req);
    }

    /** 존재하지 않는 URL */
    @ExceptionHandler(NoHandlerFoundException.class)
    public ResponseEntity<Map<String, String>> handleNotFound(NoHandlerFoundException e,
                                                              NativeWebRequest req) {
        return wrap(HttpStatus.NOT_FOUND, "요청하신 페이지를 찾을 수 없습니다.", req);
    }

    // ────────────────────────────── NETWORK & IO ──────────────────────────────

    /**
     * 클라이언트가 SSE 커넥션을 강제로 끊으면 Tomcat/Servlet 이 IOException("Connection reset by peer")을 던진다.
     * → 서버 로그만 지저분하므로 조용히 무시.
     */
    @ExceptionHandler(IOException.class)
    public ResponseEntity<Map<String, String>> handleIo(IOException e, NativeWebRequest req) {
        // (로그는 debug 레벨 정도로)
        return wrap(HttpStatus.NO_CONTENT, null, req); // 204 & body 없음
    }

    // ────────────────────────────── FALLBACK ──────────────────────────────

    /** 알 수 없는 서버 오류 */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, String>> handleGeneric(Exception e, NativeWebRequest req) {
        e.printStackTrace();
        return wrap(HttpStatus.INTERNAL_SERVER_ERROR, "서버 내부 오류가 발생했습니다.", req);
    }

    // ────────────────────────────── UTIL ──────────────────────────────

    private ResponseEntity<Map<String, String>> wrap(HttpStatus status,
                                                     String message,
                                                     NativeWebRequest req) {
        String accept = req.getHeader(HttpHeaders.ACCEPT);
        boolean isSse = MediaType.TEXT_EVENT_STREAM_VALUE.equalsIgnoreCase(accept);

        if (isSse) {
            // SSE : 빈 응답으로 닫음 (204 or 지정 status)
            return ResponseEntity.status(status).build();
        }
        // 일반 REST : JSON 응답
        if (message == null) message = status.getReasonPhrase();
        return ResponseEntity.status(status)
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("message", message));
    }
}
