package com.lidesheng.hyperlyric.root.source

import android.content.SharedPreferences
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import com.lidesheng.hyperlyric.lyric.source.LyricSink
import com.lidesheng.hyperlyric.root.LyriconDataBridge
import com.lidesheng.hyperlyric.root.island.IslandSlotContentAssembler
import com.lidesheng.hyperlyric.root.island.renderer.IslandRenderer
import com.lidesheng.hyperlyric.root.utils.HookLogger
import com.lidesheng.hyperlyric.lyric.model.interfaces.IRichLyricLine

class RootLyricSink(
    private val renderer: IslandRenderer,
    private val prefs: SharedPreferences? = null
) : LyricSink {

    private val mainHandler = Handler(Looper.getMainLooper())
    private var lastPositionDispatchTimeMs = 0L
    private var pendingPosition: Long? = null
    private var positionDispatchScheduled = false

    private companion object {
        const val MIN_POSITION_DISPATCH_INTERVAL_MS = 33L
    }

    override fun onSongChanged(song: Any?) {
        // 歌曲变更时无需额外处理
    }

    override fun onLyricLine(line: Any?) {
        if (line is IRichLyricLine) {
    
            LyriconDataBridge.updateLyricLine(line)
            renderer.updateLyricLine()
        }
    }

    override fun onPlainText(text: String?) {

        LyriconDataBridge.updateLyric(text)
        renderer.updateLyricLine()
    }

    override fun onStop() {
        pendingPosition = null
        positionDispatchScheduled = false
        renderer.clearAllViews()
        LyriconDataBridge.clearState()
    }

    override fun onMetadata(title: String?, artist: String?, album: String?, publisher: String?) {
        if (title != null) LyriconDataBridge.currentSongName = title
        if (!publisher.isNullOrEmpty()) {
            LyriconDataBridge.updateLyricPackage(publisher)
        }
        IslandSlotContentAssembler.invalidate()
        renderer.refreshActiveIsland()
    }

    override fun onPlaybackStateChanged(isPlaying: Boolean) {
        renderer.onPlaybackStateChanged(isPlaying)
    }

    override fun onPositionChanged(position: Long) {
        val lyricChanged = LyriconDataBridge.updatePosition(position)
        if (lyricChanged) {
            renderer.updateLyricLine()
        }
        dispatchPositionThrottled(position)
    }

    override fun onSeekTo(position: Long) {
        LyriconDataBridge.updatePosition(position)
        renderer.updateLyricLine()
        renderer.updatePosition(position)
    }

    override fun onDisplayTranslationChanged(isDisplayTranslation: Boolean) {
        LyriconDataBridge.isDisplayTranslation = isDisplayTranslation
        renderer.refreshActiveIsland()
    }

    override fun onDisplayRomaChanged(isDisplayRoma: Boolean) {
        LyriconDataBridge.isDisplayRoma = isDisplayRoma
        renderer.refreshActiveIsland()
    }

    private fun dispatchPositionThrottled(position: Long) {
        val now = SystemClock.uptimeMillis()
        val elapsed = now - lastPositionDispatchTimeMs
        if (elapsed >= MIN_POSITION_DISPATCH_INTERVAL_MS) {
            lastPositionDispatchTimeMs = now
            pendingPosition = null
            renderer.updatePosition(position)
            return
        }

        pendingPosition = position
        if (positionDispatchScheduled) return

        positionDispatchScheduled = true
        mainHandler.postDelayed({
            positionDispatchScheduled = false
            val latest = pendingPosition ?: return@postDelayed
            pendingPosition = null
            lastPositionDispatchTimeMs = SystemClock.uptimeMillis()
            renderer.updatePosition(latest)
        }, MIN_POSITION_DISPATCH_INTERVAL_MS - elapsed)
    }

}


