package com.dataviz.di.annotation;

import java.lang.annotation.*;

/**
 * Спеціалізація @Component для Data Access шару.
 * Позначає класи, що реалізують доступ до даних.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Documented
@Component
public @interface Repository {
    String value() default "";
}