package com.yy.core.yyp.smart

/**
 * Created by liyong on 2018/10/17.
 */
interface ISmartFlyperFactory {
    fun initApi()
    fun getApi(cls: Class<out Any>): Any
    fun removeApi(cls: Class<out Any>): Boolean
}