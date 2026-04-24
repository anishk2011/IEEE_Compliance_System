package com.ieee.pdfchecker.controller;

import com.ieee.pdfchecker.dto.ApiErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;

import java.time.LocalDateTime;

@RestControllerAdvice
public class ApiExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(ApiExceptionHandler.class);

    @ExceptionHandler({
            MissingServletRequestPartException.class,
            MissingServletRequestParameterException.class,
            MultipartException.class,
            MethodArgumentTypeMismatchException.class,
            MethodArgumentNotValidException.class,
            MaxUploadSizeExceededException.class
    })
    public ResponseEntity<ApiErrorResponse> handleBadRequest(Exception ex, HttpServletRequest request) {
        logger.warn("Request validation error for [{}]: {}", request.getRequestURI(), ex.getMessage());
        return buildError(
                HttpStatus.BAD_REQUEST,
                resolveMessage(ex),
                request.getRequestURI()
        );
    }

    private ResponseEntity<ApiErrorResponse> buildError(HttpStatus status, String message, String path) {
        return ResponseEntity.status(status).body(
                new ApiErrorResponse(
                        LocalDateTime.now(),
                        status.value(),
                        status.getReasonPhrase(),
                        message,
                        path
                )
        );
    }

    private String resolveMessage(Exception ex) {
        if (ex instanceof MissingServletRequestPartException) {
            return "Upload request must include a file part named 'file'.";
        }
        if (ex instanceof MissingServletRequestParameterException missingParameter) {
            return "Missing request parameter: " + missingParameter.getParameterName();
        }
        if (ex instanceof MaxUploadSizeExceededException) {
            return "Uploaded file exceeds the maximum allowed size.";
        }
        if (ex instanceof MultipartException) {
            return "Malformed multipart upload request.";
        }
        if (ex instanceof MethodArgumentTypeMismatchException mismatch) {
            return "Invalid value for parameter: " + mismatch.getName();
        }
        return "Invalid request.";
    }
}
