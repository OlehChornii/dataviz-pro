package com.dataviz.di.annotation;

import java.lang.annotation.*;

/**
 * Позначає поле, конструктор або сетер для впровадження залежності.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD, ElementType.CONSTRUCTOR, ElementType.METHOD})
@Documented
public @interface Inject {
    boolean required() default true;
}