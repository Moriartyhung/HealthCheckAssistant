package com.healthcheckassistant.common.error;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import jakarta.servlet.http.HttpServletRequest;

@RestControllerAdvice
public class GlobalExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<ApiErrorResponse> notFound(NotFoundException exception) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ApiErrorResponse(exception.getMessage()));
    }

    @ExceptionHandler(HandlerMethodValidationException.class)
    public ResponseEntity<ApiErrorResponse> invalidRequest(HandlerMethodValidationException exception) {
        return ResponseEntity.badRequest().body(new ApiErrorResponse("請求參數不正確"));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> unexpected(Exception exception) {
        // Preserve framework request/routing statuses without exposing their detail text.
        if (exception instanceof ErrorResponse error && error.getStatusCode().is4xxClientError()) {
            String message = switch (error.getStatusCode().value()) {
                case 404 -> "找不到資源";
                case 405 -> "不支援此 HTTP 方法";
                default -> "請求參數不正確";
            };
            return new ResponseEntity<>(new ApiErrorResponse(message), error.getHeaders(), error.getStatusCode());
        }
        log.error("API request failed", exception);
        return ResponseEntity.internalServerError().body(new ApiErrorResponse("服務暫時無法使用"));
    }

    @ExceptionHandler(ConflictException.class)
    public ResponseEntity<ApiErrorResponse> conflict(ConflictException exception) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(new ApiErrorResponse(exception.getMessage()));
    }

    @ExceptionHandler({MethodArgumentNotValidException.class, HttpMessageNotReadableException.class})
    public ResponseEntity<ApiErrorResponse> invalidBody(Exception exception, HttpServletRequest request) {
        String message = request.getRequestURI().equals(request.getContextPath() + "/api/ws2/complete")
                ? "請提供 code1" : "請提供受檢者與站點代碼";
        return ResponseEntity.badRequest().body(new ApiErrorResponse(message));
    }
}
