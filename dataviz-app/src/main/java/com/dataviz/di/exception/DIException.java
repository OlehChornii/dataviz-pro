package com.dataviz.di.exception;

public class DIException extends RuntimeException {

    public DIException(String message) {
        super(message);
    }

    public DIException(String message, Throwable cause) {
        super(message, cause);
    }
}