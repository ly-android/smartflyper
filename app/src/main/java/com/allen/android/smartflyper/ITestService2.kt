package com.allen.android.smartflyper

import com.yy.core.room.protocol.BaseEntity
import com.yy.core.yyp.smart.anotation.SmartParam
import com.yy.core.yyp.smart.anotation.SmartUri2

/**
 * Time:2021/7/4 8:34 上午
 * Author:
 * Description:
 */
interface ITestService2 {

    @SmartUri2(max = 101, req = 310, rsp = 311)
    suspend fun getUserInfo(@SmartParam("uid") uid: Long): BaseEntity?

    @SmartUri2(max = 101, req = 312, rsp = 313)
    suspend fun getUserInfo2(@SmartParam("uid") uid: Long): UserEntity?
}