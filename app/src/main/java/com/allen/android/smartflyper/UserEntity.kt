package com.allen.android.smartflyper

import com.yy.core.room.protocol.BaseEntity

/**
 * Time:2021/7/4 8:41 上午
 * Author:
 * Description:
 */
class UserEntity : BaseEntity() {
    var name: String? = null
    var uid: Long = 0
    var iconUrl: String? = ""
}