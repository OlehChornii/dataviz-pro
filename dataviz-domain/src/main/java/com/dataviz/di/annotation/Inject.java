package com.dataviz.di.annotation;

import java.lang.annotation.*;

/**
 * Позначає поле, конструктор або сетер для впровадження залежності.
 * Контейнер автоматично вирішує та впроваджує відповідну реалізацію.
 *
 * Аналог: @Inject (JSR-330) / @Autowired (Spring).
 *
 * Підтримувані цілі:
 * - FIELD     — впровадження у поле (рефлексія)
 * - CONSTRUCTOR — конструкторне впровадження (пріоритетне)
 * - METHOD    — сетерне впровадження
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD, ElementType.CONSTRUCTOR, ElementType.METHOD})
@Documented
public @interface Inject {
    /** true — контейнер не кидає виняток якщо залежність відсутня */
    boolean required() default true;
}