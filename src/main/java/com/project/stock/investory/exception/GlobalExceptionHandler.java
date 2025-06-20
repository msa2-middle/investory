package com.project.stock.investory.exception;

import jakarta.persistence.EntityNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice // 전역 예외 처리 클래스 (모든 컨트롤러에서 발생하는 예외 처리 담당)
public class GlobalExceptionHandler {

    /**
     * 비즈니스 로직에서 발생하는 커스텀 예외(BusinessException)를 처리
     * - 예: UserNotFoundException, DuplicateEmailException 등
     */
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<String> handleBusinessException(BusinessException e) {
        return ResponseEntity.status(e.getStatus()).body(e.getMessage());
    }

    /**
     * 요청 데이터 검증 실패시 발생하는 유효성 예외(MethodArgumentNotValidException) 처리
     * - @Valid 검증 실패 시 호출됨
     * - 첫 번째 필드 에러 메시지를 응답으로 내려줌
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<String> handleValidationException(MethodArgumentNotValidException e) {
        String errorMessage = e.getBindingResult().getFieldError().getDefaultMessage();
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorMessage);
    }

    /**
     * 요청 파라미터 누락 시 발생 (주로 쿼리 파라미터 누락)
     */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<String> handleMissingParamException(MissingServletRequestParameterException e) {
        String errorMessage = "필수 요청 파라미터 누락: " + e.getParameterName();
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorMessage);
    }

    /**
     * 요청 본문이 잘못되었을 때 (JSON 파싱 실패 등)
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<String> handleInvalidJson(HttpMessageNotReadableException e) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("요청 본문의 형식이 올바르지 않습니다.");
    }

    /**
     * JPA에서 EntityNotFoundException 발생 시 처리
     * (현재 우리 프로젝트에서는 대부분 도메인 커스텀 NotFoundException을 사용하고 있지만,
     * 혹시 다른 도메인에서 JPA getReference() 등을 사용할 경우를 대비하여 등록)
     */
    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<String> handleEntityNotFound(EntityNotFoundException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body("해당 데이터를 찾을 수 없습니다.");
    }
}