package com.yy.core.yyp.smart.anotation;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.CLASS;

/**
 * Created by liyong on 2017/7/10.
 */
@Documented
@Target(METHOD)
@Retention(CLASS)
public @interface SmartBroadCast {
    int max();

    int min();

    /**
     * svc通道appId
     *
     * @return
     */
    int appId() default 0;

    boolean sync() default true;
}
