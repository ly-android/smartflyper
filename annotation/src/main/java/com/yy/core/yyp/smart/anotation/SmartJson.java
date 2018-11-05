package com.yy.core.yyp.smart.anotation;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.RetentionPolicy.CLASS;

/**
 * 标识参数是json String
 * Created by liyong on 2017-07-27.
 */
@Documented
@Target(PARAMETER)
@Retention(CLASS)
public @interface SmartJson {
}
