package com.dataviz.di.annotation;

import java.lang.annotation.*;

/**
 * Спеціалізація @Component для Service-шару.
 * Семантично позначає класи бізнес-логіки.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Documented
@Component
public @interface Service {
    String value() default "";
}