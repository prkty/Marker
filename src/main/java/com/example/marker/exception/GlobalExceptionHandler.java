package com.example.marker.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 애플리케이션 전역에서 발생하는 예외를 처리하는 클래스입니다.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * BookmarkNotFoundException이 발생했을 때 처리하는 핸들러입니다.
     * HTTP 404 Not Found 상태 코드와 에러 메시지를 담은 응답을 반환합니다.
     * @param ex 발생한 예외
     * @return 에러 정보를 담은 ResponseEntity
     */
    @ExceptionHandler(BookmarkNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleBookmarkNotFoundException(BookmarkNotFoundException ex, WebRequest request) {
        ErrorResponse errorResponse = ErrorResponse.of(
                HttpStatus.NOT_FOUND,
                ex.getMessage(),
                request.getDescription(false).replace("uri=", "")
        );
        return new ResponseEntity<>(errorResponse, HttpStatus.NOT_FOUND);
    }

    /**
     * @Valid 어노테이션을 사용한 유효성 검증 실패 시 발생하는 MethodArgumentNotValidException을 처리합니다.
     * HTTP 400 Bad Request 상태 코드와 필드별 에러 정보를 담은 응답을 반환합니다.
     * @param ex 발생한 예외
     * @param request 웹 요청 정보
     * @return 에러 정보를 담은 ResponseEntity
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleMethodArgumentNotValid(MethodArgumentNotValidException ex, WebRequest request) {
        List<ErrorResponse.FieldError> fieldErrors = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> ErrorResponse.FieldError.builder()
                        .field(error.getField())
                        .value(error.getRejectedValue() != null ? error.getRejectedValue().toString() : null)
                        .reason(error.getDefaultMessage())
                        .build())
                .collect(Collectors.toList());

        ErrorResponse errorResponse = ErrorResponse.of(HttpStatus.BAD_REQUEST, "입력 값에 대한 유효성 검증에 실패했습니다.", request.getDescription(false).replace("uri=", ""), fieldErrors);
        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }

    /**
     * UserAlreadyExistsException이 발생했을 때 처리하는 핸들러입니다.
     * HTTP 409 Conflict 상태 코드와 에러 메시지를 담은 응답을 반환합니다.
     * @param ex 발생한 예외
     * @param request 웹 요청 정보
     * @return 에러 정보를 담은 ResponseEntity
     */
    @ExceptionHandler(UserAlreadyExistsException.class)
    public ResponseEntity<ErrorResponse> handleUserAlreadyExistsException(UserAlreadyExistsException ex, WebRequest request) {
        ErrorResponse errorResponse = ErrorResponse.of(
                HttpStatus.CONFLICT, // 409 Conflict
                ex.getMessage(),
                request.getDescription(false).replace("uri=", "")
        );
        return new ResponseEntity<>(errorResponse, HttpStatus.CONFLICT);
    }

    /**
     * InvalidCredentialsException이 발생했을 때 처리하는 핸들러입니다.
     * HTTP 401 Unauthorized 상태 코드와 에러 메시지를 담은 응답을 반환합니다.
     * @param ex 발생한 예외
     * @param request 웹 요청 정보
     * @return 에러 정보를 담은 ResponseEntity
     */
    @ExceptionHandler(InvalidCredentialsException.class)
    public ResponseEntity<ErrorResponse> handleInvalidCredentialsException(InvalidCredentialsException ex, WebRequest request) {
        ErrorResponse errorResponse = ErrorResponse.of(HttpStatus.UNAUTHORIZED, ex.getMessage(), request.getDescription(false).replace("uri=", ""));
        return new ResponseEntity<>(errorResponse, HttpStatus.UNAUTHORIZED);
    }

    /**
     * UnauthorizedBookmarkAccessException이 발생했을 때 처리하는 핸들러입니다.
     * HTTP 403 Forbidden 상태 코드와 에러 메시지를 담은 응답을 반환합니다.
     * @param ex 발생한 예외
     * @param request 웹 요청 정보
     * @return 에러 정보를 담은 ResponseEntity
     */
    @ExceptionHandler(UnauthorizedBookmarkAccessException.class)
    public ResponseEntity<ErrorResponse> handleUnauthorizedBookmarkAccessException(UnauthorizedBookmarkAccessException ex, WebRequest request) {
        ErrorResponse errorResponse = ErrorResponse.of(
                HttpStatus.FORBIDDEN,
                ex.getMessage(),
                request.getDescription(false).replace("uri=", ""));
        return new ResponseEntity<>(errorResponse, HttpStatus.FORBIDDEN);
    }
}