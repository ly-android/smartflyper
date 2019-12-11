package com.yy.core.yyp.smart;

import com.yy.core.room.protocol.BaseEntity;
import com.yy.core.yyp.smart.anotation.SmartAppender;
import com.yy.core.yyp.smart.anotation.SmartBroadCast;
import com.yy.core.yyp.smart.anotation.SmartUri;

import java.lang.annotation.Annotation;
import java.lang.annotation.AnnotationFormatError;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.Arrays;

/**
 * Created by liyong on 2017/7/7.
 */

public class WrapperMethod {

    public int appId = 0; //svc appId
    public int max = 0;//大类
    public int min_req = 0;//req小类
    public int min_rsp = 0;//rsp小类
    public ParamEntity[] params;//参数名称列表
    public Class returnTypeParams;
    public boolean isSmartBroadcast;//是否为广播类型
    public SmartObserverResult smartObserverResult;//广播类型需要回传执行结果
    public Class paramsTypes;//广播类型保存SmartObserverResult的参数类型
    public Object[] args; //参数列表

    public boolean includeUid = false;
    public boolean includePf = false;
    public boolean includeVersion = false;

    public WrapperMethod(Builder builder) {
        this.appId = builder.appId;
        this.max = builder.max;
        this.min_req = builder.min_req;
        this.min_rsp = builder.min_rsp;
        this.params = builder.params;
        if (builder.returnTypeParams != null && builder.returnTypeParams.length > 0) {
            this.returnTypeParams = (Class) builder.returnTypeParams[0];
        }
        this.isSmartBroadcast = builder.isSmartBroadcast;
        this.paramsTypes = builder.paramsTypes;
        this.args = builder.args;
    }

    public WrapperMethod() {

    }

    static final class Builder {
        Method method;
        int appId;
        int max;
        int min_req;
        int min_rsp;
        ParamEntity[] params;
        Type[] returnTypeParams;//方法返回类型的参数类型
        boolean isSmartBroadcast;
        Class paramsTypes;
        Object[] args; //参数列表
        public boolean includeUid = false;
        public boolean includePf = false;
        public boolean includeVersion = false;

        public Builder(Method method) {
            this.method = method;
        }

        public WrapperMethod build() {
            for (Annotation annotation : method.getAnnotations()) {
                parseMethodAnnotation(annotation);
            }
            if (isSmartBroadcast) {
                paramsTypes = Utils.getParamGenericTypes(method);
                if (paramsTypes != null && !paramsTypes.equals(String.class) && !(BaseEntity.class.isAssignableFrom(paramsTypes))) {
                    throw new IllegalArgumentException("参数必须是SmartObservelResult<T>,T is String or BaseEntity类型");
                }
                if (!String.class.equals(method.getReturnType())) {
                    throw new IllegalArgumentException("must be return String!");
                }
            } else {
                returnTypeParams = Utils.getTurnTypeGenericTypes(method);
                if (returnTypeParams == null) {
                    throw new IllegalArgumentException("方法返回类型必须是Observable<T>,T is String or BaseEntity");
                }
                Class cls = (Class) returnTypeParams[0];
                if (!cls.equals(String.class) && !(BaseEntity.class.isAssignableFrom(cls))) {
                    throw new IllegalArgumentException("方法返回类型必须是Observable<T>,T is String or BaseEntity");
                }
            }
            params = Utils.getMethodParameterNamesByAnnotation(method);
            return new WrapperMethod(this);
        }

        private void parseMethodAnnotation(Annotation annotation) {
            if (annotation instanceof SmartUri) {
                SmartUri smartUri = (SmartUri) annotation;
                max = smartUri.max();
                min_req = smartUri.req();
                min_rsp = smartUri.rsp();
                appId = smartUri.appId();
            } else if (annotation instanceof SmartBroadCast) {
                SmartBroadCast smartBroadCast = (SmartBroadCast) annotation;
                max = smartBroadCast.max();
                min_rsp = smartBroadCast.min();
                isSmartBroadcast = true;
                appId = smartBroadCast.appId();
            } else if (annotation instanceof SmartAppender) {
                SmartAppender appender = (SmartAppender) annotation;
                includePf = appender.includePf();
                includeUid = appender.includeUid();
                includeVersion = appender.includeVersion();
            } else {
                throw new AnnotationFormatError("没有加入SmartUri or SmartBroadCast注解");
            }
        }
    }

    @Override
    public String toString() {
        return "WrapperMethod{" +
                "appId=" + appId +
                "max=" + max +
                ", min_req=" + min_req +
                ", min_rsp=" + min_rsp +
                ", params=" + Arrays.toString(params) +
                ", returnTypeParams=" + returnTypeParams +
                ", isSmartBroadcast=" + isSmartBroadcast +
                ", smartObserverResult=" + smartObserverResult +
                ", paramsTypes=" + paramsTypes +
                '}';
    }
}
