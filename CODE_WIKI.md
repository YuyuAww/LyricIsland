# HyperLyric Code Wiki

## 目录

- [1. 项目概述](#1-项目概述)
- [2. 项目架构](#2-项目架构)
  - [2.1 整体架构图](#21-整体架构图)
  - [2.2 包结构说明](#22-包结构说明)
- [3. 核心模块详解](#3-核心模块详解)
  - [3.1 common 公共模块](#31-common-公共模块)
  - [3.2 lyric 歌词模块](#32-lyric-歌词模块)
  - [3.3 root Hook 模块](#33-root-hook-模块)
  - [3.4 ui 界面模块](#34-ui-界面模块)
- [4. 核心功能详解](#4-核心功能详解)
  - [4.1 歌词源管理](#41-歌词源管理)
  - [4.2 超级岛歌词注入](#42-超级岛歌词注入)
- [5. 关键数据结构](#5-关键数据结构)
  - [5.1 歌词接口体系](#51-歌词接口体系)
  - [5.2 核心模型](#52-核心模型)
  - [5.3 运行时状态](#53-运行时状态)
- [6. 依赖关系](#6-依赖关系)
- [7. 项目构建与运行](#7-项目构建与运行)

---

## 1. 项目概述

**项目名称**: HyperLyric (音乐岛 / LyricIsland)

**项目类型**: Android Xposed 歌词显示模块

**核心功能**:
- 通过 Xposed Hook 在 MIUI 超级岛（灵动岛）中实时显示歌词
- 集成 Lyricon 歌词订阅 SDK，获取多源实时歌词
- 支持逐字歌词、翻译、罗马音、渐变色进度、单词动画等高级效果
- 丰富的样式自定义与配置选项

**技术栈**:
- 语言: Kotlin
- UI 框架: Jetpack Compose + MIUI X (miuix) 组件库
- Hook 框架: LibXposed (API 101)
- 序列化: Kotlinx Serialization JSON
- 协程: Kotlin Coroutines
- 构建: Gradle 9.2.1 / AGP 9.2.1

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
│  接口体系 / 数据模型 / SourceManager / DynamicData         │
│  TimingNavigator / SongPreprocessor / InterludeTracker     │
└────────────────────────────┬────────────────────────────────┘
                             │
          ┌──────────────────┴──────────────────┐
          │                                     │
┌─────────▼─────────┐               ┌───────────▼─────────────┐
│  Root Hook 层     │               │   公共层 (common)        │
│  (Xposed 模块)    │               │   工具类/常量/解析器     │
│  HookEntry        │               └─────────────────────────┘
│  IslandTextHooker │
│  BaseIslandRenderer│
│  LyriconSource    │
└───────────────────┘
```

### 2.2 包结构说明

```
com.lidesheng.hyperlyric
├── common/                    # 公共工具与常量
│   ├── color/                 # 专辑封面颜色提取
│   ├── extensions/            # Kotlin 扩展函数
│   ├── image/                 # 图片处理
│   ├── media/                 # 媒体元数据获取
│   ├── HyperLogger.kt         # 日志接口抽象
│   ├── PreferenceKeys.kt      # SharedPreferences 键名
│   ├── PrefsBridge.kt         # 配置桥接（App ↔ Root 进程）
│   ├── RootConstants.kt       # Root 侧配置常量
│   └── UIConstants.kt         # UI 侧常量
├── lyric/                     # 歌词核心模块
│   ├── model/                 # 数据模型
│   │   ├── interfaces/        # 歌词接口定义
│   │   ├── LyricLine.kt       # 基础歌词行
│   │   ├── RichLyricLine.kt   # 富歌词行
│   │   ├── LyricWord.kt       # 歌词单词
│   │   ├── LyricMetadata.kt   # 元数据容器
│   │   ├── LyricTiming.kt     # 时间信息
│   │   └── Song.kt            # 歌曲聚合模型
│   ├── source/                # 歌词源接口与管理
│   ├── view/                  # 歌词视图与渲染组件
│   │   ├── line/model/        # 视图层内部模型
│   │   ├── RichLyricLineView.kt        # 歌词行自定义 View
│   │   ├── SpaceGateRichLyricLineView.kt
│   │   ├── SongPreprocessor.kt         # 歌曲预处理
│   │   ├── InterludeTracker.kt         # 间奏追踪器
│   │   └── ...
│   └── DynamicLyricData.kt    # 全局歌词状态（StateFlow）
├── root/                      # Xposed Hook 模块
│   ├── bridge/                # IPC 桥接（AIDL / IpcRouter）
│   ├── island/                # 超级岛注入核心
│   │   ├── renderer/          # 渲染器
│   │   ├── IslandTextHooker.kt          # Hook 安装器
│   │   ├── IslandSlotContentAssembler.kt # 内容装配器
│   │   ├── IslandViewRegistry.kt         # 注入视图注册表
│   │   ├── IslandProbeUtils.kt           # 岛探测工具
│   │   ├── RealIslandHooker.kt           # 真实岛 Hook
│   │   ├── IslandWidthHooker.kt          # 宽度 Hook
│   │   ├── FakeIslandTransitionHooker.kt # 假视图过渡
│   │   ├── IslandModuleRestoreHooker.kt  # 模块恢复
│   │   └── SystemUIHookRegistry.kt       # Hook 注册中心
│   ├── source/                # Root 进程歌词源
│   │   ├── LyriconSource.kt   # Lyricon SDK 接入
│   │   └── RootLyricSink.kt   # 歌词接收器
│   ├── utils/                 # Hook 工具
│   │   ├── HookLogger.kt      # Root 侧日志
│   │   ├── CoverColorHelper.kt# 封面颜色助手
│   │   ├── LyricStyleHelper.kt# 歌词样式助手
│   │   └── ...
│   ├── HookEntry.kt           # Xposed 入口
│   ├── LyriconDataBridge.kt   # 全局数据桥接
│   ├── RootApplication.kt     # Root 侧 Application
│   ├── UnlockFocusWhitelist.kt
│   └── UnlockIslandWhitelist.kt
├── ui/                        # 用户界面（Compose）
│   ├── component/             # 可复用组件
│   ├── navigation/            # 导航体系
│   ├── page/                  # 页面
│   └── utils/                 # UI 工具
└── utils/                     # 应用级工具
    ├── BackupRestoreManager.kt
    ├── LogManager.kt
    └── ...
```

---

## 3. 核心模块详解

### 3.1 common 公共模块

**路径**: [app/src/main/java/com/lidesheng/hyperlyric/common](file:///workspace/app/src/main/java/com/lidesheng/hyperlyric/common)

#### 3.1.1 核心常量类

| 类名 | 职责 |
|------|------|
| [RootConstants.kt](file:///workspace/app/src/main/java/com/lidesheng/hyperlyric/common/RootConstants.kt) | Root Hook 相关的配置键名与默认值，涵盖超级岛开关、样式、动画、翻译、白名单解锁等数十项配置 |
| [PreferenceKeys.kt](file:///workspace/app/src/main/java/com/lidesheng/hyperlyric/common/PreferenceKeys.kt) | SharedPreferences 名称与日志级别配置 |
| [UIConstants.kt](file:///workspace/app/src/main/java/com/lidesheng/hyperlyric/common/UIConstants.kt) | UI 相关常量 |

#### 3.1.2 核心工具类

| 类名 | 职责 |
|------|------|
| [PrefsBridge.kt](file:///workspace/app/src/main/java/com/lidesheng/hyperlyric/common/PrefsBridge.kt) | SharedPreferences 桥接，支持 App 进程写入配置后同步到 Xposed 远程进程 |
| [HyperLogger.kt](file:///workspace/app/src/main/java/com/lidesheng/hyperlyric/common/HyperLogger.kt) | 日志接口抽象，统一日志输出规范，区分 App 进程与 Root 进程实现 |

#### 3.1.3 媒体与图片工具

| 类名 | 职责 |
|------|------|
| [ColorExtractor.kt](file:///workspace/app/src/main/java/com/lidesheng/hyperlyric/common/color/ColorExtractor.kt) | 专辑封面主色与渐变色提取（基于 Palette） |
| [AlbumImageHelper.kt](file:///workspace/app/src/main/java/com/lidesheng/hyperlyric/common/image/AlbumImageHelper.kt) | 专辑图片加载、缩放、缓存处理 |
| [MediaMetadataHelper.kt](file:///workspace/app/src/main/java/com/lidesheng/hyperlyric/common/media/MediaMetadataHelper.kt) | 通过 MediaSession 获取当前播放媒体的元数据（歌名、歌手、专辑、封面） |

---

### 3.2 lyric 歌词模块

**路径**: [app/src/main/java/com/lidesheng/hyperlyric/lyric](file:///workspace/app/src/main/java/com/lidesheng/hyperlyric/lyric)

#### 3.2.1 数据模型接口体系 (model/interfaces)

歌词模型采用**分层接口设计**，职责清晰，便于扩展：

| 接口 | 继承关系 | 职责 |
|------|---------|------|
| [ILyricTiming](file:///workspace/app/src/main/java/com/lidesheng/hyperlyric/lyric/model/interfaces/ILyricTiming.kt) | 根接口 | 定义时间属性：`begin`、`end`、`duration`（计算属性 `end - begin`） |
| [ILyricWord](file:///workspace/app/src/main/java/com/lidesheng/hyperlyric/lyric/model/interfaces/ILyricWord.kt) | ← ILyricTiming | 定义单词属性：`text`、`metadata` |
| [ILyricLine](file:///workspace/app/src/main/java/com/lidesheng/hyperlyric/lyric/model/interfaces/ILyricLine.kt) | ← ILyricTiming | 定义歌词行属性：`isAlignedRight`、`metadata`、`text`、`words` |
| [IRichLyricLine](file:///workspace/app/src/main/java/com/lidesheng/hyperlyric/lyric/model/interfaces/IRichLyricLine.kt) | ← ILyricLine | 扩展多语言属性：`secondary`、`translation`、`roma` 及对应逐字列表 |
| [DeepCopyable](file:///workspace/app/src/main/java/com/lidesheng/hyperlyric/lyric/model/interfaces/DeepCopyable.kt) | 独立 | 深拷贝能力接口 |
| [Normalize](file:///workspace/app/src/main/java/com/lidesheng/hyperlyric/lyric/model/interfaces/Normalize.kt) | 独立 | 数据规范化接口（排序、去重、填充 text） |

#### 3.2.2 数据模型实现 (model)

| 类名 | 实现接口 | 职责 |
|------|---------|------|
| [LyricWord](file:///workspace/app/src/main/java/com/lidesheng/hyperlyric/lyric/model/LyricWord.kt) | ILyricWord, DeepCopyable | 歌词单词，最细粒度时间同步单元 |
| [LyricLine](file:///workspace/app/src/main/java/com/lidesheng/hyperlyric/lyric/model/LyricLine.kt) | ILyricLine, DeepCopyable, Normalize | 基础歌词行（text + words） |
| [RichLyricLine](file:///workspace/app/src/main/java/com/lidesheng/hyperlyric/lyric/model/RichLyricLine.kt) | IRichLyricLine, DeepCopyable, Normalize | 富歌词行，支持翻译、罗马音、次要文本 |
| [LyricMetadata](file:///workspace/app/src/main/java/com/lidesheng/hyperlyric/lyric/model/LyricMetadata.kt) | Map<String, String?> by delegate | 基于委托的元数据容器，支持类型安全读取（getBoolean / getInt / getLong / getFloat / getDouble / getString） |
| [LyricTiming](file:///workspace/app/src/main/java/com/lidesheng/hyperlyric/lyric/model/LyricTiming.kt) | ILyricTiming | 独立的时间信息数据类 |
| [Song](file:///workspace/app/src/main/java/com/lidesheng/hyperlyric/lyric/model/Song.kt) | DeepCopyable, Normalize | 歌曲聚合模型，包含歌词列表（`List<RichLyricLine>`） |

> **重要变更**: `duration` 已从所有持久化模型中移除，改为 `ILyricTiming` 接口上的**计算属性** `val duration get() = end - begin`，与 Lyricon 官方标准对齐。

#### 3.2.3 歌词源管理 (source)

| 类名 | 职责 |
|------|------|
| [LyricSource.kt](file:///workspace/app/src/main/java/com/lidesheng/hyperlyric/lyric/source/LyricSource.kt) | 歌词源接口，定义 `start()` / `stop()` / `initialize()` 生命周期 |
| [LyricSink.kt](file:///workspace/app/src/main/java/com/lidesheng/hyperlyric/lyric/source/LyricSink.kt) | 歌词接收器接口，定义歌曲变更、歌词行、位置、播放状态等回调 |
| [SourceManager.kt](file:///workspace/app/src/main/java/com/lidesheng/hyperlyric/lyric/source/SourceManager.kt) | 歌词源管理器，管理多源切换、生命周期、配置持久化 |
| [StateResetter.kt](file:///workspace/app/src/main/java/com/lidesheng/hyperlyric/lyric/source/StateResetter.kt) | 状态重置接口，用于播放停止时清理运行时状态 |

#### 3.2.4 动态数据与视图

| 类名 | 职责 |
|------|------|
| [DynamicLyricData.kt](file:///workspace/app/src/main/java/com/lidesheng/hyperlyric/lyric/DynamicLyricData.kt) | 全局歌词状态容器（`LyricState` + `PlaybackAnchor`），使用 `StateFlow` 驱动 UI 响应式更新 |
| [SongPreprocessor.kt](file:///workspace/app/src/main/java/com/lidesheng/hyperlyric/lyric/view/SongPreprocessor.kt) | 歌曲预处理：将 `RichLyricLine` 转换为视图可用的 `TimedLine`，处理标题行插入 |
| [InterludeTracker.kt](file:///workspace/app/src/main/java/com/lidesheng/hyperlyric/lyric/view/InterludeTracker.kt) | 间奏追踪器，在歌词间隙保持显示最后一行，避免回退到歌名 |
| [RichLyricLineView.kt](file:///workspace/app/src/main/java/com/lidesheng/hyperlyric/lyric/view/RichLyricLineView.kt) | 核心歌词视图，继承 `LinearLayout`，支持主/副双行、跑马灯、渐变色、单词动画 |
| [SpaceGateRichLyricLineView.kt](file:///workspace/app/src/main/java/com/lidesheng/hyperlyric/lyric/view/SpaceGateRichLyricLineView.kt) | 空间门歌词视图变体，用于特定岛样式 |

---

### 3.3 root Hook 模块

**路径**: [app/src/main/java/com/lidesheng/hyperlyric/root](file:///workspace/app/src/main/java/com/lidesheng/hyperlyric/root)

#### 3.3.1 Hook 入口

| 类名 | 职责 |
|------|------|
| [HookEntry.kt](file:///workspace/app/src/main/java/com/lidesheng/hyperlyric/root/HookEntry.kt) | Xposed 模块主入口，继承 `XposedModule`。负责注入 SystemUI 和 miui.systemui.plugin |
| [RootApplication.kt](file:///workspace/app/src/main/java/com/lidesheng/hyperlyric/root/RootApplication.kt) | Root 侧 Application，负责 Xposed 服务绑定与配置同步 |

**HookEntry 核心流程**:

1. **`onModuleLoaded`**: 模块加载时初始化单例、日志器
2. **`onPackageLoaded`**:
   - 针对 `com.android.systemui`:
     - 注入白名单解锁（`UnlockIslandWhitelist`、`UnlockFocusWhitelist`）
     - Hook `Application.onCreate`（`AppCreateHooker`）：初始化歌词源、渲染器、配置监听
     - Hook `BaseDexClassLoader` 构造（`ClassLoaderHooker`）：捕获动态加载的插件
   - 针对 `miui.systemui.plugin`: 调用 `SystemUIHookRegistry.hook()`
3. **配置变更监听**: `SUPER_ISLAND_RUNTIME_REFRESH_KEYS` 中 40+ 项配置变更时自动刷新岛视图

#### 3.3.2 数据桥接 (LyriconDataBridge)

[LyriconDataBridge.kt](file:///workspace/app/src/main/java/com/lidesheng/hyperlyric/root/LyriconDataBridge.kt) 是**全局歌词数据桥接器**，作为 Root 进程内的数据共享中心：

- 维护当前歌曲、歌词行、播放位置等运行时状态（全部 `@Volatile`）
- 使用 [TimingNavigator](file:///workspace/app/src/main/java/com/lidesheng/hyperlyric/lyric/model/extensions/TimingNavigator.kt) 高效定位当前歌词行（二分查找优化）
- 支持纯文本模式（`isTextMode`，如椒盐音乐通过 `onSendText` 推送）
- 间奏处理：通过 `InterludeTracker` 在歌词间隙保持最后一行显示

#### 3.3.3 超级岛渲染层 (island)

**渲染器**:

| 类名 | 职责 |
|------|------|
| [BaseIslandRenderer](file:///workspace/app/src/main/java/com/lidesheng/hyperlyric/root/island/renderer/BaseIslandRenderer.kt) | 核心渲染器，实现 `IslandRenderer` 接口。负责刷新、更新歌词行、更新播放位置、处理播放状态变化 |
| [IslandRenderer.kt](file:///workspace/app/src/main/java/com/lidesheng/hyperlyric/root/island/renderer/IslandRenderer.kt) | 渲染器接口定义 |

**BaseIslandRenderer 关键行为**:
- **`refreshActiveIsland()`**: 带 32ms 防抖的岛刷新，遍历 `IslandViewRegistry` 中所有活动视图
- **`updateLyricLine()`**: 仅更新歌词内容（不触发完整刷新）
- **`updatePosition(position)`**: 将播放位置分发给所有注入的歌词视图（`RichLyricLineView.setPosition()`）
- **`onPlaybackStateChanged(isPlaying)`**: 根据暂停行为配置（保留歌词 / 恢复原生岛）执行不同逻辑

**内容装配器**:

| 类名 | 职责 |
|------|------|
| [IslandSlotContentAssembler.kt](file:///workspace/app/src/main/java/com/lidesheng/hyperlyric/root/island/IslandSlotContentAssembler.kt) | 超级岛槽位内容装配器。根据模式应用不同内容，支持签名缓存避免重复渲染 |

**IslandSlotContentAssembler 内容模式**:

| mode | 显示内容 | 说明 |
|------|---------|------|
| 1 | 歌名 | 单行 |
| 2 | 歌手 | 单行 |
| 3 | 专辑 | 单行 |
| 4 | 歌名 - 歌手 | 单行拼接 |
| 5 | 歌名 / 歌手（双行） | 上行歌名，下行歌手 |
| 6 | 歌名 / 歌手-专辑（双行） | 上行歌名，下行歌手-专辑 |
| 7 | 实时歌词 | 从 `LyriconDataBridge` 获取当前歌词行 |
| 0 | 禁用 | 不显示任何内容 |

**Hook 安装器**:

| 类名 | 职责 |
|------|------|
| [IslandTextHooker.kt](file:///workspace/app/src/main/java/com/lidesheng/hyperlyric/root/island/IslandTextHooker.kt) | 统一安装所有超级岛相关 Hook，分为三大功能组：真实岛、Fake View 过渡、模块恢复 |
| [SystemUIHookRegistry.kt](file:///workspace/app/src/main/java/com/lidesheng/hyperlyric/root/island/SystemUIHookRegistry.kt) | Hook 注册中心，防止重复 Hook，负责加载 `miui.systemui.dynamicisland` 相关类 |

**IslandTextHooker 安装的 Hook 点**:

| 功能组 | Hook 目标 | 用途 |
|--------|----------|------|
| 真实岛 | `DynamicIslandContentView.updateBigIslandView()` | 拦截大岛更新，注入歌词槽位 |
| 真实岛 | `DynamicIslandContentView.calculateBigIslandWidth()` | 修改岛宽度以容纳歌词 |
| 真实岛 | `hideIslandLayout` / `showIslandLayout` | 控制岛可见性，恢复歌词视图 |
| Fake View 过渡 | `DynamicIslandContentFakeView.onTrackingFakeViewStart()` | 处理假视图拖拽过渡 |
| Fake View 过渡 | `updateViewStateWhenOpenAnimStart` | 展开动画开始时准备视图 |
| Fake View 过渡 | `setVisibility` | 可见性变化时同步歌词视图 |
| 模块恢复 | `IslandTemplateBuilder.updateModuleView()` | 系统恢复模块视图时保留歌词注入 |
| 模块恢复 | `IslandModuleViewHolderAdapter.updateView()` | Adapter 更新时恢复歌词 |

#### 3.3.4 Root 进程歌词源 (source)

| 类名 | 职责 |
|------|------|
| [LyriconSource.kt](file:///workspace/app/src/main/java/com/lidesheng/hyperlyric/root/source/LyriconSource.kt) | Lyricon 歌词源，通过 `Lyricon Subscriber SDK (0.1.70)` 订阅歌词事件。监听 `ActivePlayerListener` 获取歌曲变更、播放状态、歌词行等 |
| [RootLyricSink.kt](file:///workspace/app/src/main/java/com/lidesheng/hyperlyric/root/source/RootLyricSink.kt) | Root 进程歌词接收器，实现 `LyricSink`。将歌词事件转发给 `LyriconDataBridge` 和 `BaseIslandRenderer`。位置更新做了节流处理（最小 33ms 间隔） |

#### 3.3.5 白名单解锁

| 类名 | 职责 |
|------|------|
| [UnlockIslandWhitelist.kt](file:///workspace/app/src/main/java/com/lidesheng/hyperlyric/root/UnlockIslandWhitelist.kt) | 解锁超级岛下拉小窗白名单，允许非系统应用显示媒体岛 |
| [UnlockFocusWhitelist.kt](file:///workspace/app/src/main/java/com/lidesheng/hyperlyric/root/UnlockFocusWhitelist.kt) | 解锁焦点通知白名单 |

---

### 3.4 ui 界面模块

**路径**: [app/src/main/java/com/lidesheng/hyperlyric/ui](file:///workspace/app/src/main/java/com/lidesheng/hyperlyric/ui)

#### 3.4.1 主入口

| 类名 | 职责 |
|------|------|
| [MainActivity.kt](file:///workspace/app/src/main/java/com/lidesheng/hyperlyric/ui/MainActivity.kt) | 主 Activity，Jetpack Compose 入口，集成 miuix 主题 |

#### 3.4.2 导航 (navigation)

| 类名 | 职责 |
|------|------|
| [AppNavigation.kt](file:///workspace/app/src/main/java/com/lidesheng/hyperlyric/ui/navigation/AppNavigation.kt) | 应用导航图，定义所有页面路由与过渡动画 |
| [Navigator.kt](file:///workspace/app/src/main/java/com/lidesheng/hyperlyric/ui/navigation/Navigator.kt) | 导航器封装，管理返回栈 |
| [Route.kt](file:///workspace/app/src/main/java/com/lidesheng/hyperlyric/ui/navigation/Route.kt) | 路由定义（sealed class），类型安全导航 |

#### 3.4.3 页面 (page)

| 页面 | 职责 |
|------|------|
| [MainPage.kt](file:///workspace/app/src/main/java/com/lidesheng/hyperlyric/ui/page/MainPage.kt) | 主页面，展示当前歌词预览、快捷设置入口 |
| [SetupPage.kt](file:///workspace/app/src/main/java/com/lidesheng/hyperlyric/ui/page/SetupPage.kt) | 首次引导设置页，检测 Xposed 框架激活状态 |
| [HookSettingsPage.kt](file:///workspace/app/src/main/java/com/lidesheng/hyperlyric/ui/page/HookSettingsPage.kt) | Hook 模块设置页（超级岛开关、内容模式、样式等） |
| [SettingsPage.kt](file:///workspace/app/src/main/java/com/lidesheng/hyperlyric/ui/page/SettingsPage.kt) | 通用设置页 |
| [LogPage.kt](file:///workspace/app/src/main/java/com/lidesheng/hyperlyric/ui/page/LogPage.kt) | 日志查看页，支持实时滚动与导出 |
| [HelpPage.kt](file:///workspace/app/src/main/java/com/lidesheng/hyperlyric/ui/page/HelpPage.kt) | 帮助与 FAQ 页面 |
| [ChangelogPage.kt](file:///workspace/app/src/main/java/com/lidesheng/hyperlyric/ui/page/ChangelogPage.kt) | 更新日志页 |
| [LicensesPage.kt](file:///workspace/app/src/main/java/com/lidesheng/hyperlyric/ui/page/LicensesPage.kt) | 开源许可证页 |

---

## 4. 核心功能详解

### 4.1 歌词源管理

#### 4.1.1 架构设计

系统采用**源-接收器-数据桥**三层模式：

```
Lyricon SDK → LyriconSource → SourceManager → RootLyricSink
                                              │
                                              ▼
                                       LyriconDataBridge
                                              │
                        ┌─────────────────────┼─────────────────────┐
                        ▼                     ▼                     ▼
                 BaseIslandRenderer      DynamicLyricData      UI Settings
                        │                     │
                        ▼                     ▼
              IslandSlotContentAssembler   Compose UI
                        │
                        ▼
                RichLyricLineView
                        │
                        ▼
                超级岛 (Dynamic Island)
```

#### 4.1.2 数据流转

1. **歌曲变更**: `LyriconSource` 收到歌曲 → `SourceManager` 分发 → `RootLyricSink.onSongChanged()` → `LyriconDataBridge.updateSong()` → 初始化 `TimingNavigator` 和 `SongPreprocessor`
2. **歌词行变更**: `LyriconSource` 收到歌词行 → `RootLyricSink.onLyricLine()` → `LyriconDataBridge.currentLyricLine` 更新 → `BaseIslandRenderer.updateLyricLine()`
3. **位置更新**: `LyriconSource` 收到位置 → `RootLyricSink.onPosition()`（节流 33ms）→ `LyriconDataBridge.updatePosition()`（使用 `TimingNavigator` 定位行）→ `BaseIslandRenderer.updatePosition()` → 各 `RichLyricLineView.setPosition()`
4. **播放状态**: `LyriconSource` 收到状态 → `RootLyricSink.onPlaybackStateChanged()` → `BaseIslandRenderer.onPlaybackStateChanged()` → 根据配置保留或清除注入视图

### 4.2 超级岛歌词注入

#### 4.2.1 技术原理

通过 Xposed Hook MIUI SystemUI 的超级岛渲染流程，在系统原有视图树中注入自定义歌词视图。

#### 4.2.2 完整注入流程

```
SystemUI 更新超级岛
    ↓
RealIslandHooker.UpdateBigIslandViewHook.intercept()
    ↓
前置处理：轻量恢复已注入的歌词视图（IslandModuleRestoreHooker）
    ↓
调用原方法（系统正常渲染媒体岛）
    ↓
后置处理：
  1. 检查超级岛总开关
  2. IslandProbeUtils.extractMediaIslandInfo() 提取媒体岛信息
  3. IslandTextHookerSupport.isCurrentLyricIsland() 判断是否为当前播放应用
  4. IslandLyricTextInjector.injectSlots() 注入歌词槽位视图（如未注入）
  5. BaseIslandRenderer.updateContentForView() 刷新歌词内容
  6. IslandHostFacade.updateHostGlow() 注入光晕效果
```

#### 4.2.3 视图生命周期管理

- **注册**: `IslandViewRegistry` 使用 `WeakHashMap` 持有注入的视图，避免内存泄漏
- **恢复**: 当系统回收或重建超级岛视图时，`IslandModuleRestoreHooker` 确保歌词视图被重新注入
- **清除**: `BaseIslandRenderer.clearAllViews()` 遍历注册表清除所有注入视图并触发系统重新布局

---

## 5. 关键数据结构

### 5.1 歌词接口体系

```
ILyricTiming (时间基接口)
    ├── begin: Long
    ├── end: Long
    └── duration: Long  [计算属性: end - begin]
    │
    ├──► ILyricWord (歌词单词接口)
    │       ├── text: String?
    │       └── metadata: LyricMetadata?
    │       └──► LyricWord (实现类)
    │
    └──► ILyricLine (歌词行接口)
            ├── isAlignedRight: Boolean
            ├── metadata: LyricMetadata?
            ├── text: String?
            ├── words: List<LyricWord>?
            │
            ├──► IRichLyricLine (富歌词行接口)
            │       ├── secondary: String?
            │       ├── secondaryWords: List<LyricWord>?
            │       ├── translation: String?
            │       ├── translationWords: List<LyricWord>?
            │       ├── roma: String?
            │       └──► RichLyricLine (实现类)
            │
            └──► LyricLine (基础实现类，仅 text + words)
```

### 5.2 核心模型

#### 5.2.1 LyricWord（歌词单词）

定义在 [LyricWord.kt](file:///workspace/app/src/main/java/com/lidesheng/hyperlyric/lyric/model/LyricWord.kt):

| 字段 | 类型 | 说明 |
|------|------|------|
| begin | Long | 单词开始时间（毫秒） |
| end | Long | 单词结束时间（毫秒） |
| text | String? | 单词文本 |
| metadata | LyricMetadata? | 单词级元数据 |

> `duration` 通过 `ILyricTiming` 接口计算：`end - begin`

#### 5.2.2 LyricLine（基础歌词行）

定义在 [LyricLine.kt](file:///workspace/app/src/main/java/com/lidesheng/hyperlyric/lyric/model/LyricLine.kt):

| 字段 | 类型 | 说明 |
|------|------|------|
| begin | Long | 行开始时间（毫秒） |
| end | Long | 行结束时间（毫秒） |
| isAlignedRight | Boolean | 是否右对齐 |
| metadata | LyricMetadata? | 行元数据 |
| text | String? | 行文本 |
| words | List<LyricWord>? | 逐字歌词列表 |

**Normalize 行为**: 若 `words` 非空，自动将 `text` 拼接为 `words.joinToString("") { it.text }`

#### 5.2.3 RichLyricLine（富歌词行）

定义在 [RichLyricLine.kt](file:///workspace/app/src/main/java/com/lidesheng/hyperlyric/lyric/model/RichLyricLine.kt):

| 字段 | 类型 | 说明 |
|------|------|------|
| begin | Long | 行开始时间 |
| end | Long | 行结束时间 |
| isAlignedRight | Boolean | 是否右对齐 |
| metadata | LyricMetadata? | 行元数据 |
| text | String? | **主文本**（如歌词原文） |
| words | List<LyricWord>? | 主文本逐字时间 |
| secondary | String? | **次要文本**（如歌手名） |
| secondaryWords | List<LyricWord>? | 次要文本逐字时间 |
| translation | String? | **翻译文本** |
| translationWords | List<LyricWord>? | 翻译逐字时间 |
| roma | String? | **罗马音**（日语音标） |

**Normalize 行为**: 对 `words`、`secondaryWords`、`translationWords` 分别执行 normalize，并回填对应的 `text`、`secondary`、`translation`

#### 5.2.4 LyricMetadata（歌词元数据）

定义在 [LyricMetadata.kt](file:///workspace/app/src/main/java/com/lidesheng/hyperlyric/lyric/model/LyricMetadata.kt):

基于 `Map<String, String?>` 委托实现，提供类型安全访问：

| 方法 | 返回值 | 默认值 |
|------|--------|--------|
| `getBoolean(key, default)` | Boolean | false |
| `getInt(key, default)` | Int | 0 |
| `getLong(key, default)` | Long | 0 |
| `getFloat(key, default)` | Float | 0f |
| `getDouble(key, default)` | Double | 0.0 |
| `getString(key, default)` | String? | null |

构建函数：`lyricMetadataOf("TitleLine" to "true", ...)`

#### 5.2.5 Song（歌曲）

定义在 [Song.kt](file:///workspace/app/src/main/java/com/lidesheng/hyperlyric/lyric/model/Song.kt):

| 字段 | 类型 | 说明 |
|------|------|------|
| id | String? | 歌曲 ID |
| name | String? | 歌曲名 |
| artist | String? | 艺术家 |
| duration | Long | 歌曲总时长（毫秒） |
| metadata | LyricMetadata? | 歌曲级元数据 |
| lyrics | List<RichLyricLine>? | 歌词列表 |

**Normalize 行为**: 过滤无效歌词行（`begin < 0` 或 `begin >= end` 或 `text.isNullOrBlank()`），并按时间排序。

### 5.3 运行时状态

#### 5.3.1 LyricState

定义在 [DynamicLyricData.kt](file:///workspace/app/src/main/java/com/lidesheng/hyperlyric/lyric/DynamicLyricData.kt):

```kotlin
data class LyricState(
    val islandTitleLeft: String = "等待播放...",     // 岛左标题
    val islandTitleRight: String = "HyperLyric",     // 岛右标题
    val notificationTitleLeft: String = "",          // 通知左标题
    val notificationTitleRight: String = "",         // 通知右标题
    val songLyric: String = "",                      // 当前歌词文本
    val songInfo: String = "",                       // 歌曲信息
    val showIslandLeftAlbum: Boolean = false,        // 是否显示专辑图
    val duration: Long = 100L,                       // 歌曲时长
    val isPlaying: Boolean = false,                  // 播放状态
    val targetPackageName: String = "",              // 目标包名
    val albumColor: Int = Color.BLACK,               // 专辑主色
    val albumColorEnd: Int = Color.BLACK,            // 专辑辅色
    val albumBitmap: Bitmap? = null,                 // 专辑封面
    val playbackAnchor: PlaybackAnchor = PlaybackAnchor()  // 播放锚点
)
```

#### 5.3.2 PlaybackAnchor（播放锚点）

| 字段 | 类型 | 说明 |
|------|------|------|
| position | Long | 播放位置（毫秒） |
| timestamp | Long | 锚点时间戳（`elapsedRealtime`） |
| speed | Float | 播放速度 |
| isPlaying | Boolean | 是否播放中 |

用于计算经过时间后的当前进度：`position + (now - timestamp) * speed`

#### 5.3.3 支持的音乐应用

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
| Compose UI / Foundation / Material3 | - | Compose UI 核心与 Material 3 |
| MIUI X (miuix) | 0.9.1 | MIUI 风格组件库（SuperDialog、SuperSwitch 等） |
| Navigation3 | 1.1.0 | Compose 导航 |
| Palette KTX | 1.0.0 | 专辑封面颜色提取 |
| HiddenApiBypass | 4.3 | 绕过 Android 隐藏 API 限制 |
| LibXposed API / Service | 101.0.0 | Xposed Framework API |
| Lyricon Subscriber | 0.1.70 | Lyricon 歌词订阅 SDK |
| Kotlinx Serialization JSON | 1.6.3 | JSON 序列化 |
| Kotlinx Coroutines | 1.9.0 | 协程支持 |
| YoYo Animations / Easing | 2.4 | 歌词切换动画库 |

完整版本定义在 [gradle/libs.versions.toml](file:///workspace/gradle/libs.versions.toml)。

### 6.2 模块间依赖关系

```
ui → lyric → common
              ↓
            root (Xposed 模块，独立进程)
```

`root` 模块通过 `common` 中的常量与工具类共享配置定义，但在运行时运行于独立的 SystemUI 进程中。

### 6.3 AIDL 接口

路径: [app/src/main/aidl/com/lidesheng/hyperlyric](file:///workspace/app/src/main/aidl/com/lidesheng/hyperlyric)

| 接口 | 用途 |
|------|------|
| [IBridgeCallback.aidl](file:///workspace/app/src/main/aidl/com/lidesheng/hyperlyric/root/bridge/IBridgeCallback.aidl) | App 进程与 Root 进程之间的桥接回调 |
| [Song.aidl](file:///workspace/app/src/main/aidl/com/lidesheng/hyperlyric/lyric/model/Song.aidl) | 歌曲数据跨进程传输 |

---

## 7. 项目构建与运行

### 7.1 环境要求

- Android Studio（支持 AGP 9.2.1）
- JDK 17
- Android SDK 37（compileSdk）
- minSdk: 35（Android 15）
- targetSdk: 37
- 仅支持 arm64-v8a ABI
- 运行环境：MIUI（支持超级岛）+ LSPosed / 其他 Xposed 框架

### 7.2 构建命令

```bash
# 构建 Release 版本
./gradlew assembleRelease

# 构建 Debug 版本
./gradlew assembleDebug

# 安装到设备
./gradlew installDebug
```

### 7.3 安装与使用

1. **安装 APK**: 安装构建生成的 APK
2. **激活 Xposed 模块**: 在 LSPosed 中激活 HyperLyric，作用域勾选「系统框架」和「系统界面」
3. **配置应用**: 打开 App 进行各项配置（超级岛开关、内容模式、样式等）
4. **重启 SystemUI**: 在 App 内点击「重启系统界面」或手动重启手机以激活 Hook

### 7.4 Xposed 模块配置

模块配置文件位于 [app/src/main/resources/META-INF/xposed](file:///workspace/app/src/main/resources/META-INF/xposed):

| 文件 | 用途 |
|------|------|
| `module.prop` | 模块元数据（名称、版本、作者、描述） |
| `scope.list` | 作用域包名列表 |
| `java_init.list` | Java 初始化类列表（HookEntry） |

### 7.5 CI/CD

GitHub Actions 配置位于 [.github/workflows/android_ci.yml](file:///workspace/.github/workflows/android_ci.yml)，支持自动构建 Release APK。

---

## 附录

### A. 配置同步机制

App 进程与 Xposed（SystemUI）进程通过以下机制同步配置：

1. **PrefsBridge**: App 进程写入配置时，通过 `RootApplication.syncPreference()` 触发同步
2. **RemotePreferences**: LibXposed 提供的 `getRemotePreferences()` 获取跨进程 SharedPreferences
3. **OnSharedPreferenceChangeListener**: Root 进程监听配置变化，实时更新运行时状态并触发岛刷新

### B. 日志系统

- **[HyperLogger](file:///workspace/app/src/main/java/com/lidesheng/hyperlyric/common/HyperLogger.kt)**: 统一的日志接口抽象
- **[HookLogger](file:///workspace/app/src/main/java/com/lidesheng/hyperlyric/root/utils/HookLogger.kt)**: Root 进程的实现，通过 LibXposed 的日志接口输出
- **[LogManager](file:///workspace/app/src/main/java/com/lidesheng/hyperlyric/utils/LogManager.kt)**: App 进程的日志管理，支持文件写入与导出

### C. 备份与恢复

- [BackupRestoreManager.kt](file:///workspace/app/src/main/java/com/lidesheng/hyperlyric/utils/BackupRestoreManager.kt): 配置备份恢复管理，支持导出/导入 JSON 配置

---

*文档版本: 5.0*  
*更新日期: 2026-07-14*  
*对应代码版本: 歌词数据结构重构（duration 改为计算属性）*
