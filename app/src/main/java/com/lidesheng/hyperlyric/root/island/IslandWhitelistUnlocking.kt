package com.lidesheng.hyperlyric.root.island

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import com.lidesheng.hyperlyric.common.RootConstants
import com.lidesheng.hyperlyric.root.HookEntry
import com.lidesheng.hyperlyric.root.utils.HookLogger
import io.github.libxposed.api.XposedInterface.Chain
import io.github.libxposed.api.XposedInterface.HookHandle
import io.github.libxposed.api.XposedInterface.Hooker
import io.github.libxposed.api.XposedModule
import java.lang.reflect.Method

object IslandWhitelistUnlocking {
    private const val TARGET_CLASS = "miui.systemui.notification.NotificationSettingsManager"
    private const val AUTH_CALLBACK_CLASS =
        $$"miui.systemui.notification.auth.AuthManager$AuthServiceCallback$onAuthResult$1"
    private const val PLUGIN_INSTANCE_CLASS = "com.android.systemui.shared.plugins.PluginInstance"
    private const val ISLAND_METHOD = "mediaIslandSupportMiniWindow"

    internal lateinit var module: XposedModule
    private val hookedClassLoaders = java.util.Collections.newSetFromMap(java.util.WeakHashMap<ClassLoader, Boolean>())
    private val focusWhitelistHandles = mutableListOf<HookHandle>()
    private val islandWhitelistHandles = mutableMapOf<Method, HookHandle>()
    private val knownClassLoaders = mutableSetOf<ClassLoader>()
    private var focusPrefsListener: SharedPreferences.OnSharedPreferenceChangeListener? = null
    private var islandPrefsListener: SharedPreferences.OnSharedPreferenceChangeListener? = null

    fun hook(xposedModule: XposedModule, defaultClassLoader: ClassLoader) {
        module = xposedModule
        val prefs = (module as HookEntry).prefs

        setupFocusWhitelistListener(prefs)
        setupIslandWhitelistListener(prefs)

        runCatching {
            val pluginInstanceClass = defaultClassLoader.loadClass(PLUGIN_INSTANCE_CLASS)
            val method = pluginInstanceClass.declaredMethods.find { it.name == "loadPlugin" }
            if (method != null) {
                module.deoptimize(method)
                module.hook(method).intercept(PluginLoadHooker())
                HookLogger.i("IslandWhitelistUnlocking", "插件拦截器已就绪 (PluginInstance)")
            } else {
                HookLogger.w("IslandWhitelistUnlocking", "未找到 PluginInstance.loadPlugin")
            }
        }.onFailure { e ->
            if (e is ClassNotFoundException) {
                HookLogger.w("IslandWhitelistUnlocking", "$PLUGIN_INSTANCE_CLASS 未找到")
            } else {
                HookLogger.e("IslandWhitelistUnlocking", "拦截 PluginInstance 时发生错误", e)
            }
        }

        if (prefs.getBoolean(RootConstants.KEY_HOOK_REMOVE_FOCUS_WHITELIST, RootConstants.DEFAULT_HOOK_REMOVE_FOCUS_WHITELIST)) {
            doHookFocusWhitelistInClassLoader(defaultClassLoader)
        } else {
            hookedClassLoaders.add(defaultClassLoader)
            knownClassLoaders.add(defaultClassLoader)
        }

        if (prefs.getBoolean(RootConstants.KEY_HOOK_REMOVE_ISLAND_WHITELIST, RootConstants.DEFAULT_HOOK_REMOVE_ISLAND_WHITELIST)) {
            doHookIslandWhitelistInClassLoader(defaultClassLoader)
        } else {
            knownClassLoaders.add(defaultClassLoader)
        }
    }

    private fun setupFocusWhitelistListener(prefs: SharedPreferences) {
        val prefKey = RootConstants.KEY_HOOK_REMOVE_FOCUS_WHITELIST
        focusPrefsListener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            if (key == prefKey) {
                val enabled = prefs.getBoolean(prefKey, RootConstants.DEFAULT_HOOK_REMOVE_FOCUS_WHITELIST)
                if (enabled) {
                    hookAllKnownClassLoadersForFocus()
                } else {
                    unhookFocusWhitelist()
                }
            }
        }
        prefs.registerOnSharedPreferenceChangeListener(focusPrefsListener)
    }

    private fun setupIslandWhitelistListener(prefs: SharedPreferences) {
        val prefKey = RootConstants.KEY_HOOK_REMOVE_ISLAND_WHITELIST
        islandPrefsListener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            if (key == prefKey) {
                val enabled = prefs.getBoolean(prefKey, RootConstants.DEFAULT_HOOK_REMOVE_ISLAND_WHITELIST)
                if (enabled) {
                    hookAllKnownClassLoadersForIsland()
                } else {
                    unhookIslandWhitelist()
                }
            }
        }
        prefs.registerOnSharedPreferenceChangeListener(islandPrefsListener)
    }

    fun doHookFocusWhitelistInClassLoader(cl: ClassLoader?) {
        if (cl == null || !hookedClassLoaders.add(cl)) return
        knownClassLoaders.add(cl)

        val prefs = (module as? HookEntry)?.prefs ?: return
        val enabled = prefs.getBoolean(RootConstants.KEY_HOOK_REMOVE_FOCUS_WHITELIST, RootConstants.DEFAULT_HOOK_REMOVE_FOCUS_WHITELIST)
        if (!enabled) return

        installFocusWhitelistHooks(cl)
    }

    fun doHookIslandWhitelistInClassLoader(cl: ClassLoader?) {
        if (cl == null) return
        knownClassLoaders.add(cl)

        val prefs = (module as? HookEntry)?.prefs ?: return
        val enabled = prefs.getBoolean(RootConstants.KEY_HOOK_REMOVE_ISLAND_WHITELIST, RootConstants.DEFAULT_HOOK_REMOVE_ISLAND_WHITELIST)
        if (!enabled) return

        installIslandWhitelistHook(cl)
    }

    private fun installFocusWhitelistHooks(cl: ClassLoader) {
        runCatching {
            val targetClass = cl.loadClass(TARGET_CLASS)
            val methods = targetClass.declaredMethods.filter {
                it.name == "canShowFocus" || it.name == "canCustomFocus"
            }

            if (methods.isNotEmpty()) {
                methods.forEach { method ->
                    module.deoptimize(method)
                    val handle = module.hook(method).intercept(ReturnTrueHooker())
                    focusWhitelistHandles.add(handle)
                }
                HookLogger.i("IslandWhitelistUnlocking", "焦点通知白名单: hook (${methods.joinToString { it.name }})")
            }
        }.onFailure { e ->
            if (e !is ClassNotFoundException) {
                HookLogger.e("IslandWhitelistUnlocking", "焦点通知白名单注入失败 ($cl)", e)
            }
        }

        runCatching {
            val authClass = cl.loadClass(AUTH_CALLBACK_CLASS)
            val method = authClass.declaredMethods.find { it.name == "invokeSuspend" }

            if (method != null) {
                module.deoptimize(method)
                val handle = module.hook(method).intercept(AuthResultHooker())
                focusWhitelistHandles.add(handle)
                HookLogger.i("IslandWhitelistUnlocking", "焦点通知白名单: hook (authCallback)")
            }
        }.onFailure { e ->
            if (e !is ClassNotFoundException) {
                HookLogger.e("IslandWhitelistUnlocking", "焦点通知白名单授权注入失败 ($cl)", e)
            }
        }
    }

    private fun installIslandWhitelistHook(cl: ClassLoader) {
        runCatching {
            val targetClass = cl.loadClass(TARGET_CLASS)
            val method = targetClass.declaredMethods.find { it.name == ISLAND_METHOD }

            if (method != null && !islandWhitelistHandles.containsKey(method)) {
                module.deoptimize(method)
                val handle = module.hook(method).intercept(ReturnTrueHooker())
                islandWhitelistHandles[method] = handle
                HookLogger.i("IslandWhitelistUnlocking", "媒体超级岛下拉小窗白名单: hook ($ISLAND_METHOD)")
            }
        }.onFailure { e ->
            if (e !is ClassNotFoundException) {
                HookLogger.e("IslandWhitelistUnlocking", "媒体超级岛下拉小窗白名单注入失败", e)
            }
        }
    }

    private fun hookAllKnownClassLoadersForFocus() {
        knownClassLoaders.toList().forEach { cl ->
            installFocusWhitelistHooks(cl)
        }
    }

    private fun hookAllKnownClassLoadersForIsland() {
        knownClassLoaders.toList().forEach { cl ->
            installIslandWhitelistHook(cl)
        }
    }

    private fun unhookFocusWhitelist() {
        focusWhitelistHandles.forEach { it.unhook() }
        focusWhitelistHandles.clear()
        HookLogger.i("IslandWhitelistUnlocking", "焦点通知白名单: unhook")
    }

    private fun unhookIslandWhitelist() {
        islandWhitelistHandles.values.forEach { it.unhook() }
        islandWhitelistHandles.clear()
        HookLogger.i("IslandWhitelistUnlocking", "媒体超级岛下拉小窗白名单: unhook")
    }

    class PluginLoadHooker : Hooker {
        override fun intercept(chain: Chain): Any? {
            val result = chain.proceed()
            runCatching {
                val thisObj = chain.thisObject ?: return result
                thisObj.javaClass.declaredFields.forEach { f ->
                    if (f.name == "mPluginContext" || f.name == "mContext") {
                        f.isAccessible = true
                        (f.get(thisObj) as? Context)?.let { context ->
                            doHookFocusWhitelistInClassLoader(context.classLoader)
                            doHookIslandWhitelistInClassLoader(context.classLoader)
                        }
                    }
                }
            }
            return result
        }
    }

    class ReturnTrueHooker : Hooker {
        override fun intercept(chain: Chain): Any? {
            return true
        }
    }

    class AuthResultHooker : Hooker {
        override fun intercept(chain: Chain): Any? {
            runCatching {
                val thisObj = chain.thisObject
                thisObj.javaClass.declaredFields.forEach { f ->
                    if (f.name.contains("authBundle")) {
                        f.isAccessible = true
                        (f.get(thisObj) as? Bundle)?.putInt("result_code", 0)
                    }
                }
            }
            return chain.proceed()
        }
    }
}