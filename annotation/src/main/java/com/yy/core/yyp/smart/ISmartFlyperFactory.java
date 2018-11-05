package com.yy.core.yyp.smart;

/**
 * Created by liyong on 2018/10/17.
 */
public interface ISmartFlyperFactory {
    void initApi();

    Object getApi(Class cls);

    boolean removeApi(Class cls);
}
