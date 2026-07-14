/*
 * Copyright 2026 Proify, Tomakino
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package com.lidesheng.hyperlyric.lyric.model

import com.lidesheng.hyperlyric.lyric.model.interfaces.DeepCopyable
import com.lidesheng.hyperlyric.lyric.model.interfaces.ILyricWord
import kotlinx.serialization.Serializable

/**
 * 歌词单词
 *
 * @property begin 开始时间 (毫秒)
 * @property end 结束时间 (毫秒)
 * @property text 文本
 * @property metadata 元数据
 */
@Serializable
data class LyricWord(
    override var begin: Long = 0,
    override var end: Long = 0,
    override var text: String? = null,
    override var metadata: LyricMetadata? = null,
) : ILyricWord, DeepCopyable<LyricWord> {

    override fun deepCopy(): LyricWord = copy()
}
