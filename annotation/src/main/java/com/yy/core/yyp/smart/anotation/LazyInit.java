package com.yy.core.yyp.smart.anotation;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.CLASS;

/**
 * 是否延迟初始化api
 * Created by liyong on 2018/10/17.
 */
@Documented
@Target(TYPE)
@Retention(CLASS)
public @interface LazyInit {
    boolean value() default false;
}
