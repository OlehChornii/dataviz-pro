package com.dataviz.di.annotation;

import java.lang.annotation.*;

/**
 * Позначає метод, що виконується після впровадження всіх залежностей.
 * Аналог @PostConstruct (Jakarta EE).
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
@Documented
public @interface PostConstruct {}