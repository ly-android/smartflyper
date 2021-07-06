package com.yy.core.yyp.smart

/**
 * Created by liyong on 2018/10/16.
 */
interface ISmartFlyper2 {
    suspend fun <T> sendCoroutines(method: WrapperMethod?): T?
}