/*
 * Copyright 2026 Proify, Tomakino
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */
package com.lidesheng.hyperlyric.lyric.model.interfaces

/**
 * 歌词时间
 *
 * @property begin 开始时间 (毫秒)
 * @property end 结束时间 (毫秒)
 */
interface ILyricTiming {
    var begin: Long
    var end: Long

    val duration: Long
        get() = end - begin
}
