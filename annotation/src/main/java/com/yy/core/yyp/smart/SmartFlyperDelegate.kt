package com.yy.core.yyp.smart

import kotlinx.coroutines.channels.Channel

/**
 * Created by liyong on 2018/10/16.
 */
object SmartFlyperDelegate {
    fun setSmartFlyper(smartFlyper: ISmartFlyper?) {
        SmartFlyperDelegate.smartFlyper = smartFlyper
    }

    fun setSmartFlyper2(smartFlyper2: ISmartFlyper2?) {
        SmartFlyperDelegate.smartFlyper2 = smartFlyper2
    }

    private var smartFlyper: ISmartFlyper? = null
    private var smartFlyper2: ISmartFlyper2? = null

    @JvmStatic
    fun <T> send(method: WrapperMethod?): T {
        return smartFlyper!!.send(method)
    }

    @JvmStatic
    suspend fun <T> sendCoroutines(method: WrapperMethod): T? {
        return smartFlyper2?.sendCoroutines(method)
    }

    @JvmStatic
    fun <T> registerCoroutinesBroadcast(method: WrapperMethod): Channel<T>? {
        return smartFlyper2?.registerCoroutinesBroadcast(method)
    }
}