package com.dataviz.di.annotation;

import java.lang.annotation.*;

/**
 * Позначає клас як керований компонент DI-контейнера.
 * Контейнер автоматично реєструє такі класи під час сканування пакетів.
 *
 * Аналог: @Component у Spring Framework.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Documented
public @interface Component {
    /** Необов'язкова назва компоненту. Якщо порожня — використовується ім'я класу. */
    String value() default "";
}