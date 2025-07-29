package com.yy.core.room.protocol;

import org.jetbrains.annotations.Nullable;

/**
 * Created by liyong on 2017/5/29.
 */

public class BaseEntity {
    public int result = 0;
    @Nullable
    public String msg = "";

    public boolean isSuccess() {
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
