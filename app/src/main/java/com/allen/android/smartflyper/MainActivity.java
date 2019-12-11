package com.allen.android.smartflyper;

import android.app.Activity;
import android.os.Bundle;

import com.yy.core.yyp.smart.SmartFlyperFactory$$app;
import com.yy.core.yyp.smart.anotation.SmartAppender;
import com.yy.core.yyp.smart.anotation.SmartUri;

import io.reactivex.Observable;

public class MainActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        new SmartFlyperFactory$$app().getApi(InnerService.class);
    }

    public interface InnerService {
        @SmartAppender
        @SmartUri(max = 101, req = 312, rsp = 313, appId = 60015)
        Observable<String> getRoomInfo();
    }
}
