# HyperLyric Code Wiki

## 目录

- [1. 项目概述](#1-项目概述)
- [2. 项目架构](#2-项目架构)
- [3. 核心模块详解](#3-核心模块详解)
  - [3.1 common 公共模块](#31-common-公共模块)
  - [3.2 lyric 歌词模块](#32-lyric-歌词模块)
  - [3.3 root Hook 模块](#33-root-hook-模块)
  - [3.4 ui 界面模块](#34-ui-界面模块)
- [4. 核心功能详解](#4-核心功能详解)
  - [4.1 歌词源管理](#41-歌词源管理)
  - [4.2 超级岛歌词注入](#42-超级岛歌词注入)
- [5. 关键数据结构](#5-关键数据结构)
- [6. 依赖关系](#6-依赖关系)
- [7. 项目构建与运行](#7-项目构建与运行)

---

## 1. 项目概述

**项目名称**: HyperLyric (音乐岛 / LyricIsland)

**项目类型**: Android 歌词显示应用

**核心功能**:
- 通过 Xposed Hook 方式在 MIUI 超级岛（灵动岛）中显示歌词
- 使用 Lyricon 歌词源获取实时歌词
- 丰富的样式自定义选项

**技术栈**:
- 语言: Kotlin
- UI 框架: Jetpack Compose + MIUI X 组件库
- Hook 框架: LibXposed
- 序列化: Kotlinx Serialization
- 协程: Kotlin Coroutines

---

## 2. 项目架构

### 2.1 整体架构图

```
┌─────────────────────────────────────────────────────────────┐
│                        UI 层 (ui)                           │
│  MainActivity / AppNavigation / Pages / Components         │
└────────────────────────────┬────────────────────────────────┘
                             │
┌────────────────────────────▼────────────────────────────────┐
│                      歌词核心层 (lyric)                      │
│  数据模型 / SourceManager / DynamicData                     │
└────────────────────────────┬────────────────────────────────┘
                             │
          ┌──────────────────┴──────────────────┐
          │                                     │
┌─────────▼─────────┐               ┌───────────▼─────────────┐
│  Root Hook 层     │               │   公共层 (common)        │
│  (Xposed 模块)    │               │   工具类/常量/解析器     │
│  HookEntry        │               └─────────────────────────┘
│  IslandHooker     │
│  LyriconSource    │
└───────────────────┘
```

### 2.2 包结构说明

```
com.lidesheng.hyperlyric
├── common/                    # 公共工具与常量
│   ├── color/                 # 颜色提取
│   ├── extensions/            # Kotlin 扩展函数
│   ├── image/                 # 图片处理
│   ├── lyric/                 # 歌词解析工具
│   └── media/                 # 媒体元数据
├── lyric/                     # 歌词核心模块
│   ├── model/                 # 数据模型
│   ├── source/                # 歌词源接口与管理
│   ├── style/                 # 样式配置
│   └── view/                  # 歌词视图相关
├── root/                      # Xposed Hook 模块
│   ├── bridge/                # IPC 桥接
│   ├── island/                # 超级岛注入
│   ├── source/                # Root 进程歌词源（LyriconSource）
│   └── utils/                 # Hook 工具
├── ui/                        # 用户界面
│   ├── component/             # Compose 组件
│   ├── navigation/            # 导航
│   ├── page/                  # 页面
│   └── utils/                 # UI 工具
└── utils/                     # 应用级工具
```

---

## 3. 核心模块详解

### 3.1 common 公共模块

**路径**: [app/src/main/java/com/lidesheng/hyperlyric/common](file:///workspace/app/src/main/java/com/lidesheng/hyperlyric/common)

#### 3.1.1 核心常量类

| 类名 | 职责 |
|------|------|
| [RootConstants.kt](file:///workspace/app/src/main/java/com/lidesheng/hyperlyric/common/RootConstants.kt) | Root Hook 相关的配置键名与默认值，包括超级岛、样式、动画、翻译、白名单解锁等配置 |
| [PreferenceKeys.kt](file:///workspace/app/src/main/java/com/lidesheng/hyperlyric/common/PreferenceKeys.kt) | SharedPreferences 名称与日志级别配置 |
| [UIConstants.kt](file:///workspace/app/src/main/java/com/lidesheng/hyperlyric/common/UIConstants.kt) | UI 相关常量 |

#### 3.1.2 核心工具类

| 类名 | 职责 |
|------|------|
| [PrefsBridge.kt](file:///workspace/app/src/main/java/com/lidesheng/hyperlyric/common/PrefsBridge.kt) | SharedPreferences 桥接，支持 App 进程与 Xposed 远程进程配置同步 |
| [HyperLogger.kt](file:///workspace/app/src/main/java/com/lidesheng/hyperlyric/common/HyperLogger.kt) | 日志接口抽象，统一日志输出规范 |

#### 3.1.3 歌词解析工具

| 类名 | 职责 |
|------|------|
| [LrcParser.kt](file:///workspace/app/src/main/java/com/lidesheng/hyperlyric/common/lyric/LrcParser.kt) | LRC 格式歌词解析器 |
| [LyricInfoParser.kt](file:///workspace/app/src/main/java/com/lidesheng/hyperlyric/common/lyric/LyricInfoParser.kt) | 歌词信息解析器 |
| [LyricSplitter.kt](file:///workspace/app/src/main/java/com/lidesheng/hyperlyric/common/lyric/LyricSplitter.kt) | 歌词文本分割器，用于左右岛布局分割 |
| [RichLyricLineSplitter.kt](file:///workspace/app/src/main/java/com/lidesheng/hyperlyric/common/lyric/RichLyricLineSplitter.kt) | 富文本歌词行分割器 |

#### 3.1.4 媒体与图片工具

| 类名 | 职责 |
|------|------|
| [ColorExtractor.kt](file:///workspace/app/src/main/java/com/lidesheng/hyperlyric/common/color/ColorExtractor.kt) | 专辑封面颜色提取 |
| [AlbumImageHelper.kt](file:///workspace/app/src/main/java/com/lidesheng/hyperlyric/common/image/AlbumImageHelper.kt) | 专辑图片处理助手 |
| [MediaMetadataHelper.kt](file:///workspace/app/src/main/java/com/lidesheng/hyperlyric/common/media/MediaMetadataHelper.kt) | 媒体元数据获取助手 |

### 3.2 lyric 歌词模块

**路径**: [app/src/main/java/com/lidesheng/hyperlyric/lyric](file:///workspace/app/src/main/java/com/lidesheng/hyperlyric/lyric)

#### 3.2.1 数据模型 (model)

| 类名 | 职责 |
|------|------|
| [Song.kt](file:///workspace/app/src/main/java/com/lidesheng/hyperlyric/lyric/model/Song.kt) | 歌曲信息模型，包含 id、名称、艺术家、时长、歌词列表等 |
| [LyricLine.kt](file:///workspace/app/src/main/java/com/lidesheng/hyperlyric/lyric/model/LyricLine.kt) | 歌词行模型，包含开始/结束时间、文本、单词列表 |
| [RichLyricLine.kt](file:///workspace/app/src/main/java/com/lidesheng/hyperlyric/lyric/model/RichLyricLine.kt) | 富文本歌词行 |
| [LyricWord.kt](file:///workspace/app/src/main/java/com/lidesheng/hyperlyric/lyric/model/LyricWord.kt) | 歌词单词模型，支持逐字歌词 |
| [LyricMetadata.kt](file:///workspace/app/src/main/java/com/lidesheng/hyperlyric/lyric/model/LyricMetadata.kt) | 歌词元数据（翻译、音译等） |
| [LyricTiming.kt](file:///workspace/app/src/main/java/com/lidesheng/hyperlyric/lyric/model/LyricTiming.kt) | 歌词时间信息 |
| [LyricModels.kt](file:///workspace/app/src/main/java/com/lidesheng/hyperlyric/lyric/LyricModels.kt) | LrcLine 数据类定义 |

#### 3.2.2 歌词源管理 (source)

| 类名 | 职责 |
|------|------|
| [LyricSource.kt](file:///workspace/app/src/main/java/com/lidesheng/hyperlyric/lyric/source/LyricSource.kt) | 歌词源接口，定义 start/stop 等生命周期方法 |
| [LyricSink.kt](file:///workspace/app/src/main/java/com/lidesheng/hyperlyric/lyric/source/LyricSink.kt) | 歌词接收器接口，接收歌词更新 |
| [SourceManager.kt](file:///workspace/app/src/main/java/com/lidesheng/hyperlyric/lyric/source/SourceManager.kt) | 歌词源管理器 |
| [StateResetter.kt](file:///workspace/app/src/main/java/com/lidesheng/hyperlyric/lyric/source/StateResetter.kt) | 状态重置接口 |

#### 3.2.3 动态数据与配置

| 类名 | 职责 |
|------|------|
| [DynamicLyricData.kt](file:///workspace/app/src/main/java/com/lidesheng/hyperlyric/lyric/DynamicLyricData.kt) | 全局歌词状态容器，使用 StateFlow 驱动 UI 更新 |

#### 3.2.4 LyricState 数据结构

定义在 [DynamicLyricData.kt](file:///workspace/app/src/main/java/com/lidesheng/hyperlyric/lyric/DynamicLyricData.kt):

```kotlin
data class LyricState(
    val islandTitleLeft: String,        // 超级岛左侧标题
    val islandTitleRight: String,       // 超级岛右侧标题
    val notificationTitleLeft: String,  // 通知左侧标题
    val notificationTitleRight: String, // 通知右侧标题
    val songLyric: String,              // 歌曲歌词文本
    val songInfo: String,               // 歌曲信息（歌名-歌手）
    val showIslandLeftAlbum: Boolean,   // 是否显示左侧专辑
    val duration: Long,                 // 歌曲时长
    val isPlaying: Boolean,             // 是否播放中
    val targetPackageName: String,      // 目标音乐包名
    val albumColor: Int,                // 专辑主色
    val albumColorEnd: Int,             // 专辑渐变色
    val albumBitmap: Bitmap?,           // 专辑封面
    val playbackAnchor: PlaybackAnchor  // 播放锚点（用于进度计算）
)
```

### 3.3 root Hook 模块

**路径**: [app/src/main/java/com/lidesheng/hyperlyric/root](file:///workspace/app/src/main/java/com/lidesheng/hyperlyric/root)

#### 3.3.1 Hook 入口

| 类名 | 职责 |
|------|------|
| [HookEntry.kt](file:///workspace/app/src/main/java/com/lidesheng/hyperlyric/root/HookEntry.kt) | Xposed 模块入口，继承 XposedModule，负责注入 SystemUI 和插件 |
| [RootApplication.kt](file:///workspace/app/src/main/java/com/lidesheng/hyperlyric/root/RootApplication.kt) | Application 类，负责 Xposed 服务绑定与配置同步 |

**HookEntry 核心流程**:

1. **onModuleLoaded**: 模块加载时初始化
2. **onPackageLoaded**: 
   - 针对 `com.android.systemui`: 注入白名单解锁、Application 生命周期、ClassLoader 劫持
   - 针对 `miui.systemui.plugin`: 注入超级岛视图 Hook
3. **AppCreateHooker**: 在 SystemUI 的 Application.onCreate 后初始化 Lyricon 歌词源和渲染器
4. **ClassLoaderHooker**: 劫持 ClassLoader 构造，捕获动态加载的插件并注入

#### 3.3.2 超级岛注入 (island)

| 类名 | 职责 |
|------|------|
| [RealIslandHooker.kt](file:///workspace/app/src/main/java/com/lidesheng/hyperlyric/root/island/RealIslandHooker.kt) | 实际的超级岛视图 Hook 实现 |
| [IslandHostFacade.kt](file:///workspace/app/src/main/java/com/lidesheng/hyperlyric/root/island/IslandHostFacade.kt) | 超级岛宿主外观类，封装注入/清除/刷新操作 |
| [IslandLyricTextInjector.kt](file:///workspace/app/src/main/java/com/lidesheng/hyperlyric/root/island/IslandLyricTextInjector.kt) | 歌词文本注入器 |
| [IslandViewHelper.kt](file:///workspace/app/src/main/java/com/lidesheng/hyperlyric/root/island/IslandViewHelper.kt) | 视图操作辅助类 |
| [IslandViewRegistry.kt](file:///workspace/app/src/main/java/com/lidesheng/hyperlyric/root/island/IslandViewRegistry.kt) | 注入视图注册表 |
| [IslandProbeUtils.kt](file:///workspace/app/src/main/java/com/lidesheng/hyperlyric/root/island/IslandProbeUtils.kt) | 超级岛探测工具，提取媒体岛信息 |
| [SystemUIHookRegistry.kt](file:///workspace/app/src/main/java/com/lidesheng/hyperlyric/root/island/SystemUIHookRegistry.kt) | SystemUI Hook 注册表 |
| [HookIslandGlow.kt](file:///workspace/app/src/main/java/com/lidesheng/hyperlyric/root/HookIslandGlow.kt) | 岛光效 Hook |

**超级岛注入流程**:

```
UpdateBigIslandViewHook
    ↓
拦截系统更新超级岛视图的调用
    ↓
前置：轻量恢复已注入的歌词视图
    ↓
调用原方法（系统正常渲染）
    ↓
后置：
  1. 检查超级岛开关
  2. 提取媒体岛信息
  3. 判断是否为当前歌词岛
  4. 注入歌词槽位（如未注入）
  5. 刷新歌词内容
  6. 注入光晕效果
```

#### 3.3.3 Root 进程歌词源 (source)

| 类名 | 职责 |
|------|------|
| [LyriconSource.kt](file:///workspace/app/src/main/java/com/lidesheng/hyperlyric/root/source/LyriconSource.kt) | Lyricon 歌词源，通过 Lyricon Subscriber SDK 获取歌词 |
| [RootLyricSink.kt](file:///workspace/app/src/main/java/com/lidesheng/hyperlyric/root/source/RootLyricSink.kt) | Root 进程歌词接收器，将歌词传递给渲染器 |

#### 3.3.4 桥接 (bridge)

| 类名 | 职责 |
|------|------|
| [AppBridge.kt](file:///workspace/app/src/main/java/com/lidesheng/hyperlyric/root/bridge/AppBridge.kt) | App 与模块状态桥接 |
| [IpcRouter.kt](file:///workspace/app/src/main/java/com/lidesheng/hyperlyric/root/bridge/IpcRouter.kt) | IPC 路由 |
| [LyriconBridge.kt](file:///workspace/app/src/main/java/com/lidesheng/hyperlyric/root/bridge/LyriconBridge.kt) | Lyricon 桥接 |

#### 3.3.5 白名单解锁

| 类名 | 职责 |
|------|------|
| [UnlockFocusWhitelist.kt](file:///workspace/app/src/main/java/com/lidesheng/hyperlyric/root/UnlockFocusWhitelist.kt) | 解锁焦点通知白名单 |
| [UnlockIslandWhitelist.kt](file:///workspace/app/src/main/java/com/lidesheng/hyperlyric/root/UnlockIslandWhitelist.kt) | 解锁超级岛白名单 |

### 3.4 ui 界面模块

**路径**: [app/src/main/java/com/lidesheng/hyperlyric/ui](file:///workspace/app/src/main/java/com/lidesheng/hyperlyric/ui)

#### 3.4.1 主入口

| 类名 | 职责 |
|------|------|
| [MainActivity.kt](file:///workspace/app/src/main/java/com/lidesheng/hyperlyric/ui/MainActivity.kt) | 主 Activity，Jetpack Compose 入口 |

#### 3.4.2 导航 (navigation)

| 类名 | 职责 |
|------|------|
| [AppNavigation.kt](file:///workspace/app/src/main/java/com/lidesheng/hyperlyric/ui/navigation/AppNavigation.kt) | 应用导航图，定义所有页面路由 |
| [Navigator.kt](file:///workspace/app/src/main/java/com/lidesheng/hyperlyric/ui/navigation/Navigator.kt) | 导航器封装 |
| [Route.kt](file:///workspace/app/src/main/java/com/lidesheng/hyperlyric/ui/navigation/Route.kt) | 路由定义（sealed class） |

#### 3.4.3 页面 (page)

| 页面 | 职责 |
|------|------|
| [MainPage.kt](file:///workspace/app/src/main/java/com/lidesheng/hyperlyric/ui/page/MainPage.kt) | 主页面 |
| [SetupPage.kt](file:///workspace/app/src/main/java/com/lidesheng/hyperlyric/ui/page/SetupPage.kt) | 引导设置页 |
| [HookSettingsPage.kt](file:///workspace/app/src/main/java/com/lidesheng/hyperlyric/ui/page/HookSettingsPage.kt) | Hook 模块设置页 |
| [LyricSettingsPage.kt](file:///workspace/app/src/main/java/com/lidesheng/hyperlyric/ui/page/hooksettings/LyricSettingsPage.kt) | 歌词设置页 |
| [LogPage.kt](file:///workspace/app/src/main/java/com/lidesheng/hyperlyric/ui/page/LogPage.kt) | 日志查看页 |
| [HelpPage.kt](file:///workspace/app/src/main/java/com/lidesheng/hyperlyric/ui/page/HelpPage.kt) | 帮助页 |
| [ChangelogPage.kt](file:///workspace/app/src/main/java/com/lidesheng/hyperlyric/ui/page/ChangelogPage.kt) | 更新日志页 |
| [LicensesPage.kt](file:///workspace/app/src/main/java/com/lidesheng/hyperlyric/ui/page/LicensesPage.kt) | 开源许可证页 |
| [PoetryPage.kt](file:///workspace/app/src/main/java/com/lidesheng/hyperlyric/ui/page/PoetryPage.kt) | 诗词页 |

---

## 4. 核心功能详解

### 4.1 歌词源管理

#### 4.1.1 架构设计

系统采用**源-接收器**模式（Source-Sink Pattern）：

```
LyriconSource (提供者) → RootLyricSink (消费者)
         ↑                      ↑
         │                      │
    SourceManager           渲染器
    (管理)               (超级岛显示)
```

#### 4.1.2 Root 体系

项目仅使用 Root 体系（SystemUI 进程）进行超级岛歌词显示，唯一歌词源为 LyriconSource。

### 4.2 超级岛歌词注入

#### 4.2.1 技术原理

通过 Xposed Hook MIUI SystemUI 的超级岛渲染流程，在系统原有视图基础上注入自定义歌词视图。

#### 4.2.2 Hook 点

1. **Application.onCreate**: 初始化 Lyricon 歌词源和渲染器
2. **ClassLoader 构造**: 捕获动态加载的插件
3. **超级岛视图更新**: 拦截 `updateBigIslandView` 等方法
4. **布局可见性变化**: 监听视图可见性变化，恢复歌词视图

#### 4.2.3 关键类协作

```
RealIslandHooker.UpdateBigIslandViewHook
    ↓
IslandProbeUtils.extractMediaIslandInfo()  提取媒体岛信息
    ↓
IslandTextHookerSupport.isCurrentLyricIsland()  判断是否为当前播放应用
    ↓
IslandLyricTextInjector.injectSlots()  注入歌词槽位
    ↓
IslandLyricTextInjector.refreshCurrentContent()  刷新歌词内容
    ↓
IslandHostFacade.injectHostGlow()  注入光晕效果
```

---

## 5. 关键数据结构

### 5.1 Song（歌曲）

定义在 [Song.kt](file:///workspace/app/src/main/java/com/lidesheng/hyperlyric/lyric/model/Song.kt):

| 字段 | 类型 | 说明 |
|------|------|------|
| id | String? | 歌曲 ID |
| name | String? | 歌曲名 |
| artist | String? | 艺术家 |
| duration | Long | 歌曲时长（毫秒） |
| metadata | LyricMetadata? | 歌词元数据 |
| lyrics | List\<RichLyricLine\>? | 歌词列表 |

### 5.2 LyricLine（歌词行）

定义在 [LyricLine.kt](file:///workspace/app/src/main/java/com/lidesheng/hyperlyric/lyric/model/LyricLine.kt):

| 字段 | 类型 | 说明 |
|------|------|------|
| begin | Long | 开始时间（毫秒） |
| end | Long | 结束时间（毫秒） |
| duration | Long | 持续时间（毫秒） |
| isAlignedRight | Boolean | 是否右对齐 |
| metadata | LyricMetadata? | 元数据（翻译等） |
| text | String? | 文本内容 |
| words | List\<LyricWord\>? | 单词列表（逐字歌词） |

### 5.3 LyricState（歌词状态）

定义在 [DynamicLyricData.kt](file:///workspace/app/src/main/java/com/lidesheng/hyperlyric/lyric/DynamicLyricData.kt):

全局单例状态容器，使用 Kotlin Flow 驱动响应式更新。

### 5.4 PlaybackAnchor（播放锚点）

定义在 [DynamicLyricData.kt](file:///workspace/app/src/main/java/com/lidesheng/hyperlyric/lyric/DynamicLyricData.kt):

| 字段 | 类型 | 说明 |
|------|------|------|
| position | Long | 播放位置（毫秒） |
| timestamp | Long | 锚点时间戳（elapsedRealtime） |
| speed | Float | 播放速度 |
| isPlaying | Boolean | 是否播放中 |

### 5.5 支持的音乐应用

定义在 [DynamicLyricData.kt](file:///workspace/app/src/main/java/com/lidesheng/hyperlyric/lyric/DynamicLyricData.kt):

| 包名 | 应用名 |
|------|--------|
| com.salt.music | Salt Player |
| com.netease.cloudmusic | 网易云音乐 |
| com.tencent.qqmusic | QQ 音乐 |
| cn.kuwo.player | 酷我音乐 |
| com.kugou.android | 酷狗音乐 |
| com.apple.android.music | Apple Music |
| com.spotify.music | Spotify |
| cmccwm.mobilemusic | 咪咕音乐 |
| com.luna.music | 汽水音乐 |
| com.kugou.android.lite | 酷狗音乐概念版 |
| com.google.android.apps.youtube.music | YouTube Music |
| cn.wenyu.bodian | 波点音乐 |
| com.miui.player | 小米音乐 |
| com.xuncorp.qinalt.music | 青盐云听 |

---

## 6. 依赖关系

### 6.1 主要依赖库

| 依赖 | 版本 | 用途 |
|------|------|------|
| AndroidX Core KTX | 1.17.0 | Android KTX 扩展 |
| Material | 1.13.0 | Material Design 组件 |
| Jetpack Compose BOM | 2026.04.01 | Compose 版本管理 |
| Compose UI | - | Compose UI 核心 |
| Compose Material3 | - | Material 3 组件 |
| MIUI X (miuix) | 0.9.1 | MIUI 风格组件库 |
| Navigation3 | 1.1.0 | 导航组件 |
| Palette KTX | 1.0.0 | 颜色提取 |
| HiddenApiBypass | 4.3 | 隐藏 API 绕过 |
| LibXposed API/Service | 101.0.0 | Xposed 框架 |
| Lyricon Subscriber | 0.1.70 | Lyricon 歌词 SDK |
| Kotlinx Serialization | 1.6.3 | JSON 序列化 |
| Kotlinx Coroutines | 1.9.0 | 协程 |
| YoYo Animations | 2.4 | 动画库 |
| YoYo Easing | 2.4 | 缓动函数 |

完整版本定义在 [gradle/libs.versions.toml](file:///workspace/gradle/libs.versions.toml)。

### 6.2 模块间依赖关系

```
ui → lyric → common
              ↓
            root (Xposed 模块，独立进程)
```

### 6.3 AIDL 接口

路径: [app/src/main/aidl/com/lidesheng/hyperlyric](file:///workspace/app/src/main/aidl/com/lidesheng/hyperlyric)

| 接口 | 用途 |
|------|------|
| IBridgeCallback.aidl | 桥接回调 |
| Song.aidl | 歌曲数据 AIDL |

---

## 7. 项目构建与运行

### 7.1 环境要求

- Android Studio (支持 AGP 9.2.1)
- JDK 17
- Android SDK 37 (compileSdk)
- minSdk: 35 (Android 15)
- targetSdk: 37
- 仅支持 arm64-v8a ABI

### 7.2 构建命令

```bash
# 构建 Release 版本
./gradlew assembleRelease

# 构建 Debug 版本
./gradlew assembleDebug
```

### 7.3 安装与使用

1. **安装 APK**: 安装构建生成的 APK
2. **激活 Xposed 模块**: 在 LSPosed 等框架中激活 HyperLyric，作用域为系统界面（SystemUI）
3. **配置应用**: 打开 App 进行各项配置
4. **重启 SystemUI**: 重启系统界面以激活 Hook

### 7.4 Xposed 模块配置

模块配置文件位于 [app/src/main/resources/META-INF/xposed](file:///workspace/app/src/main/resources/META-INF/xposed)

### 7.5 CI/CD

GitHub Actions 配置位于 [.github/workflows/android_ci.yml](file:///workspace/.github/workflows/android_ci.yml)

---

## 附录

### A. 配置同步机制

App 进程与 Xposed（SystemUI）进程通过以下机制同步配置：

1. **PrefsBridge**: App 进程写入配置时，调用 `RootApplication.syncPreference()` 同步到远程
2. **XposedService**: 通过 LibXposed 的 `getRemotePreferences()` 获取远程配置
3. **OnSharedPreferenceChangeListener**: 监听配置变化，实时更新运行时状态

### B. 日志系统

- **HyperLogger 接口**: 统一的日志抽象
- **HookLogger**: Root 进程的日志实现
- **LogManager**: App 进程的日志管理

### C. 备份与恢复

- [BackupRestoreManager.kt](file:///workspace/app/src/main/java/com/lidesheng/hyperlyric/utils/BackupRestoreManager.kt): 配置备份恢复管理

---

*文档版本: 4.0 (Super Island Only)*  
*生成日期: 2026-07-13*
