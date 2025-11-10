package com.jpd.web.controller.common;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import com.jpd.web.exception.BusinessException;
import com.jpd.web.exception.ChapterNotFoundException;
import com.jpd.web.exception.ErrorResponse;
import com.jpd.web.exception.FileUploadException;
import com.jpd.web.exception.UnauthorizedException;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import com.jpd.web.exception.*;
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    private String getPath(WebRequest request) {
        return request.getDescription(false).replace("uri=", "");
    }

    private String getTraceId() {
        return UUID.randomUUID().toString();
    }

    // ===== 404 NOT FOUND =====
    @ExceptionHandler({
            ChapterNotFoundException.class,
            CourseNotFoundException.class,
            ModuleNotFoundException.class,
            ModuleContentNotFoundException.class,
            CreatorNotFoundException.class,
            CustomerNotFoundException.class
    })
    public ResponseEntity<ErrorResponse> handleNotFound(
            BusinessException e,
            WebRequest request) {
        String traceId = getTraceId();
        log.warn("[{}] Resource not found: {}", traceId, e.getMessage());

        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(ErrorResponse.builder()
                        .success(false)
                        .status(404)
                        .code(e.getErrorCode())
                        .message(e.getMessage())
                        .userMessage("Tài nguyên không được tìm thấy")
                        .path(getPath(request))
                        .timestamp(LocalDateTime.now())
                        .traceId(traceId)
                        .responseType("NOT_FOUND")
                        .build());
    }

    // ===== 401 UNAUTHORIZED =====
    @ExceptionHandler(UnauthorizedException.class)
    public ResponseEntity<ErrorResponse> handleUnauthorized(
            UnauthorizedException e,
            WebRequest request) {
        String traceId = getTraceId();
        log.warn("[{}] Unauthorized access: {}", traceId, e.getMessage());

        return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED)
                .body(ErrorResponse.builder()
                        .code(e.getErrorCode())
                        .message(e.getMessage())
                        .userMessage("Bạn không có quyền truy cập")
                        .path(getPath(request))
                        .timestamp(LocalDateTime.now())
                        .traceId(traceId)
                        .build());
    }

    // ===== 409 CONFLICT - Duplicate/Already Exists =====
    @ExceptionHandler({
            EnrollmentExistException.class,
            CreatorAlreadyExistsException.class,
            PaymentEmailAlreadyExistsException.class,
            WishlistExistException.class
    })
    public ResponseEntity<ErrorResponse> handleConflict(
            BusinessException e,
            WebRequest request) {
        String traceId = getTraceId();
        log.warn("[{}] Conflict: {}", traceId, e.getMessage());

        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(ErrorResponse.builder()
                        .code(e.getErrorCode())
                        .message(e.getMessage())
                        .userMessage("Dữ liệu đã tồn tại hoặc bị trùng lặp")
                        .path(getPath(request))
                        .timestamp(LocalDateTime.now())
                        .traceId(traceId)
                        .build());
    }

    // ===== 400 BAD REQUEST - Business Logic Errors =====
    @ExceptionHandler({
            ExceedLimitRequestException.class,
            PayoutLimitExceededException.class,
            WithdrawException.class,
            FileUploadException.class,
            ApiException.class,
            AIHandlerException.class,
            FeedBackIligalException.class
    })
    public ResponseEntity<ErrorResponse> handleBadRequest(
            BusinessException e,
            WebRequest request) {
        String traceId = getTraceId();
        log.warn("[{}] Business error: {}", traceId, e.getMessage());

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ErrorResponse.builder()
                        .code(e.getErrorCode())
                        .message(e.getMessage())
                        .userMessage(e.getUserMessage())
                        .path(getPath(request))
                        .timestamp(LocalDateTime.now())
                        .details(e.getDetails())
                        .traceId(traceId)
                        .build());
    }

    // ===== 400 VALIDATION ERRORS =====
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(
            MethodArgumentNotValidException e,
            WebRequest request) {
        String traceId = getTraceId();
        log.warn("[{}] Validation error", traceId);

        Map<String, String> errors = new HashMap<>();
        e.getBindingResult().getFieldErrors().forEach(err ->
                errors.put(err.getField(), err.getDefaultMessage())
        );

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ErrorResponse.builder()
                        .code("VALIDATION_ERROR")
                        .message("Validation failed")
                        .userMessage("Dữ liệu nhập không hợp lệ")
                        .path(getPath(request))
                        .timestamp(LocalDateTime.now())
                        .details(errors)
                        .traceId(traceId)
                        .build());
    }
    //feedback
    

    // ===== 400 TYPE MISMATCH =====
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleTypeMismatch(
            MethodArgumentTypeMismatchException e,
            WebRequest request) {
        String traceId = getTraceId();
        log.warn("[{}] Type mismatch: {}", traceId, e.getMessage());

        String message = String.format(
                "Parameter '%s' should be of type %s",
                e.getName(),
                e.getRequiredType().getSimpleName()
        );

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ErrorResponse.builder()
                        .code("TYPE_MISMATCH")
                        .message(message)
                        .userMessage("Định dạng dữ liệu không đúng")
                        .path(getPath(request))
                        .timestamp(LocalDateTime.now())
                        .traceId(traceId)
                        .build());
    }

    // ===== 500 INTERNAL SERVER ERROR =====
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneral(
            Exception e,
            WebRequest request) {
        String traceId = getTraceId();
        log.error("[{}] Unexpected error", traceId, e);

        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ErrorResponse.builder()
                        .code("INTERNAL_ERROR")
                        .message(e.getMessage())
                        .userMessage("Có lỗi xảy ra trên server, vui lòng thử lại sau")
                        .path(getPath(request))
                        .timestamp(LocalDateTime.now())
                        .traceId(traceId)
                        .build());
    }
    @ExceptionHandler(CreatorIdNotFoundInRequestException.class)
    public ResponseEntity<ErrorResponse> handleCreatorIdNotFound(
            CreatorIdNotFoundInRequestException e,
            WebRequest request) {

        String traceId = getTraceId();
        log.warn("[{}] Creator ID not found: {}", traceId, e.getMessage());

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ErrorResponse.builder()
                        .code(e.getErrorCode())
                        .message(e.getMessage())
                        .userMessage("Có lỗi xảy ra khi xác thực tác giả")
                        .path(getPath(request))
                        .timestamp(LocalDateTime.now())
                        .traceId(traceId)
                        .build());
    }

    @ExceptionHandler(EmailSendingFailedException.class)
    public ResponseEntity<ErrorResponse> handleEmailSendingFailed(
            EmailSendingFailedException e,
            WebRequest request) {

        String traceId = getTraceId();
        log.error("[{}] Email sending failed: {}", traceId, e.getMessage());

        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ErrorResponse.builder()
                        .code(e.getErrorCode())
                        .message(e.getMessage())
                        .userMessage(e.getUserMessage())
                        .path(getPath(request))
                        .timestamp(LocalDateTime.now())
                        .traceId(traceId)
                        .build());
    }
}

// DTO cho error response
