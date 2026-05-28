package com.paystream.api.exception;

import org.springframework.http.HttpStatus;
import lombok.Getter;

@Getter
public class PayStreamException extends RuntimeException {
    private final HttpStatus status;

    public PayStreamException(String message, HttpStatus status) {
        super(message);
        this.status = status;
    }
}