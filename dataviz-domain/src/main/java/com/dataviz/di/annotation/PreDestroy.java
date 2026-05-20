package com.dataviz.di.annotation;

import java.lang.annotation.*;

/**
 * Позначає метод, що виконується перед знищенням компоненту контейнером.
 * Використовується для вивільнення ресурсів.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
@Documented
public @interface PreDestroy {}