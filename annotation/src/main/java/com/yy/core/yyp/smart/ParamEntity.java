package com.yy.core.yyp.smart;

import java.lang.annotation.Annotation;

/**
 * Created by liyong on 2017-07-26.
 */

public class ParamEntity {
    public static final int SMARTPARAM = 0;
    public static final int SMARTMAP = 1;
    public static final int SMARTJSON = 2;
    public Annotation annotation;
    public String name;
    public int type;//参数类型

    public ParamEntity() {
    }

    public ParamEntity(Annotation annotation, String name) {
        this.annotation = annotation;
        this.name = name;
    }

    public ParamEntity(int type, String name) {
        this.name = name;
        this.type = type;
    }
}
