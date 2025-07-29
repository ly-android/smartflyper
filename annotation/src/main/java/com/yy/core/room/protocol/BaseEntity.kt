package com.yy.core.room.protocol

/**
 * Created by liyong on 2017/5/29.
 */
open class BaseEntity {
    @JvmField
    var result: Int = 0

    @JvmField
    var msg: String? = ""

    open fun isSuccess(): Boolean {
        return result == 0
    }

    override fun toString(): String {
        return "BaseEntity{" +
            "result=" + result +
            ", msg='" + msg + '\'' +
            '}'
    }
}
