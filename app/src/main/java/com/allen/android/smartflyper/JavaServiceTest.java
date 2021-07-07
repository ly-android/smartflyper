package com.allen.android.smartflyper;

import android.util.Log;

import com.yy.core.yyp.smart.anotation.SmartParam;
import com.yy.core.yyp.smart.anotation.SmartUri;

import io.reactivex.Observable;

/**
 * Time:2021/7/7 9:47 上午
 * Author:
 * Description:
 */
class JavaServiceTest {

    public static final String TAG = "JavaServiceTest";

    public void doWork() {
        Log.d(TAG, "doWork: >>>>>>>>>");
    }

    interface IJavaService {
        @SmartUri(max = 101, req = 312, rsp = 313, appId = 60015)
        Observable<String> roomInfo(@SmartParam("sid") long sid);
    }
}
