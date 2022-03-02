package com.yy.core.yyp.smart

import kotlinx.coroutines.channels.Channel

/**
 * Created by liyong on 2018/10/16.
 */
interface ISmartFlyper2 {
    suspend fun <T> sendCoroutines(method: WrapperMethod): T?
    fun <T> registerCoroutinesBroadcast(method: WrapperMethod): Channel<T>
}