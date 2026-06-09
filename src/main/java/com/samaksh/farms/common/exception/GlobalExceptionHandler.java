package com.samaksh.farms.common.exception;

import com.samaksh.farms.common.dto.ApiResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(
            AuthenticationException.class
    )
    public ResponseEntity<ApiResponse<Object>>
    handleAuthenticationException(
            AuthenticationException ex
    ) {

        return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED)
                .body(
                        ApiResponse.builder()
                                .success(false)
                                .message(ex.getMessage())
                                .data(null)
                                .build()
                );
    }

    @ExceptionHandler(
            AccessDeniedException.class
    )
    public ResponseEntity<ApiResponse<Object>>
    handleAccessDeniedException(
            AccessDeniedException ex
    ) {

        return ResponseEntity
                .status(HttpStatus.FORBIDDEN)
                .body(
                        ApiResponse.builder()
                                .success(false)
                                .message(
                                        "You do not have permission to perform this action"
                                )
                                .data(null)
                                .build()
                );
    }

    @ExceptionHandler(
            MethodArgumentNotValidException.class
    )
    public ResponseEntity<ApiResponse<Object>>
    handleValidationException(
            MethodArgumentNotValidException ex
    ) {

        Map<String, String> errors =
                ex.getBindingResult()
                        .getFieldErrors()
                        .stream()
                        .collect(
                                Collectors.toMap(
                                        error -> error.getField(),
                                        error -> error.getDefaultMessage(),
                                        (existing, replacement) -> existing
                                )
                        );

        return ResponseEntity
                .badRequest()
                .body(
                        ApiResponse.builder()
                                .success(false)
                                .message("Validation failed")
                                .data(errors)
                                .build()
                );
    }

    @ExceptionHandler(
            ResourceNotFoundException.class
    )
    public ResponseEntity<ApiResponse<Object>>
    handleResourceNotFound(
            ResourceNotFoundException ex
    ) {

        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(
                        ApiResponse.builder()
                                .success(false)
                                .message(
                                        ex.getMessage()
                                )
                                .data(null)
                                .build()
                );
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ApiResponse<Object>>
    handleRuntimeException(
            RuntimeException ex
    ) {

        return ResponseEntity
                .badRequest()
                .body(
                        ApiResponse.builder()
                                .success(false)
                                .message(
                                        ex.getMessage()
                                )
                                .data(null)
                                .build()
                );
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Object>>
    handleException(
            Exception ex
    ) {

        return ResponseEntity
                .status(
                        HttpStatus.INTERNAL_SERVER_ERROR
                )
                .body(
                        ApiResponse.builder()
                                .success(false)
                                .message(
                                        "Internal server error"
                                )
                                .data(null)
                                .build()
                );
    }
}
