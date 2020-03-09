package com.personal.wspring.annotation;

import java.lang.annotation.*;

/**
 * 自定义autowire注解
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface WAutowired {
    String value() default "";
}
