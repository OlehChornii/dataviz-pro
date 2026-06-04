package com.dataviz.di.exception;

public class CircularDependencyException extends DIException {

    public CircularDependencyException(String message) {
        super(message);
    }
}