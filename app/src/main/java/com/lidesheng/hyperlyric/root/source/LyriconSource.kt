package com.lidesheng.hyperlyric.root.source

import android.app.Application
import android.content.pm.PackageManager
import com.lidesheng.hyperlyric.lyric.source.LyricSink
import com.lidesheng.hyperlyric.lyric.source.LyricSource
import com.lidesheng.hyperlyric.root.LyriconDataBridge
import com.lidesheng.hyperlyric.root.utils.HookLogger
import io.github.proify.lyricon.lyric.model.Song
import io.github.proify.lyricon.subscriber.ActivePlayerListener
import io.github.proify.lyricon.subscriber.ConnectionListener
import io.github.proify.lyricon.subscriber.LyriconFactory
import io.github.proify.lyricon.subscriber.LyriconSubscriber
import io.github.proify.lyricon.subscriber.ProviderInfo
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString

class LyriconSource : LyricSource {

    companion object {
        private const val TAG = "LyriconSource"
        private const val LYRICON_CORE_PACKAGE = "io.github.proify.lyricon.core"
        private val json = Json { ignoreUnknownKeys = true; coerceInputValues = true }
    }

    private fun Song.toLocalSong(): com.lidesheng.hyperlyric.lyric.model.Song {
        val jsonString = json.encodeToString(this)
        return json.decodeFromString(jsonString)
    }

    override val id = "lyricon"
    override val displayName = "Lyricon"

    @Volatile
    private var sink: LyricSink? = null
    private var app: Application? = null
    @Volatile
    private var subscriber: LyriconSubscriber? = null

    /**
     * 检查 Lyricon 核心服务是否可用
     * 官方标准：Subscriber 需要安装 Lyricon 核心服务
     */
    override fun isAvailable(): Boolean {
        val application = app ?: return false
        return try {
            application.packageManager.getPackageInfo(LYRICON_CORE_PACKAGE, 0)
            true
        } catch (e: PackageManager.NameNotFoundException) {
            HookLogger.w(TAG, "Lyricon 核心服务未安装: $LYRICON_CORE_PACKAGE")
            false
        }
    }

    override fun start(sink: LyricSink) {
        if (this.subscriber != null) {
            HookLogger.w(TAG, "Lyricon 数据源已在运行，跳过重复启动")
            return
        }
        this.sink = sink
        val application = app ?: run {
            HookLogger.w(TAG, "Application 未初始化，无法启动")
            return
        }
        initializeSubscriber(application)
        HookLogger.i(TAG, "Lyricon 数据源已启动")
    }

    override fun stop() {
        try {
            subscriber?.unsubscribeActivePlayer(activePlayerListener)
            subscriber?.removeConnectionListener(connectionListener)
            subscriber?.unregister()
            subscriber?.destroy()
        } catch (e: Exception) {
            HookLogger.e(TAG, "清理 Subscriber 时发生错误", e)
        } finally {
            subscriber = null
            sink?.onStop()
            sink = null
        }
        HookLogger.i(TAG, "Lyricon 数据源已停止")
    }

    fun initialize(app: Application) {
        this.app = app
    }

    private fun initializeSubscriber(app: Application) {
        val sub = LyriconFactory.createSubscriber(app)
        subscriber = sub

        sub.addConnectionListener(connectionListener)
        sub.subscribeActivePlayer(activePlayerListener)
        sub.register()
    }

    private val connectionListener = object : ConnectionListener {
        override fun onConnected(subscriber: LyriconSubscriber) {
            HookLogger.i(TAG, "Subscriber 已连接")
        }

        override fun onReconnected(subscriber: LyriconSubscriber) {
            HookLogger.i(TAG, "Subscriber 已重连")
        }

        override fun onDisconnected(subscriber: LyriconSubscriber) {
            HookLogger.w(TAG, "Subscriber 已断开")
        }

        override fun onConnectTimeout(subscriber: LyriconSubscriber) {
            HookLogger.w(TAG, "Subscriber 连接超时")
        }
    }

    /**
     * 活跃播放器监听器
     * 实现 ActivePlayerListener 接口处理所有歌词事件
     */
    private val activePlayerListener = object : ActivePlayerListener {
        
        /**
         * 官方标准：providerInfo == null 表示当前没有活跃播放器，应清理当前 UI 状态
         * 官方标准：播放器切换时，应将播放进度重置为新 Provider 回调中的值
         */
        override fun onActiveProviderChanged(providerInfo: ProviderInfo?) {
            if (providerInfo == null) {
                // 无活跃播放器，清理 UI 状态并进入等待状态
                HookLogger.i(TAG, "无活跃播放器，清理状态")
                LyriconDataBridge.clearState()
            } else {
                // 播放器切换，通知 Sink 停止旧播放器
                HookLogger.i(TAG, "活跃播放器变更: ${providerInfo.playerPackageName}")
            }
            sink?.onStop()
            LyriconDataBridge.updateLyricPackage(providerInfo?.playerPackageName)
        }

        /**
         * 官方标准：song == null 表示当前歌曲已清空
         */
        override fun onSongChanged(song: Song?) {
            val localSong = song?.toLocalSong()
            LyriconDataBridge.updateSong(localSong)
            sink?.onSongChanged(localSong)
        }

        /**
         * 官方标准：播放状态变化时触发
         */
        override fun onPlaybackStateChanged(isPlaying: Boolean) {
            sink?.onPlaybackStateChanged(isPlaying)
        }

        /**
         * 官方标准：播放进度更新，单位毫秒，用于驱动歌词滚动或逐字进度
         */
        override fun onPositionChanged(position: Long) {
            sink?.onPositionChanged(position)
        }

        /**
         * 官方标准：主动跳转进度，应立即校准歌词位置
         */
        override fun onSeekTo(position: Long) {
            sink?.onSeekTo(position)
        }

        /**
         * 官方标准：收到纯文本歌词时触发，应视为进入纯文本模式
         */
        override fun onReceiveText(text: String?) {
            sink?.onPlainText(text)
        }

        /**
         * 官方标准：翻译显示开关变化，不保证当前歌词一定包含翻译内容
         */
        override fun onDisplayTranslationChanged(isDisplayTranslation: Boolean) {
            sink?.onDisplayTranslationChanged(isDisplayTranslation)
        }

        /**
         * 官方标准：罗马音显示开关变化，不保证当前歌词一定包含罗马音内容
         */
        override fun onDisplayRomaChanged(isDisplayRoma: Boolean) {
            sink?.onDisplayRomaChanged(isDisplayRoma)
        }
    }
}
