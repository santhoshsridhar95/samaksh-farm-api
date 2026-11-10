package com.samaksh.farms.common.exception;

import com.samaksh.farms.common.dto.ApiResponse;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
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

    @ExceptionHandler({
            HttpMessageNotReadableException.class,
            MethodArgumentTypeMismatchException.class
    })
    public ResponseEntity<ApiResponse<Object>>
    handleInvalidRequestFormat(
            Exception ex
    ) {

        return ResponseEntity
                .badRequest()
                .body(
                        ApiResponse.builder()
                                .success(false)
                                .message(
                                        "Invalid request value. Please refresh and try again."
                                )
                                .data(null)
                                .build()
                );
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiResponse<Object>>
    handleDataIntegrityViolation(
            DataIntegrityViolationException ex
    ) {

        String state = null;
        String constraint = null;
        String databaseMessage = "";
        for (Throwable cause = ex; cause != null; cause = cause.getCause()) {
            if (cause instanceof java.sql.SQLException sql) { state = sql.getSQLState(); databaseMessage = sql.getMessage() == null ? "" : sql.getMessage(); }
            if (cause instanceof org.hibernate.exception.ConstraintViolationException violation) constraint = violation.getConstraintName();
        }
        if (constraint == null) {
            var match = java.util.regex.Pattern.compile("constraint \"([a-zA-Z0-9_]+)\"", java.util.regex.Pattern.CASE_INSENSITIVE).matcher(databaseMessage);
            if (match.find()) constraint = match.group(1);
        }
        // SQL statements contain created_by_email/updated_by_email even when email is unrelated.
        // Only a PostgreSQL unique-key detail identifying email proves a duplicate email.
        boolean duplicateEmail = "23505".equals(state) && java.util.regex.Pattern.compile("Key\\s*\\(email\\)\\s*=", java.util.regex.Pattern.CASE_INSENSITIVE).matcher(databaseMessage).find();
        String message = duplicateEmail ? "Email already exists"
                : "23505".equals(state) ? "A record with these unique details already exists."
                : "23503".equals(state) ? "A linked record is missing or still in use. Refresh and select valid records."
                : "23502".equals(state) ? "The database requires a value missing from this record."
                : "22001".equals(state) ? "A saved description exceeds the database column length. Restart the updated API to apply the correction-text migration."
                : "23514".equals(state) ? "A database validation rule rejected this correction."
                : "The database rejected this correction.";
        if (!duplicateEmail && constraint != null && constraint.matches("[a-zA-Z0-9_]{1,100}")) message += " Rule: " + constraint + ".";
        if (!duplicateEmail && state != null && state.matches("[A-Z0-9]{5}")) message += " Code: " + state + ".";
        org.slf4j.LoggerFactory.getLogger(GlobalExceptionHandler.class).error("Database write rejected: SQLState={}, constraint={}", state, constraint);

        return ResponseEntity
                .badRequest()
                .body(
                        ApiResponse.builder()
                                .success(false)
                                .message(message)
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
