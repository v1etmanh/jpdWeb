package com.jpd.web.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * ✅ Xử lý lỗi toàn cục cho các test Integration
 * Giúp tránh lỗi 500 khi gặp các exception mock từ ValidationResources.
 */
@RestControllerAdvice
public class SimpleExceptionHandler {

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Void> handleIllegalArgument(IllegalArgumentException ex) {
        return ResponseEntity.badRequest().build(); // 400
    }

    @ExceptionHandler(ModuleNotFoundException.class)
    public ResponseEntity<Void> handleModuleNotFound(ModuleNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).build(); // 404
    }

    @ExceptionHandler(UnauthorizedException.class)
    public ResponseEntity<Void> handleUnauthorized(UnauthorizedException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).build(); // 403
    }

    // Catch fallback (optional)
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Void> handleGeneral(Exception ex) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
    }
}
