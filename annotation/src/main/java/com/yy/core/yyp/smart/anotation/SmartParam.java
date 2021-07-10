package com.yy.core.yyp.smart.anotation;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.RetentionPolicy.CLASS;

/**
 * Created by liyong on 2017/7/7.
 */
@Documented
@Target(PARAMETER)
@Retention(CLASS)
public @interface SmartParam {
    String value();//param name

    boolean isMutableMap() default false;

    boolean isMutableSet() default false;

    boolean isMutableList() default false;
}
