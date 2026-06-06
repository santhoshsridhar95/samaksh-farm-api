package com.samaksh.farms.common;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(RuntimeException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Map<String,String> handleRuntimeException(
            RuntimeException ex
    ) {

        return Map.of(
                "message",
                ex.getMessage()
        );
    }
}