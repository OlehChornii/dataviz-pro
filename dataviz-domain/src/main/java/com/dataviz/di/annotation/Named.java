package com.dataviz.di.annotation;

import java.lang.annotation.*;

/**
 * Дозволяє вказати конкретну іменовану реалізацію при впровадженні.
 * Використовується коли для одного інтерфейсу існує кілька реалізацій.
 *
 * Приклад:
 *   @Inject @Named("csv") private DataReader reader;
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Documented
public @interface Named {
    String value();
}