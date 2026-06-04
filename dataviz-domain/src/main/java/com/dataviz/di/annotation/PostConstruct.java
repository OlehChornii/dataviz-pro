package com.dataviz.di.annotation;

import java.lang.annotation.*;

/**
 * Позначає метод, що виконується після впровадження всіх залежностей.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
@Documented
public @interface PostConstruct {}