/*
 * Copyright 2026 Proify, Tomakino
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */
package com.lidesheng.hyperlyric.lyric.model.extensions

import com.lidesheng.hyperlyric.lyric.model.LyricWord

/**
 * 规范化歌词单词列表。
 * 处理无效的时间戳、合并碎片单词以及填充空隙。
 *
 * 规则说明：
 * - 空文本单词会被丢弃，空白文本会保留为分隔符。
 * - 时间有效的单词必须满足 begin >= 0 且 end > begin。
 * - 时间无效的单词会先缓存，之后按可用时间空隙填充，或合并到相邻有效单词。
 * - duration 通过 end - begin 自动计算。
 * - ASCII 字母/数字片段之间如果没有空白分隔符，会按同一个英文单词合并。
 */
fun List<LyricWord>.normalize(): List<LyricWord> {
    val validTextWords = this.filter { !it.text.isNullOrEmpty() }

    if (validTextWords.isEmpty()) {
        return emptyList()
    }

    val result = ArrayList<LyricWord>()
    val invalidBuffer = ArrayList<LyricWord>()

    var lastEndTime = 0L

    for (word in validTextWords) {
        val isTimeValid = word.begin >= 0 && word.end > word.begin

        if (isTimeValid) {
            if (invalidBuffer.isNotEmpty()) {
                val combinedText = invalidBuffer.joinToString("") { it.text ?: "" }
                val gap = word.begin - lastEndTime

                if (gap > 0) {
                    val filler = LyricWord().apply {
                        this.text = combinedText
                        this.begin = lastEndTime
                        this.end = word.begin
                    }
                    result.add(filler)
                } else {
                    if (result.isNotEmpty()) {
                        val prev = result.last()
                        prev.text = (prev.text ?: "") + combinedText
                    } else {
                        word.text = combinedText + (word.text ?: "")
                    }
                }
                invalidBuffer.clear()
            }

            result.add(word)
            lastEndTime = word.end
        } else {
            invalidBuffer.add(word)
        }
    }

    if (invalidBuffer.isNotEmpty()) {
        val combinedText = invalidBuffer.joinToString("") { it.text ?: "" }

        if (result.isNotEmpty()) {
            val lastWord = result.last()
            lastWord.text = (lastWord.text ?: "") + combinedText
        } else {
            val newWord = LyricWord().apply {
                this.text = combinedText
                this.begin = 0
                this.end = 100
            }
            result.add(newWord)
        }
    }

    return result.normalizeSortByTime().mergeAsciiWordFragments()
}

/**
 * 合并没有空白分隔的 ASCII 字母/数字片段。
 *
 * 部分歌词源会把英文复合词拆成多个有时间戳的片段，例如 under + ground。
 * 这些片段之间没有独立空格词，因此规范化后应作为同一个词显示。
 */
private fun List<LyricWord>.mergeAsciiWordFragments(): List<LyricWord> {
    if (size < 2) return this

    val result = ArrayList<LyricWord>(size)

    for (word in this) {
        val previous = result.lastOrNull()

        if (previous != null && previous.canMergeAsciiWordFragmentWith(word)) {
            previous.text = previous.text.orEmpty() + word.text.orEmpty()
            previous.end = maxOf(previous.end, word.end)
        } else {
            result.add(word)
        }
    }

    return result
}

private fun LyricWord.canMergeAsciiWordFragmentWith(next: LyricWord): Boolean =
    text.isAsciiWordFragment() && next.text.isAsciiWordFragment() && end == next.begin

private fun String?.isAsciiWordFragment(): Boolean =
    !isNullOrEmpty() && all { it.isLetterOrDigit() && it.code < 128 }

