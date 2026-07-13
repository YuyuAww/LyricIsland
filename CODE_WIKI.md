# HyperLyric Code Wiki

## 目录

- [1. 项目概述](#1-项目概述)
- [2. 项目架构](#2-项目架构)
- [3. 核心模块详解](#3-核心模块详解)
  - [3.1 common 公共模块](#31-common-公共模块)
  - [3.2 lyric 歌词模块](#32-lyric-歌词模块)
  - [3.3 root Hook 模块](#33-root-hook-模块)
  - [3.4 service 服务模块](#34-service-服务模块)
  - [3.5 ui 界面模块](#35-ui-界面模块)
- [4. 核心功能详解](#4-核心功能详解)
  - [4.1 歌词源管理系统](#41-歌词源管理系统)
  - [4.2 超级岛歌词注入](#42-超级岛歌词注入)
  - [4.3 通知歌词展示](#43-通知歌词展示)
  - [4.4 AI 歌词翻译](#44-ai-歌词翻译)
  - [4.5 在线歌词搜索](#45-在线歌词搜索)
- [5. 关键数据结构](#5-关键数据结构)
- [6. 依赖关系](#6-依赖关系)
- [7. 项目构建与运行](#7-项目构建与运行)
- [8. 产品 Flavor](#8-产品-flavor)

---

## 1. 项目概述

**项目名称**: HyperLyric (音乐岛 / LyricIsland)

**项目类型**: Android 歌词显示应用

**核心功能**:
- 通过 Xposed Hook 方式在 MIUI 超级岛（灵动岛）中显示歌词
- 通过通知方式显示歌词（焦点通知/普通通知）
- 支持多种歌词源（Lyricon、SuperLyric、在线歌词等）
- AI 歌词翻译功能
- 丰富的样式自定义选项

**技术栈**:
- 语言: Kotlin
- UI 框架: Jetpack Compose + MIUI X 组件库
- Hook 框架: LibXposed
- 权限框架: Shizuku
- 网络请求: Retrofit + OkHttp
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
│                      服务层 (service)                        │
│  LiveLyricService / NotificationPresenter / MetadataSource  │
└────────────────────────────┬────────────────────────────────┘
                             │
┌────────────────────────────▼────────────────────────────────┐
│                      歌词核心层 (lyric)                      │
│  数据模型 / SourceManager / ConfigRepository / DynamicData  │
└────────────────────────────┬────────────────────────────────┘
                             │
          ┌──────────────────┴──────────────────┐
          │                                     │
┌─────────▼─────────┐               ┌───────────▼─────────────┐
│  Root Hook 层     │               │   公共层 (common)        │
│  (Xposed 模块)    │               │   工具类/常量/解析器     │
│  HookEntry        │               └─────────────────────────┘
│  IslandHooker     │
│  AITranslator     │
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
│   ├── aitrans/               # AI 翻译
│   ├── bridge/                # IPC 桥接
│   ├── island/                # 超级岛注入
│   ├── source/                # Root 进程歌词源
│   └── utils/                 # Hook 工具
├── service/                   # 前台服务
│   ├── scheduler/             # 调度器
│   ├── source/                # 服务端歌词源
│   └── utils/                 # 通知构建等
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
| [RootConstants.kt](file:///workspace/app/src/main/java/com/lidesheng/hyperlyric/common/RootConstants.kt) | Root Hook 相关的配置键名与默认值，包括超级岛、样式、动画、翻译、AI 翻译等配置 |
| [ServiceConstants.kt](file:///workspace/app/src/main/java/com/lidesheng/hyperlyric/common/ServiceConstants.kt) | 服务层相关配置键名与默认值，包括通知类型、白名单、歌词源类型等 |
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
| [LrcParser.kt](file:///workspace/app/src/main/java/com/lidesheng/hyperlyric/common/lyric/LrcParser.kt) | LRC 格式歌词解析器，支持标准时间标签解析 |
| [LyricInfoParser.kt](file:///workspace/app/src/main/java/com/lidesheng/hyperlyric/common/lyric/LyricInfoParser.kt) | 歌词信息解析器 |
| [LyricSplitter.kt](file:///workspace/app/src/main/java/com/lidesheng/hyperlyric/common/lyric/LyricSplitter.kt) | 歌词文本分割器，用于左右岛/通知布局分割 |
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

#### 3.2.2 歌词源管理 (source)

| 类名 | 职责 |
|------|------|
| [LyricSource.kt](file:///workspace/app/src/main/java/com/lidesheng/hyperlyric/lyric/source/LyricSource.kt) | 歌词源接口，定义 start/stop 等生命周期方法 |
| [LyricSink.kt](file:///workspace/app/src/main/java/com/lidesheng/hyperlyric/lyric/source/LyricSink.kt) | 歌词接收器接口，接收歌词更新 |
| [SourceManager.kt](file:///workspace/app/src/main/java/com/lidesheng/hyperlyric/lyric/source/SourceManager.kt) | 歌词源管理器，负责切换和管理多个歌词源 |
| [StateResetter.kt](file:///workspace/app/src/main/java/com/lidesheng/hyperlyric/lyric/source/StateResetter.kt) | 状态重置接口 |

**SourceManager 工作流程**:
1. 启动时根据配置选择默认歌词源
2. 监听配置变更，动态切换歌词源
3. 维护当前活跃歌词源的生命周期

#### 3.2.3 动态数据与配置

| 类名 | 职责 |
|------|------|
| [DynamicLyricData.kt](file:///workspace/app/src/main/java/com/lidesheng/hyperlyric/lyric/DynamicLyricData.kt) | 全局歌词状态容器，使用 StateFlow 驱动 UI 更新 |
| [ConfigRepository.kt](file:///workspace/app/src/main/java/com/lidesheng/hyperlyric/lyric/ConfigRepository.kt) | 配置仓库，管理通知白名单等 |
| [LyricProviderFactory.kt](file:///workspace/app/src/main/java/com/lidesheng/hyperlyric/lyric/LyricProviderFactory.kt) | 歌词提供者工厂，根据 flavor 创建不同实现 |

#### 3.2.4 LyricState 数据结构

定义在 [DynamicLyricData.kt](file:///workspace/app/src/main/java/com/lidesheng/hyperlyric/lyric/DynamicLyricData.kt#L36-L57):

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
3. **AppCreateHooker**: 在 SystemUI 的 Application.onCreate 后初始化歌词源、渲染器、AI 翻译等
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
| [SuperLyricSource.kt](file:///workspace/app/src/main/java/com/lidesheng/hyperlyric/root/source/SuperLyricSource.kt) | SuperLyric API 歌词源 |
| [LyricInfoSource.kt](file:///workspace/app/src/main/java/com/lidesheng/hyperlyric/root/source/LyricInfoSource.kt) | 歌词信息源 |
| [RootLyricSink.kt](file:///workspace/app/src/main/java/com/lidesheng/hyperlyric/root/source/RootLyricSink.kt) | Root 进程歌词接收器，将歌词传递给渲染器 |

#### 3.3.4 AI 翻译 (aitrans)

| 类名 | 职责 |
|------|------|
| [AITranslator.kt](file:///workspace/app/src/main/java/com/lidesheng/hyperlyric/root/aitrans/AITranslator.kt) | AI 翻译门面，对外提供翻译接口 |
| [AITranslationScheduler.kt](file:///workspace/app/src/main/java/com/lidesheng/hyperlyric/root/aitrans/AITranslationScheduler.kt) | 翻译调度器，管理并发与队列 |
| [AITranslationCache.kt](file:///workspace/app/src/main/java/com/lidesheng/hyperlyric/root/aitrans/AITranslationCache.kt) | 翻译缓存（内存+SQLite） |
| [AITranslationKey.kt](file:///workspace/app/src/main/java/com/lidesheng/hyperlyric/root/aitrans/AITranslationKey.kt) | 翻译缓存键生成 |
| [OpenAiTranslationClient.kt](file:///workspace/app/src/main/java/com/lidesheng/hyperlyric/root/aitrans/OpenAiTranslationClient.kt) | OpenAI 兼容 API 客户端 |
| [AITranslationPrompt.kt](file:///workspace/app/src/main/java/com/lidesheng/hyperlyric/root/aitrans/AITranslationPrompt.kt) | AI 翻译 Prompt 构建 |
| [AITranslationResponseParser.kt](file:///workspace/app/src/main/java/com/lidesheng/hyperlyric/root/aitrans/AITranslationResponseParser.kt) | AI 响应解析器 |
| [AITranslationApplicator.kt](file:///workspace/app/src/main/java/com/lidesheng/hyperlyric/root/aitrans/AITranslationApplicator.kt) | 翻译结果应用器 |
| [AITranslationModels.kt](file:///workspace/app/src/main/java/com/lidesheng/hyperlyric/root/aitrans/AITranslationModels.kt) | 翻译相关数据模型 |

**AI 翻译架构**:

```
AITranslator (门面)
    ├── 缓存检查: 内存 → SQLite
    ├── AITranslationScheduler (调度)
    │   ├── 并发限制: 最多3个运行中
    │   ├── 队列限制: 最多5个等待中
    │   └── 同 key 复用
    └── OpenAiTranslationClient (网络)
        ├── HTTP POST 请求
        └── JSON 响应解析
```

#### 3.3.5 桥接 (bridge)

| 类名 | 职责 |
|------|------|
| [AppBridge.kt](file:///workspace/app/src/main/java/com/lidesheng/hyperlyric/root/bridge/AppBridge.kt) | App 与模块状态桥接 |
| [IpcRouter.kt](file:///workspace/app/src/main/java/com/lidesheng/hyperlyric/root/bridge/IpcRouter.kt) | IPC 路由 |
| [LyriconBridge.kt](file:///workspace/app/src/main/java/com/lidesheng/hyperlyric/root/bridge/LyriconBridge.kt) | Lyricon 桥接 |

#### 3.3.6 白名单解锁

| 类名 | 职责 |
|------|------|
| [UnlockFocusWhitelist.kt](file:///workspace/app/src/main/java/com/lidesheng/hyperlyric/root/UnlockFocusWhitelist.kt) | 解锁焦点通知白名单 |
| [UnlockIslandWhitelist.kt](file:///workspace/app/src/main/java/com/lidesheng/hyperlyric/root/UnlockIslandWhitelist.kt) | 解锁超级岛白名单 |

### 3.4 service 服务模块

**路径**: [app/src/main/java/com/lidesheng/hyperlyric/service](file:///workspace/app/src/main/java/com/lidesheng/hyperlyric/service)

#### 3.4.1 核心服务

| 类名 | 职责 |
|------|------|
| [LiveLyricService.kt](file:///workspace/app/src/main/java/com/lidesheng/hyperlyric/service/LiveLyricService.kt) | 歌词监听服务，继承 NotificationListenerService |
| [LyricTileService.kt](file:///workspace/app/src/main/java/com/lidesheng/hyperlyric/service/LyricTileService.kt) | 快捷设置磁贴服务 |
| [NotificationPresenter.kt](file:///workspace/app/src/main/java/com/lidesheng/hyperlyric/service/NotificationPresenter.kt) | 通知展示调度中心 |

**LiveLyricService 工作流程**:

1. **onCreate**: 初始化组件
   - 创建文本画笔和歌词分割器
   - 初始化 NotificationPresenter
   - 初始化白名单配置
   - 创建 MetadataSource（从通知中提取媒体信息）
   - 创建 AppLyricSink（处理歌词更新）
   - 收集状态流并驱动通知更新

2. **onListenerConnected**: 通知监听服务连接成功
3. **onDestroy**: 清理资源

#### 3.4.2 服务端歌词源 (source)

| 类名 | 职责 |
|------|------|
| [ServiceLyricSource.kt](file:///workspace/app/src/main/java/com/lidesheng/hyperlyric/service/source/ServiceLyricSource.kt) | 服务端歌词源接口 |
| [ServiceSourceManager.kt](file:///workspace/app/src/main/java/com/lidesheng/hyperlyric/service/source/ServiceSourceManager.kt) | 服务端歌词源管理器 |
| [AutoLyricSource.kt](file:///workspace/app/src/main/java/com/lidesheng/hyperlyric/service/source/AutoLyricSource.kt) | 自动歌词源（优先 LyricInfo，回退 LRC） |
| [LyricInfoLyricSource.kt](file:///workspace/app/src/main/java/com/lidesheng/hyperlyric/service/source/LyricInfoLyricSource.kt) | 歌词信息源 |
| [MetadataLrcLyricSource.kt](file:///workspace/app/src/main/java/com/lidesheng/hyperlyric/service/source/MetadataLrcLyricSource.kt) | 元数据 LRC 歌词源 |
| [OnlineLyricSource.kt](file:///workspace/app/src/main/java/com/lidesheng/hyperlyric/service/source/OnlineLyricSource.kt) | 在线歌词源 |
| [TitleLyricSource.kt](file:///workspace/app/src/main/java/com/lidesheng/hyperlyric/service/source/TitleLyricSource.kt) | 标题歌词源（仅显示歌名） |
| [MetadataSource.kt](file:///workspace/app/src/main/java/com/lidesheng/hyperlyric/service/source/MetadataSource.kt) | 元数据源，从通知提取媒体信息 |
| [AppLyricSink.kt](file:///workspace/app/src/main/java/com/lidesheng/hyperlyric/service/source/AppLyricSink.kt) | App 进程歌词接收器 |
| [SyncData.kt](file:///workspace/app/src/main/java/com/lidesheng/hyperlyric/service/source/SyncData.kt) | 同步数据模型 |

**歌词源类型** (定义在 [ServiceConstants.kt](file:///workspace/app/src/main/java/com/lidesheng/hyperlyric/common/ServiceConstants.kt)):

| 常量 | 值 | 说明 |
|------|-----|------|
| LYRIC_SOURCE_AUTO | 0 | 自动选择 |
| LYRIC_SOURCE_ONLINE | 1 | 在线歌词 |
| LYRIC_SOURCE_LYRIC_INFO | 2 | 歌词信息 |
| LYRIC_SOURCE_LRC | 3 | LRC 元数据 |
| LYRIC_SOURCE_TITLE | 4 | 仅标题 |

#### 3.4.3 通知工具 (utils)

| 类名 | 职责 |
|------|------|
| [NotificationBuilder.kt](file:///workspace/app/src/main/java/com/lidesheng/hyperlyric/service/utils/NotificationBuilder.kt) | 通知构建器，支持普通通知和焦点通知 |
| [FocusNotificationBuilder.kt](file:///workspace/app/src/main/java/com/lidesheng/hyperlyric/service/utils/FocusNotificationBuilder.kt) | 焦点通知构建器 |

**NotificationPresenter 通知调度逻辑**:

```
updateState(globalState, force)
    ↓
检查白名单 → 不在白名单则清除通知
    ↓
检查灵动岛开关 → 关闭则清除通知
    ↓
计算当前播放进度
    ↓
构建 UiState
    ↓
状态去重检查
    ↓
屏幕状态检查（息屏降频）
    ↓
播放状态防抖（暂停 150ms 后清除）
    ↓
dispatchNotifications
    ├── 普通通知模式
    └── 焦点通知模式
        └── 可选: Shizuku 闪断 XMSF 联网绕过限制
```

### 3.5 ui 界面模块

**路径**: [app/src/main/java/com/lidesheng/hyperlyric/ui](file:///workspace/app/src/main/java/com/lidesheng/hyperlyric/ui)

#### 3.5.1 主入口

| 类名 | 职责 |
|------|------|
| [MainActivity.kt](file:///workspace/app/src/main/java/com/lidesheng/hyperlyric/ui/MainActivity.kt) | 主 Activity，Jetpack Compose 入口 |

#### 3.5.2 导航 (navigation)

| 类名 | 职责 |
|------|------|
| [AppNavigation.kt](file:///workspace/app/src/main/java/com/lidesheng/hyperlyric/ui/navigation/AppNavigation.kt) | 应用导航图，定义所有页面路由 |
| [Navigator.kt](file:///workspace/app/src/main/java/com/lidesheng/hyperlyric/ui/navigation/Navigator.kt) | 导航器封装 |
| [Route.kt](file:///workspace/app/src/main/java/com/lidesheng/hyperlyric/ui/navigation/Route.kt) | 路由定义（sealed class） |

**路由列表**:

| 路由 | 页面 |
|------|------|
| Route.Setup | 引导设置页 |
| Route.Main | 主页 |
| Route.Settings | 设置页 |
| Route.HookSettings | Hook 设置页 |
| Route.LyricProvider | 歌词源设置页 |
| Route.LyricAnimation | 歌词动画页 |
| Route.LyricSettings | 歌词设置页 |
| Route.SuperIslandSettings | 超级岛设置页 |
| Route.DynamicIslandNotification | 灵动岛通知页 |
| Route.Log | 日志页 |
| Route.Licenses | 许可证页 |
| Route.Poetry | 诗词页 |
| Route.Help | 帮助页 |
| Route.Changelog | 更新日志页 |

#### 3.5.3 页面 (page)

| 页面 | 职责 |
|------|------|
| [MainPage.kt](file:///workspace/app/src/main/java/com/lidesheng/hyperlyric/ui/page/MainPage.kt) | 主页面 |
| [SetupPage.kt](file:///workspace/app/src/main/java/com/lidesheng/hyperlyric/ui/page/SetupPage.kt) | 引导设置页 |
| [SettingsPage.kt](file:///workspace/app/src/main/java/com/lidesheng/hyperlyric/ui/page/SettingsPage.kt) | 设置页 |
| [HookSettingsPage.kt](file:///workspace/app/src/main/java/com/lidesheng/hyperlyric/ui/page/HookSettingsPage.kt) | Hook 模块设置页 |
| [DynamicIslandNotificationPage.kt](file:///workspace/app/src/main/java/com/lidesheng/hyperlyric/ui/page/DynamicIslandNotificationPage.kt) | 灵动岛通知设置页 |
| [LogPage.kt](file:///workspace/app/src/main/java/com/lidesheng/hyperlyric/ui/page/LogPage.kt) | 日志查看页 |
| [HelpPage.kt](file:///workspace/app/src/main/java/com/lidesheng/hyperlyric/ui/page/HelpPage.kt) | 帮助页 |
| [ChangelogPage.kt](file:///workspace/app/src/main/java/com/lidesheng/hyperlyric/ui/page/ChangelogPage.kt) | 更新日志页 |
| [LicensesPage.kt](file:///workspace/app/src/main/java/com/lidesheng/hyperlyric/ui/page/LicensesPage.kt) | 开源许可证页 |
| [PoetryPage.kt](file:///workspace/app/src/main/java/com/lidesheng/hyperlyric/ui/page/PoetryPage.kt) | 诗词页 |

#### 3.5.4 组件 (component)

| 组件 | 职责 |
|------|------|
| [SuperComponent.kt](file:///workspace/app/src/main/java/com/lidesheng/hyperlyric/ui/component/SuperComponent.kt) | 超级组件 |
| [SuperSearchBar.kt](file:///workspace/app/src/main/java/com/lidesheng/hyperlyric/ui/component/SuperSearchBar.kt) | 搜索栏组件 |
| [SuperSwitchPreference.kt](file:///workspace/app/src/main/java/com/lidesheng/hyperlyric/ui/component/SuperSwitchPreference.kt) | 开关偏好组件 |
| [ProComponent.kt](file:///workspace/app/src/main/java/com/lidesheng/hyperlyric/ui/component/ProComponent.kt) | 专业版组件 |
| [Dialogs.kt](file:///workspace/app/src/main/java/com/lidesheng/hyperlyric/ui/component/Dialogs.kt) | 对话框组件 |
| [TagComponent.kt](file:///workspace/app/src/main/java/com/lidesheng/hyperlyric/ui/component/TagComponent.kt) | 标签组件 |
| [SearchStatus.kt](file:///workspace/app/src/main/java/com/lidesheng/hyperlyric/ui/component/SearchStatus.kt) | 搜索状态组件 |

#### 3.5.5 UI 工具 (utils)

| 类名 | 职责 |
|------|------|
| [ThemeUtils.kt](file:///workspace/app/src/main/java/com/lidesheng/hyperlyric/ui/utils/ThemeUtils.kt) | 主题工具 |
| [LocaleUtils.kt](file:///workspace/app/src/main/java/com/lidesheng/hyperlyric/ui/utils/LocaleUtils.kt) | 国际化工具 |
| [AppUtils.kt](file:///workspace/app/src/main/java/com/lidesheng/hyperlyric/ui/utils/AppUtils.kt) | 应用工具 |
| [PageUtils.kt](file:///workspace/app/src/main/java/com/lidesheng/hyperlyric/ui/utils/PageUtils.kt) | 页面工具 |
| [QuotesData.kt](file:///workspace/app/src/main/java/com/lidesheng/hyperlyric/ui/utils/QuotesData.kt) | 名言数据 |

---

## 4. 核心功能详解

### 4.1 歌词源管理系统

#### 4.1.1 架构设计

系统采用**源-接收器**模式（Source-Sink Pattern）：

```
LyricSource (提供者) → LyricSink (消费者)
      ↑                       ↑
      │                       │
  SourceManager          RootLyricSink / AppLyricSink
  (切换/管理)            (渲染/通知)
```

#### 4.1.2 两套歌词源体系

项目中有两套独立的歌词源体系，分别运行在不同进程中：

| 体系 | 运行进程 | 用途 | 歌词源 |
|------|---------|------|--------|
| Root 体系 | SystemUI 进程 | 超级岛歌词显示 | LyriconSource, SuperLyricSource, LyricInfoSource |
| Service 体系 | App 进程 | 通知歌词显示 | AutoLyricSource, OnlineLyricSource, LyricInfoLyricSource, MetadataLrcLyricSource, TitleLyricSource |

#### 4.1.3 SourceManager 核心方法

定义在 [SourceManager.kt](file:///workspace/app/src/main/java/com/lidesheng/hyperlyric/lyric/source/SourceManager.kt):

- `start()`: 启动歌词源管理器，选择默认源并启动
- `switchSource(sourceId)`: 切换到指定歌词源
- `getActiveSource()`: 获取当前活跃歌词源
- `stop()`: 停止所有歌词源

### 4.2 超级岛歌词注入

#### 4.2.1 技术原理

通过 Xposed Hook MIUI SystemUI 的超级岛渲染流程，在系统原有视图基础上注入自定义歌词视图。

#### 4.2.2 Hook 点

1. **Application.onCreate**: 初始化歌词环境
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

### 4.3 通知歌词展示

#### 4.3.1 两种通知模式

| 模式 | 说明 | 通知 ID |
|------|------|---------|
| 普通通知 | 标准通知栏歌词显示 | NORMAL_NOTIFICATION_ID |
| 焦点通知 | MIUI 焦点通知（更醒目） | FOCUS_NOTIFICATION_ID |

#### 4.3.2 通知内容结构

- **左侧**: 专辑封面 / 音乐图标
- **右侧上半**: 当前歌词（或歌曲名）
- **右侧下半**: 歌曲信息（歌名 - 歌手）
- **进度条**: 播放进度（可选）

#### 4.3.3 性能优化

1. **状态去重**: 相同状态不重复发送通知
2. **仅进度变化过滤**: 息屏或关闭进度条时不触发
3. **息屏降频**: 屏幕关闭时减少更新频率
4. **暂停防抖**: 暂停 150ms 后才清除通知

#### 4.3.4 焦点通知绕过限制

通过 Shizuku 临时禁用 XMSF 联网，绕过焦点通知的数量限制：

```
1. 闪断 XMSF 联网 (ShizukuManager.setXmsfNetworkingEnabled(false))
2. 发射焦点通知
3. 等待 100ms 盲区
4. 恢复网络
```

### 4.4 AI 歌词翻译

#### 4.4.1 功能特性

- 基于 OpenAI 兼容 API 的歌词翻译
- 内存 + SQLite 双层缓存
- 并发控制（最多 3 个同时翻译）
- 队列管理（最多 5 个等待）
- 切歌时自动取消旧请求
- 可自定义 Prompt、模型、温度等参数

#### 4.4.2 翻译流程

```
translateSongSync(song, configs)
    ↓
检查配置是否可用
    ↓
计算缓存 key (AITranslationKey)
    ↓
┌─ 内存缓存命中？→ 是 → 返回
│       ↓ 否
├─ SQLite 缓存命中？→ 是 → 写入内存 → 返回
│       ↓ 否
└─ 加入翻译队列 (AITranslationScheduler)
        ↓
    调度执行 (并发控制)
        ↓
    OpenAI API 请求
        ↓
    解析响应 (AITranslationResponseParser)
        ↓
    写入缓存（内存 + SQLite）
        ↓
    应用翻译结果 (AITranslationApplicator)
```

#### 4.4.3 默认配置

- 默认模型: `mimo-v2-flash` (小米 Mimo)
- 默认 Base URL: `https://api.xiaomimimo.com/v1/`
- 默认目标语言: 中文
- 最大缓存: 1000 首
- 最大并发: 3 个
- 最大队列: 5 个

### 4.5 在线歌词搜索

#### 4.5.1 支持的歌词源

| 源 | 平台 |
|----|------|
| QmSource | QQ 音乐 |
| NeSource | 网易云音乐 |

#### 4.5.2 技术实现

- 网络框架: Retrofit + OkHttp
- 序列化: Kotlinx Serialization
- 位置: [app/src/online/java/com/lidesheng/hyperlyric/online](file:///workspace/app/src/online/java/com/lidesheng/hyperlyric/online)

#### 4.5.3 核心类

| 类名 | 职责 |
|------|------|
| [LyricApiProvider.kt](file:///workspace/app/src/online/java/com/lidesheng/hyperlyric/online/LyricApiProvider.kt) | 在线歌词 API 提供者 |
| [OnlineLyricTargeter.kt](file:///workspace/app/src/online/java/com/lidesheng/hyperlyric/online/OnlineLyricTargeter.kt) | 在线歌词匹配器 |
| [LrcCacheManager.kt](file:///workspace/app/src/online/java/com/lidesheng/hyperlyric/online/LrcCacheManager.kt) | LRC 缓存管理器 |

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

定义在 [DynamicLyricData.kt](file:///workspace/app/src/main/java/com/lidesheng/hyperlyric/lyric/DynamicLyricData.kt#L36-L57):

全局单例状态容器，使用 Kotlin Flow 驱动响应式更新。

### 5.4 PlaybackAnchor（播放锚点）

定义在 [DynamicLyricData.kt](file:///workspace/app/src/main/java/com/lidesheng/hyperlyric/lyric/DynamicLyricData.kt#L29-L34):

| 字段 | 类型 | 说明 |
|------|------|------|
| position | Long | 播放位置（毫秒） |
| timestamp | Long | 锚点时间戳（elapsedRealtime） |
| speed | Float | 播放速度 |
| isPlaying | Boolean | 是否播放中 |

用于在没有实时进度回调时，根据时间戳推算当前播放位置。

### 5.5 支持的音乐应用

定义在 [DynamicLyricData.kt](file:///workspace/app/src/main/java/com/lidesheng/hyperlyric/lyric/DynamicLyricData.kt#L12-L27):

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
| Shizuku API | 13.1.5 | Shizuku 权限框架 |
| LibXposed API/Service | 101.0.0 | Xposed 框架 |
| SuperLyric API | 3.4 | SuperLyric 歌词 API |
| Lyricon Subscriber | 0.1.70 | Lyricon 歌词 SDK |
| Retrofit | 2.11.0 | 网络请求 |
| OkHttp | 4.12.0 | HTTP 客户端 |
| Kotlinx Serialization | 1.6.3 | JSON 序列化 |
| Kotlinx Coroutines | 1.9.0 | 协程 |
| YoYo Animations | 2.4 | 动画库 |
| YoYo Easing | 2.4 | 缓动函数 |

完整版本定义在 [gradle/libs.versions.toml](file:///workspace/gradle/libs.versions.toml)。

### 6.2 模块间依赖关系

```
ui → service → lyric → common
              ↓
            root (Xposed 模块，独立进程)

online flavor → lyric (在线歌词实现)
offline flavor → lyric (离线歌词实现)
```

### 6.3 AIDL 接口

路径: [app/src/main/aidl/com/lidesheng/hyperlyric](file:///workspace/app/src/main/aidl/com/lidesheng/hyperlyric)

| 接口 | 用途 |
|------|------|
| IPrivilegedService.aidl | 特权服务接口 |
| IPrivilegedLogCallback.aidl | 特权日志回调 |
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
# 构建 online 版本（含在线歌词功能）
./gradlew assembleOnlineRelease

# 构建 offline 版本（仅本地歌词）
./gradlew assembleOfflineRelease

# 构建所有变体
./gradlew assembleRelease
```

### 7.3 安装与使用

1. **安装 APK**: 安装构建生成的 APK
2. **激活 Xposed 模块**: 在 LSPosed 等框架中激活 HyperLyric，作用域为系统界面（SystemUI）
3. **通知权限**: 授予通知监听权限
4. **配置应用**: 打开 App 进行各项配置
5. **重启 SystemUI**: 重启系统界面以激活 Hook

### 7.4 Xposed 模块配置

模块配置文件位于 [app/src/main/resources/META-INF/xposed](file:///workspace/app/src/main/resources/META-INF/xposed):

- [module.prop](file:///workspace/app/src/main/resources/META-INF/xposed/module.prop): 模块属性
- [java_init.list](file:///workspace/app/src/main/resources/META-INF/xposed/java_init.list): Java 初始化类列表
- [scope.list](file:///workspace/app/src/main/resources/META-INF/xposed/scope.list): 作用域列表

### 7.5 CI/CD

GitHub Actions 配置位于 [.github/workflows/android_ci.yml](file:///workspace/.github/workflows/android_ci.yml)

---

## 8. 产品 Flavor

### 8.1 Flavor 维度

- 维度名: `version`

### 8.2 Online Flavor

- **构建变体**: online
- **特性**: 包含在线歌词搜索功能
- **额外依赖**: Retrofit, OkHttp
- **源码路径**: [app/src/online](file:///workspace/app/src/online)
- **BuildConfig**: `ONLINE_FEATURES_ENABLED = true`

### 8.3 Offline Flavor

- **构建变体**: offline
- **特性**: 仅本地歌词功能，无网络依赖
- **源码路径**: [app/src/offline](file:///workspace/app/src/offline)
- **BuildConfig**: `ONLINE_FEATURES_ENABLED = false`

### 8.4 Flavor 特有类

两个 flavor 都提供了各自的实现类：

| 类 | Online 实现 | Offline 实现 |
|----|------------|-------------|
| LyricProviderImpl | 含在线歌词搜索 | 仅本地歌词 |
| LicenseProvider | 在线版许可证 | 离线版许可证 |

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
- **日志级别**: 可配置，默认级别 0

### C. 备份与恢复

- [BackupRestoreManager.kt](file:///workspace/app/src/main/java/com/lidesheng/hyperlyric/utils/BackupRestoreManager.kt): 配置备份恢复管理
- 支持 Android 自动备份（[backup_rules.xml](file:///workspace/app/src/main/res/xml/backup_rules.xml)）

---

*文档版本: 1.0*  
*生成日期: 2026-07-13*  
*基于代码版本: v1.00 (versionCode 100)*
