package com.yy.core.yyp.smart.anotation;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.CLASS;

/**
 * Created by liyong on 2019/8/2.
 */
@Documented
@Target(METHOD)
@Retention(CLASS)
public @interface SmartAppender {

    boolean includeUid() default true;

    boolean includePf() default true;

    boolean includeVersion() default true;
}
