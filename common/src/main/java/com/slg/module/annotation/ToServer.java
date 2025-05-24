package com.slg.module.annotation;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
@Target(ElementType.TYPE) // 类级别注解
@Retention(RetentionPolicy.RUNTIME)
public @interface ToServer {
//    String value();
}
