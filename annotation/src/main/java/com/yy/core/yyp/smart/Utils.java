package com.yy.core.yyp.smart;

import com.yy.core.yyp.smart.anotation.SmartJson;
import com.yy.core.yyp.smart.anotation.SmartMap;
import com.yy.core.yyp.smart.anotation.SmartParam;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

/**
 * Created by liyong on 2017/7/7.
 */

public class Utils {

    /**
     * 获取指定方法的参数名
     *
     * @param method 要获取参数名的方法
     * @return 按参数顺序排列的参数名列表
     */
    static ParamEntity[] getMethodParameterNamesByAnnotation(Method method) {
        Annotation[][] parameterAnnotations = method.getParameterAnnotations();
        if (parameterAnnotations == null || parameterAnnotations.length == 0) {
            return null;
        }
        ParamEntity[] paramEntities = new ParamEntity[parameterAnnotations.length];
        int i = 0;
        for (Annotation[] parameterAnnotation : parameterAnnotations) {
            for (Annotation annotation : parameterAnnotation) {
                if (annotation instanceof SmartParam) {
                    SmartParam param = (SmartParam) annotation;
                    paramEntities[i++] =new ParamEntity(annotation,param.value());
                }else if (annotation instanceof SmartMap) {
                    paramEntities[i++] =new ParamEntity(annotation,"");
                }else if (annotation instanceof SmartJson) {
                    paramEntities[i++] =new ParamEntity(annotation,"");
                }
            }
        }
        return paramEntities;
    }

    static Type[] getTurnTypeGenericTypes(Method method){
        Type type=method.getGenericReturnType();
        if(type instanceof ParameterizedType){
            Type[] pt=((ParameterizedType)type).getActualTypeArguments();
            return pt;
        }
        return null;
    }

    static Class getParamGenericTypes(Method method){
        Type[] types=method.getGenericParameterTypes();
        if(types==null){
            throw new IllegalArgumentException("参数必须是SmartObservelResult<T>,T is String or BaseEntity类型");
        }
        Type type=types[0];
        if(type instanceof ParameterizedType){
            Type[] pt=((ParameterizedType)type).getActualTypeArguments();
            return (Class) pt[0];
        }
        return null;
    }

    static  <T> void validateServiceInterface(Class<T> service) {
        if (!service.isInterface()) {
            throw new IllegalArgumentException("API declarations must be interfaces.");
        }
        // Prevent API interfaces from extending other interfaces. This not only avoids a bug in
        // Android (http://b.android.com/58753) but it forces composition of API declarations which is
        // the recommended pattern.
        if (service.getInterfaces().length > 0) {
            throw new IllegalArgumentException("API interfaces must not extend other interfaces.");
        }
    }
}
