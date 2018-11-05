package com.allen.android.smartflyper;


import com.yy.core.room.protocol.BaseEntity;
import com.yy.core.yyp.smart.SmartObserverResult;
import com.yy.core.yyp.smart.anotation.SmartBroadCast;
import com.yy.core.yyp.smart.anotation.SmartParam;
import com.yy.core.yyp.smart.anotation.SmartUri;

import io.reactivex.Observable;

/**
 * Created by liyong on 2018/11/5.
 */
public interface ITestService {

  @SmartUri(max = 101, req = 312, rsp = 313)
  Observable<String> getMyRoomInfo(@SmartParam("uid") long uid,
                                   @SmartParam("version") String version, @SmartParam("pf") int pf);


  @SmartUri(max = 101, req = 308, rsp = 309)
  Observable<String> getRoomDetailInfo(@SmartParam("channelId") long channelId, @SmartParam("uid") long uid,
                                       @SmartParam("version") String version);

  @SmartBroadCast(max = 101, min = 310)
  String onGetRoomDetailInfoBroadcast(SmartObserverResult<BaseEntity> observerResult);
}
