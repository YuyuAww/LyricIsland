/*
 * Copyright 2026 Proify, Tomakino
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package com.lidesheng.hyperlyric.lyric.model

import com.lidesheng.hyperlyric.lyric.model.interfaces.ILyricTiming
import kotlinx.serialization.Serializable

/**
 * 歌词时间信息
 *
 * @property begin 开始时间 (毫秒)
 * @property end 结束时间 (毫秒)
 */
@Serializable
data class LyricTiming(
    override var begin: Long,
    override var end: Long
) : ILyricTiming
