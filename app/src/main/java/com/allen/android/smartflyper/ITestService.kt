package com.allen.android.smartflyper

import com.yy.core.room.protocol.BaseEntity
import com.yy.core.yyp.smart.SmartObserverResult
import com.yy.core.yyp.smart.anotation.SmartBroadCast
import com.yy.core.yyp.smart.anotation.SmartParam
import com.yy.core.yyp.smart.anotation.SmartUri
import io.reactivex.Observable

/**
 * Created by liyong on 2018/11/5.
 */
interface ITestService {
    @SmartUri(max = 101, req = 312, rsp = 313)
    fun getMyRoomInfo(
        @SmartParam("uid") uid: Long,
        @SmartParam("version") version: String?, @SmartParam("pf") pf: Int
    ): Observable<String>?

    @SmartUri(max = 101, req = 308, rsp = 309)
    fun getRoomDetailInfo(
        @SmartParam("channelId") channelId: Long, @SmartParam("uid") uid: Long,
        @SmartParam("version") version: String
    ): Observable<String>

    @SmartBroadCast(max = 101, min = 310)
    fun onGetRoomDetailInfoBroadcast(observerResult: SmartObserverResult<BaseEntity>): String?
}