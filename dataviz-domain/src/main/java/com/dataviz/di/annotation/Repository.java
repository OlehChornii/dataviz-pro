package com.dataviz.di.annotation;

import java.lang.annotation.*;

/**
 * Спеціалізація
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Documented
@Component
public @interface Repository {
    String value() default "";
}