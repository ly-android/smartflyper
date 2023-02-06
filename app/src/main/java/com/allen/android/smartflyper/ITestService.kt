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
        @SmartParam("uid", isMutableList = true) uids: MutableList<Long>,
        @SmartParam("version", isMutableMap = true) version: MutableMap<String, String>?,
        @SmartParam("pf") pf: Map<String, String>
    ): Observable<String>?

    @SmartUri(max = 101, req = 308, rsp = 309)
    fun getRoomDetailInfo(
        @SmartParam("channelId") channelIds: List<Long>,
        @SmartParam("uid") uid: HashSet<Long>,
        @SmartParam("version", isMutableSet = true) version: MutableSet<String>
    ): Observable<String>

    @SmartBroadCast(max = 101, min = 310, sync = false)
    fun onGetRoomDetailInfoBroadcast(observerResult: SmartObserverResult<BaseEntity>): String?
}