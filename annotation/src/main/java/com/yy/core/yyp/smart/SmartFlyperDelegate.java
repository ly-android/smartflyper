package com.yy.core.yyp.smart;

/**
 * Created by liyong on 2018/10/16.
 */
public class SmartFlyperDelegate {

    public static void setSmartFlyper(ISmartFlyper smartFlyper) {
        SmartFlyperDelegate.smartFlyper = smartFlyper;
    }

    private static ISmartFlyper smartFlyper;


    public static <T> T send(WrapperMethod method) {
        return smartFlyper.send(method);
    }

}
