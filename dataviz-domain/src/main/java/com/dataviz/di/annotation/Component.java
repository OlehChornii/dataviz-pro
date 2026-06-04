package com.dataviz.di.annotation;

import java.lang.annotation.*;

/**
 * Позначає клас як керований компонент DI-контейнера.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Documented
public @interface Component {
    String value() default "";
}