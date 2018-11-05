package com.yy.core.room.protocol;

/**
 * Created by liyong on 2017/5/29.
 */

public class BaseEntity {
    public int result;
    public String msg;

    public boolean isSuccess(){
        return result == 0;
    }

    @Override
    public String toString() {
        return "BaseEntity{" +
                "result=" + result +
                ", msg='" + msg + '\'' +
                '}';
    }
}
