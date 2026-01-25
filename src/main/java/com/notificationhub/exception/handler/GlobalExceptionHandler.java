package com.notificationhub.exception.handler;

import com.notificationhub.dto.response.ErrorResponse;
import com.notificationhub.exception.custom.InvalidCredentialsException;
import com.notificationhub.exception.custom.MessageDeliveryException;
import com.notificationhub.exception.custom.RateLimitExceededException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.convert.ConversionFailedException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.List;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    private final static String BAD_REQUEST_ERROR = "Bad Request";

    private ErrorResponse buildErrorResponse(HttpStatus status, String error, String message, List<String> details) {
        return ErrorResponse.builder()
                .status(status.value())
                .error(error)
                .message(message)
                .details(details)
                .timestamp(LocalDateTime.now())
                .build();
    }

    // ========== 4xx CLIENT ERRORS ==========

    @ExceptionHandler(RateLimitExceededException.class)
    public ResponseEntity<ErrorResponse> handleRateLimitExceeded(RateLimitExceededException ex) {
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                .body(buildErrorResponse(
                        HttpStatus.TOO_MANY_REQUESTS,
                        "Rate Limit Exceeded",
                        ex.getMessage(),
                        null));
    }

    @ExceptionHandler({MessageDeliveryException.class, IllegalArgumentException.class})
    public ResponseEntity<ErrorResponse> handleBadRequestExceptions(Exception ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(buildErrorResponse(
                        HttpStatus.BAD_REQUEST,
                        BAD_REQUEST_ERROR,
                        ex.getMessage(),
                        null));
    }

    @ExceptionHandler({MethodArgumentTypeMismatchException.class, ConversionFailedException.class, DateTimeParseException.class})
    public ResponseEntity<ErrorResponse> handleTypeMismatch(Exception ex) {
        String message = "Invalid parameter format";
        if (ex instanceof DateTimeParseException) {
            message = "Invalid date/time format";
        }
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(buildErrorResponse(
                        HttpStatus.BAD_REQUEST,
                        BAD_REQUEST_ERROR,
                        message,
                        null));
    }

    @ExceptionHandler({InvalidCredentialsException.class})
    public ResponseEntity<ErrorResponse> handleInvalidCredentials(InvalidCredentialsException ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(buildErrorResponse(
                        HttpStatus.UNAUTHORIZED,
                        "Unauthorized",
                        ex.getMessage(),
                        null));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationErrors(MethodArgumentNotValidException ex) {

        List<String> details = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .toList();

        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(buildErrorResponse(
                        HttpStatus.BAD_REQUEST,
                        "Validation Failed",
                        "Invalid request data",
                        details));
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleUnsupportedMediaType(HttpMediaTypeNotSupportedException ex) {
        return ResponseEntity.status(HttpStatus.UNSUPPORTED_MEDIA_TYPE)
                .body(buildErrorResponse(
                        HttpStatus.UNSUPPORTED_MEDIA_TYPE,
                        "Unsupported Media Type",
                        "Content type not supported: " + ex.getContentType(),
                        null));
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleMethodNotSupported(HttpRequestMethodNotSupportedException ex) {
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED)
                .body(buildErrorResponse(
                        HttpStatus.METHOD_NOT_ALLOWED,
                        "Method Not Allowed",
                        "Method '" + ex.getMethod() + "' not supported for this endpoint",
                        null));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleMessageNotReadable() {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(buildErrorResponse(
                        HttpStatus.BAD_REQUEST,
                        BAD_REQUEST_ERROR,
                        "Malformed JSON request",
                        null));
    }

    // ========== 5xx SERVER ERRORS ==========

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(Exception ex) {
        log.error("An unexpected internal server error ocurred", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(buildErrorResponse(
                        HttpStatus.INTERNAL_SERVER_ERROR,
                        "Internal Server Error",
                        "An unexpected error occurred",
                        null));
    }
}
