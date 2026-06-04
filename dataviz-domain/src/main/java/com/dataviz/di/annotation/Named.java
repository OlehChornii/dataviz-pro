package com.dataviz.di.annotation;

import java.lang.annotation.*;

/**
 * Дозволяє вказати конкретну іменовану реалізацію при впровадженні.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Documented
public @interface Named {
    String value();
}