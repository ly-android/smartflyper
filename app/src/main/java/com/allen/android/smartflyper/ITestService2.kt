package com.allen.android.smartflyper

import com.yy.core.room.protocol.BaseEntity
import com.yy.core.yyp.smart.anotation.SmartBroadCast2
import com.yy.core.yyp.smart.anotation.SmartParam
import com.yy.core.yyp.smart.anotation.SmartUri2
import kotlinx.coroutines.channels.Channel

/**
 * Time:2021/7/4 8:34 上午
 * Author:
 * Description:
 */
interface ITestService2 {

    @SmartUri2(max = 101, req = 310, rsp = 311, sync = false)
    suspend fun getUserInfo(@SmartParam("uid") uid: List<Long>): BaseEntity?


    @SmartUri2(max = 101, req = 312, rsp = 313)
    suspend fun getUserInfo2(@SmartParam("uid", isMutableList = true) uid: MutableList<Long>): UserEntity?

    @SmartUri2(max = 101, req = 312, rsp = 313)
    suspend fun getUserInfo3(@SmartParam("uid") uid: Long): String?

    //主持协程类型的广播
    @SmartBroadCast2(max = 101, min = 310)
    fun onGetRoomDetailInfoBroadcast(): Channel<BaseEntity>?
}