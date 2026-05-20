package com.dataviz.di.annotation;

import java.lang.annotation.*;

/**
 * Визначає Singleton-скоуп для компоненту.
 * Контейнер створює лише один екземпляр та повертає його при кожному запиті.
 * Якщо анотація відсутня — використовується Prototype-скоуп (новий об'єкт кожного разу).
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Documented
public @interface Singleton {}