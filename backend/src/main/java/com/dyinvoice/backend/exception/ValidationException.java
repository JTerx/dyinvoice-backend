package com.dyinvoice.backend.exception;


import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(value = HttpStatus.BAD_REQUEST)
public class ValidationException extends Exception {

    /**
     *
     */
    private static final long serialVersionUID = 1L;

    public ValidationException(String message) {
        super(message);
    }
}
