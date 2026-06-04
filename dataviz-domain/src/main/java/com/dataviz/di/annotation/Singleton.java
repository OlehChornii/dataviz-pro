package com.dataviz.di.annotation;

import java.lang.annotation.*;

/**
 * Визначає Singleton-скоуп для компоненту.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Documented
public @interface Singleton {}