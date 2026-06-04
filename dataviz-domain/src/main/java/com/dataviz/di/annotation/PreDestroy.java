package com.dataviz.di.annotation;

import java.lang.annotation.*;

/**
 * Позначає метод, що виконується перед знищенням компоненту контейнером.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
@Documented
public @interface PreDestroy {}