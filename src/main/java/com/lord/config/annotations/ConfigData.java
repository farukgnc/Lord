package com.lord.config.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Bir alanın (field) config dosyasındaki bir yola
 * bağlanmasını sağlar.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface ConfigData {
    /**
     * @return Config dosyasındaki yolu (path). Örnek: "database.mongo-uri"
     */
    String value();
}